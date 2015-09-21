package com.molina.cvmfs.repository.exception;


public class NestedCatalogNotFound extends Exception {

    public NestedCatalogNotFound(String repoName) {
        super(repoName);
    }
}
