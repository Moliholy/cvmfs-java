package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.catalog.exception.CatalogInitializationException;
import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.manifest.Manifest;
import com.molina.cvmfs.manifest.exception.ManifestException;
import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepositoryException;
import com.molina.cvmfs.repository.fetcher.Fetcher;
import com.molina.cvmfs.rootfile.exception.IncompleteRootFileSignature;
import com.molina.cvmfs.rootfile.exception.InvalidRootFileSignature;
import com.molina.cvmfs.rootfile.exception.RootFileException;
import com.molina.cvmfs.whitelist.Whitelist;

import java.io.*;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Wrapper around a CVMFS repository representation
 */
public class Repository implements Iterable<DirectoryEntryWrapper> {
    protected Map<String, Catalog> openedCatalogs;
    protected Manifest manifest;
    protected String fqrn;
    protected String storageLocation;
    protected String type = "unknown";
    protected Date replicatingSince;
    protected Date lastReplication;
    protected boolean replicating;
    protected Fetcher fetcher;

    public Repository(String source, String cacheDirectory)
            throws FailedToLoadSourceException,
            IOException, CacheDirectoryNotFound, RootFileException {
        if (source == null || source.isEmpty())
            throw new FailedToLoadSourceException("The source cannot be empty");
        openedCatalogs = new HashMap<String, Catalog>();
        storageLocation = cacheDirectory;
        determineSource(source, cacheDirectory);
        readManifest();
        try {
            tryToGetLastReplicationTimestamp();
            tryToGetReplicationState();
        } catch (IOException e) {
            System.err.println("Couldn't retrieve all replication data");
        }
    }

    /**
     * Returns the catalog that has the corresponding path, or the closest
     * @param path the path to search for
     * @return the currently best fit for the given path, but NOT
     * necessarily the catalog that contains the given path
     */
    public Catalog getOpenedCatalogForPath(String path) {
        String bestPath = "";
        for (String catalogPath : openedCatalogs.keySet()) {
            if (path.contains(catalogPath)) {
                bestPath = catalogPath;
            }
        }
        return openedCatalogs.get(bestPath);
    }

    private CatalogReference findBestFit(CatalogReference[] catalogReferences,
                                         String path) {
        CatalogReference bestFit = null;
        for (CatalogReference cr : catalogReferences) {
            if (cr.getRootPath().contains(path)) {
                bestFit = cr;
            }
        }
        return bestFit;
    }

    /**
     * Retrieves the DirectoryEntry that corresponds to the given path, if exists
     * @param path the path of the file or directory
     * @return the DirectoryEntry for the given path, or null if the path is not correct
     */
    public DirectoryEntry lookup(String path) {
        Catalog bestFit = getOpenedCatalogForPath(path);
        DirectoryEntry result = bestFit.findDirectoryEntry(path);
        while (result == null) {
            CatalogReference bestNested = findBestFit(bestFit.listNested(), path);
            if (bestNested == null)
                break;
            bestFit = bestNested.retrieveFrom(this);
            result = bestFit.findDirectoryEntry(path);
        }
        return result;
    }

    /**
     * List a directory
     * @param path path of the directory
     * @return a List of DirectoryEntry representing all the entries for the
     * given directory, or null if such a directory does not exist
     */
    public List<DirectoryEntry> listDirectory(String path) {
        Catalog bestFit = getOpenedCatalogForPath(path);
        List<DirectoryEntry> result = bestFit.listDirectory(path);
        while (result == null) {
            CatalogReference bestNested = findBestFit(bestFit.listNested(), path);
            if (bestNested == null)
                break;
            bestFit = bestNested.retrieveFrom(this);
            result = bestFit.listDirectory(path);
        }
        return result != null ? result : new ArrayList<DirectoryEntry>();
    }

    public Catalog getMountedCatalog(String path) {
        for (Catalog c : openedCatalogs.values()) {
            if (c.getRootPrefix().equals(path)) {
                return c;
            }
        }
        return null;
    }

    public Repository(String source) throws IOException, RootFileException,
            FailedToLoadSourceException, CacheDirectoryNotFound {
        this(source, Files.createTempDirectory("cache.").toFile().getAbsolutePath());
    }

    private void determineSource(String source, String cacheDirectory)
            throws FailedToLoadSourceException,
            IOException, CacheDirectoryNotFound {
        String finalSource;
        String serverDefaultLocation = "/srv/cvmfs/";
        if (source.startsWith("http://"))
            finalSource = source;
        else if (new File(serverDefaultLocation + source).exists())
            finalSource = "file://" + serverDefaultLocation + source;
        else if (new File(source).exists())
            finalSource = "file://" + source;
        else
            throw new FailedToLoadSourceException(
                    "Repository not found: " + source);
        fetcher = new Fetcher(finalSource, cacheDirectory);
    }

    protected void readManifest() throws IOException, RootFileException {
        File manifestFile = fetcher.retrieveRawFile(Common.MANIFEST_NAME);
        try {
            manifest = new Manifest(manifestFile);
        } catch (InvalidRootFileSignature invalidRootFileSignature) {
            System.out.println(invalidRootFileSignature.getMessage());
        } catch (UnknownManifestField unknownManifestField) {
            System.out.println(unknownManifestField.getMessage());
        } catch (IncompleteRootFileSignature incompleteRootFileSignature) {
            System.out.println(incompleteRootFileSignature.getMessage());
        } catch (ManifestValidityError manifestValidityError) {
            System.out.println(manifestValidityError.getMessage());
        }
        if (manifest == null)
            throw new ManifestException();
        fqrn = manifest.getRepositoryName();
    }

