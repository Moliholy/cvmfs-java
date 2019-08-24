package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.catalog.exception.CatalogInitializationException;
import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.fetcher.Fetcher;
import com.molina.cvmfs.history.History;
import com.molina.cvmfs.history.RevisionTag;
import com.molina.cvmfs.history.exception.HistoryNotFoundException;
import com.molina.cvmfs.manifest.Manifest;
import com.molina.cvmfs.manifest.exception.ManifestException;
import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepositoryException;
import com.molina.cvmfs.repository.exception.RepositoryNotFoundException;
import com.molina.cvmfs.revision.Revision;
import com.molina.cvmfs.rootfile.exception.IncompleteRootFileSignature;
import com.molina.cvmfs.rootfile.exception.InvalidRootFileSignature;
import com.molina.cvmfs.rootfile.exception.RootFileException;
import com.molina.cvmfs.whitelist.Whitelist;

import java.io.*;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Wrapper around a CVMFS repository representation
 */
public class Repository {
    protected Map<String, Catalog> openedCatalogs;
    protected Manifest manifest;
    protected String fqrn;
    protected String type = "unknown";
    protected Date replicatingSince;
    protected Date lastReplication;
    protected boolean replicating;
    protected Fetcher fetcher;

    public Repository(Fetcher fetcher) throws IOException, RootFileException {
        this.fetcher = fetcher;
        openedCatalogs = new HashMap<>();
        readManifest();
        tryToGetLastReplicationTimestamp();
        tryToGetReplicationState();
    }

    public Repository(String source, String cacheDir) throws RepositoryNotFoundException, RootFileException, CacheDirectoryNotFound, IOException, FailedToLoadSourceException {
        this(getFetcherFromSource(source, cacheDir));
    }

    private static Fetcher getFetcherFromSource(String source, String cacheDirectory)
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
        return new Fetcher(finalSource, cacheDirectory);
    }

    public boolean unloadCatalogs() {
        boolean closed = true;
        for (Catalog c : openedCatalogs.values()) {
            if (!c.close())
                closed = false;
        }
        openedCatalogs.clear();
        return closed;
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

    protected void readManifest() throws IOException, RootFileException {
        File manifestFile = fetcher.retrieveRawFile(Common.MANIFEST_NAME);
        try {
            manifest = new Manifest(manifestFile);
        } catch (InvalidRootFileSignature | ManifestValidityError | IncompleteRootFileSignature | UnknownManifestField invalidRootFileSignature) {
            System.err.println(invalidRootFileSignature.getMessage());
        }
        if (manifest == null)
            throw new ManifestException();
        fqrn = manifest.getRepositoryName();
    }

    protected void tryToGetLastReplicationTimestamp() {
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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                }
        }
    }

    protected void tryToGetReplicationState() {
        BufferedReader br = null;
        try {
            File file = fetcher.retrieveRawFile(Common.REPLICATING_NAME);
            replicating = false;
            br = new BufferedReader(new FileReader(file));
            String dateString = br.readLine();
            replicating = true;
            replicatingSince = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy",
                    Locale.ENGLISH).parse(dateString);
        } catch (IOException | ParseException e) {
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                }
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

    public Fetcher getFetcher() {
        return fetcher;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public boolean verify(String publicKeyPath) {
        try {
            Whitelist whitelist = retrieveWhitelist();
            Certificate certificate = retrieveCertificate();
            return whitelist.verifySignature(publicKeyPath) &&
                    !whitelist.hasExpired() &&
                    whitelist.containsCertificate(certificate) &&
                    manifest.verifySignature(certificate);
        } catch (CertificateException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundInRepositoryException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected Whitelist retrieveWhitelist() throws IOException {
        File whitelistFile = fetcher.retrieveRawFile(Common.WHITELIST_NAME);
        try {
            return new Whitelist(whitelistFile);
        } catch (RootFileException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasRepositoryType() {
        return type != null && !type.equals("unknown");
    }

    public boolean hasHistory() {
        return manifest.hasHistory();
    }

    public Certificate retrieveCertificate()
            throws FileNotFoundInRepositoryException, CertificateException, FileNotFoundException {
        File certificate = retrieveObject(manifest.getCertificate(), Certificate.CERTIFICATE_ROOT_PREFIX);
        return new Certificate(certificate);
    }

    /**
     * Retrieves an object from the content addressable storage
     *
     * @param objectHash  hash of the object
     * @param hash_suffix suffix of the object
     * @return the object, if exists in the repository
     */
    public File retrieveObject(String objectHash, String hash_suffix) throws FileNotFoundInRepositoryException {
        String path = "data" + File.separator + objectHash.substring(0, 2) + File.separator +
                objectHash.substring(2, objectHash.length()) + hash_suffix;
        return fetcher.retrieveFile(path);
    }

    public File retrieveObject(String objectHash) throws FileNotFoundInRepositoryException {
        return retrieveObject(objectHash, "");
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

    protected Catalog retrieveAndOpenCatalog(String catalogHash) {
        File catalogFile;
        try {
            catalogFile = retrieveObject(catalogHash, Catalog.CATALOG_ROOT_PREFIX);
            Catalog newCatalog = new Catalog(catalogFile, catalogHash);
            openedCatalogs.put(catalogHash, newCatalog);
            return newCatalog;
        } catch (FileNotFoundInRepositoryException | CatalogInitializationException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public History retrieveHistory() throws HistoryNotFoundException {
        if (!hasHistory()) {
            throw new HistoryNotFoundException();
        }
        try {
            File historyDB = retrieveObject(manifest.getHistoryDatabase(), "H");
            return new History(historyDB);
        } catch (FileNotFoundInRepositoryException | SQLException e) {
            throw new HistoryNotFoundException();
        }
    }

    public Revision getRevision(String tagName) {
        try {
            History history = retrieveHistory();
            RevisionTag rt = history.getTagByName(tagName);
            return new Revision(this, rt);
        } catch (HistoryNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Revision getRevision(int revisionNumber) {
        try {
            History history = retrieveHistory();
            RevisionTag rt = history.getTagByRevision(revisionNumber);
            return new Revision(this, rt);
        } catch (HistoryNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Revision getCurrentRevision() {
        return getRevision(manifest.getRevision());
    }

    public Map<String, Catalog> getOpenedCatalogs() {
        return openedCatalogs;
    }
}
