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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
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
            MalformedURLException, CacheDirectoryNotFound, ManifestException {
        if (source == null || source.isEmpty())
            throw new FailedToLoadSourceException("The source cannot be empty");
        openedCatalogs = new HashMap<String, Catalog>();
        storageLocation = cacheDirectory;
        determineSource(source, cacheDirectory);
        try {
            readManifest();
            tryToGetLastReplicationTimestamp();
            tryToGetReplicationState();
        } catch (IOException e) {
            e.printStackTrace();
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

    protected void readManifest() throws IOException, ManifestException {
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

    }

    protected void tryToGetReplicationState() {

    }

    public boolean verify(String publicKeyPath) {
        return false;
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
