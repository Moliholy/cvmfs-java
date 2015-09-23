package com.molina.cvmfs.repository.exception;


public class NestedCatalogNotFoundException extends Exception {

    public NestedCatalogNotFoundException(String repoName) {
        super(repoName);
    }
}
