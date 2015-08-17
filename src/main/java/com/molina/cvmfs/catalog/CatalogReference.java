package com.molina.cvmfs.catalog;

import com.molina.cvmfs.repository.Repository;

/**
 * @author Jose Molina Colmenero
 * Wraps a catalog reference to nested catalogs as found in Catalogs
 */
public class CatalogReference {

    protected String rootPath;
    protected String catalogHash;
    protected int catalogSize;

    public CatalogReference(String rootPath, String catalogHash, int catalogSize) {
        this.rootPath = rootPath;
        this.catalogHash = catalogHash;
        this.catalogSize = catalogSize;
    }

    public Catalog retrieveFrom(Repository sourceRepository) {
        return sourceRepository.retrieveCatalog(catalogHash);
    }

    public String getRootPath() {
        return rootPath;
    }

    public int getCatalogSize() {
        return catalogSize;
    }

    public String getCatalogHash() {
        return catalogHash;
    }
}