    protected void tryToGetLastReplicationTimestamp() throws IOException {
        BufferedReader br = null;
        lastReplication = new Date(0);
        try {
            File file = fetcher.retrieveRawFile(Common.LAST_REPLICATION_NAME);
            br = new BufferedReader(new FileReader(file));
            String dateString = br.readLine();
            lastReplication = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy",
                    Locale.ENGLISH).parse(dateString);
            if (!hasRepositoryType())
                type = "stratum1";
        } catch (ParseException e) {
            lastReplication = null;
        } finally {
            if (br != null)
                br.close();
        }

    }

    protected void tryToGetReplicationState() throws IOException {
        BufferedReader br = null;
        try {
            replicating = false;
            File file = fetcher.retrieveRawFile(Common.REPLICATING_NAME);
            br = new BufferedReader(new FileReader(file));
            String dateString = br.readLine();
            replicating = true;
            replicatingSince = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy",
                    Locale.ENGLISH).parse(dateString);
        } catch (ParseException e) {
            replicatingSince = null;
        } finally {
            if (br != null)
                br.close();
        }
    }

    public String getFqrn() {
        return fqrn;
    }

    public String getType() {
        return type;
    }

    public Date getReplicatingSince() {
        return replicatingSince;
    }

    public Date getLastReplication() {
        return lastReplication;
    }

    public boolean isReplicating() {
        return replicating;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public Fetcher getFetcher() {
        return fetcher;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public boolean verify(String publicKeyPath) {
        Whitelist whitelist = retrieveWhitelist();
        try {
            Certificate certificate = retrieveCertificate();
            return whitelist.verifySignature(publicKeyPath) &&
                    !whitelist.hasExpired() &&
                    whitelist.containsCertificate(certificate) &&
                    manifest.verifySignature(certificate);
        } catch (FileNotFoundInRepositoryException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected Whitelist retrieveWhitelist() {
        return null;
    }

    public Catalog[] getCatalogs(Catalog rootCatalog) {
        return null;
    }

    public boolean hasRepositoryType() {
        return !type.equals("unknown");
    }

    public boolean hasHistory() {
        return manifest.hasHistory();
    }

    public Certificate retrieveCertificate()
            throws FileNotFoundInRepositoryException, CertificateException, FileNotFoundException {
        File certificate = retrieveObject(manifest.getCertificate(), 'X');
        return new Certificate(certificate);
    }

    /**
     * Retrieves an object from the content addressable storage
     *
     * @param objectHash  hash of the object
     * @param hash_suffix suffix of the object
     * @return the object, if exists in the repository
     */
    public File retrieveObject(String objectHash, char hash_suffix) throws FileNotFoundInRepositoryException {
        String path = "data/" + objectHash.substring(0, 2) + "/" +
                objectHash.substring(2, objectHash.length()) + hash_suffix;
        return fetcher.retrieveFile(path);
    }

    public File retrieveObject(String objectHash) throws FileNotFoundInRepositoryException {
        return retrieveObject(objectHash, '\0');
    }

    public Catalog retrieveRootCatalog() {
        return retrieveCatalog(manifest.getRootCatalog());
    }

    /**
     * Recursively walk down the Catalogs and find the best fit for a path
     *
     * @param needlePath path of the catalog
     * @return the catalog for the given path
     */
    public Catalog retrieveCatalogForPath(String needlePath) {
        Catalog root = retrieveRootCatalog();
        while (true) {
            CatalogReference newNestedReference = root.findNestedForPath(needlePath);
            if (newNestedReference == null)
                break;
            root = retrieveCatalog(newNestedReference.getCatalogHash());
        }
        return root;
    }

    public void closeCatalog(Catalog catalog) {
        catalog.close();
    }

    /**
     * Download and open a catalog from the repository
     *
     * @param catalogHash hash of the catalog to download
     * @return the catalog that corresponds to the hash
     */
    public Catalog retrieveCatalog(String catalogHash) {
        if (openedCatalogs.containsKey(catalogHash))
            return openedCatalogs.get(catalogHash);
        return retrieveAndOpenCatalog(catalogHash);
    }

    public void retrieveCatalogTree(Catalog catalog) {
        for (CatalogReference ref : catalog.listNested()) {
            Catalog newCatalog = ref.retrieveFrom(this);
            retrieveCatalogTree(newCatalog);
        }
    }

    public void retrieveCatalogTree() {
        retrieveCatalogTree(retrieveRootCatalog());
    }

    protected Catalog retrieveAndOpenCatalog(String catalogHash) {
        File catalogFile;
        try {
            catalogFile = retrieveObject(catalogHash, 'C');
            Catalog newCatalog = new Catalog(catalogFile, catalogHash);
            openedCatalogs.put(catalogHash, newCatalog);
            return newCatalog;
        } catch (FileNotFoundInRepositoryException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (CatalogInitializationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Iterator<DirectoryEntryWrapper> iterator() {
        return new RepositoryIterator(this);
    }
}
