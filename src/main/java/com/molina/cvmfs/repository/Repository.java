package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.manifest.Manifest;

import java.io.File;
import java.util.Map;

/**
 * @author Jose Molina Colmenero
 * Abstract wrapper around a CVMFS repository representation
 */
public abstract class Repository {
    protected Map<String, Catalog> openedCatalogs;
    protected Manifest manifest;
    protected String fqrn;
    protected String storegeLocation;
    protected String type = "unknown";
    protected String replicatingSince;
    protected boolean replicating;

    public Repository(String source, String cacheDirectory) {

    }

    protected void readManifest() {

    }

    private static String readTimestamp(String timestamp) {
        return null;
    }

    protected void tryToGetLastReplicationTimestamp() {

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
