package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.manifest.Manifest;
import com.molina.cvmfs.manifest.exception.ManifestException;
import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.fetcher.Fetcher;
import com.molina.cvmfs.rootfile.exception.IncompleteRootFileSignature;
import com.molina.cvmfs.rootfile.exception.InvalidRootFileSignature;
import com.molina.cvmfs.rootfile.exception.RootFileException;
import com.molina.cvmfs.whitelist.Whitelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jose Molina Colmenero
 *
 * Abstract wrapper around a CVMFS repository representation
 */
public abstract class Repository {
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
            MalformedURLException, CacheDirectoryNotFound {
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
        return false;
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

    public Certificate retrieveCertificate() {
        return null;
    }

    public abstract File retrieveObject(String objectHash, char hash_suffix);

    public Catalog retrieveRootCatalog() {
        return retrieveCatalog(manifest.getRootCatalog());
    }

    public Catalog retrieveCatalogForPath(String needlePath) {
        return null;
    }

    public void closeCatalog(Catalog catalog) {

    }

    public Catalog retrieveCatalog(String catalogHash) {
        return null;
    }

    protected Catalog retrieveAndOpenCatalog(String catalogHash) {
        return null;
    }

    public File retrieveObject(String s) {
        return null;
    }
}
