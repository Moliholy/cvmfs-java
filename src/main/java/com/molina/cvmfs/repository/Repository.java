package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.catalog.exception.CatalogInitializationException;
import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.manifest.Manifest;
import com.molina.cvmfs.manifest.exception.ManifestException;
import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepository;
import com.molina.cvmfs.repository.fetcher.Fetcher;
import com.molina.cvmfs.rootfile.exception.IncompleteRootFileSignature;
import com.molina.cvmfs.rootfile.exception.InvalidRootFileSignature;
import com.molina.cvmfs.rootfile.exception.RootFileException;
import com.molina.cvmfs.whitelist.Whitelist;

import java.io.*;
import java.net.MalformedURLException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around a CVMFS repository representation
 */
public class Repository {
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
            System.out.print("Couldn't retrieve all replication data");
        }
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
            String timestamp = br.readLine();
            lastReplication = new Date(Long.parseLong(timestamp));
            if (!hasRepositoryType())
                type = "stratum1";
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
            String timestamp = br.readLine();
            replicating = true;
            replicatingSince = new Date(Long.parseLong(timestamp));
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
        // TODO
        return true;
    }

    protected Whitelist retrieveWhitelist() {
        return null;
    }

    public Catalog[] getCatalogs(Catalog rootCatalog) {
        return null;
    }

    public boolean hasRepositoryType() {
        return ! type.equals("unknown");
    }

    public boolean hasHistory() {
        return manifest.hasHistory();
    }

    public Certificate retrieveCertificate()
            throws FileNotFoundInRepository, CertificateException, FileNotFoundException {
        File certificate = retrieveObject(manifest.getCertificate(), 'X');
        return new Certificate(certificate);
    }

    /**
     * Retrieves an object from the content addressable storage
     * @param objectHash hash of the object
     * @param hash_suffix suffix of the object
     * @return the object, if exists in the repository
     */
    public File retrieveObject(String objectHash, char hash_suffix) throws FileNotFoundInRepository {
        String path = "data/" + objectHash.substring(0, 2) + "/" +
                objectHash.substring(2, objectHash.length()) + hash_suffix;
        return fetcher.retrieveFile(path);
    }

    public File retrieveObject(String objectHash) throws FileNotFoundInRepository {
        return retrieveObject(objectHash, '\0');
    }

    public Catalog retrieveRootCatalog() {
        return retrieveCatalog(manifest.getRootCatalog());
    }

    /**
     * Recursively walk down the Catalogs and find the best fit for a path
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
     * @param catalogHash hash of the catalog to download
     * @return the catalog that corresponds to the hash
     */
    public Catalog retrieveCatalog(String catalogHash) {
        if (openedCatalogs.containsKey(catalogHash))
            return openedCatalogs.get(catalogHash);
        return retrieveAndOpenCatalog(catalogHash);
    }

    protected Catalog retrieveAndOpenCatalog(String catalogHash) {
        File catalogFile;
        try {
            catalogFile = retrieveObject(catalogHash, 'C');
            Catalog newCatalog = new Catalog(catalogFile, catalogHash);
            openedCatalogs.put(catalogHash, newCatalog);
            return newCatalog;
        } catch (FileNotFoundInRepository e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (CatalogInitializationException e) {
            e.printStackTrace();
        }
        return null;
    }

}
