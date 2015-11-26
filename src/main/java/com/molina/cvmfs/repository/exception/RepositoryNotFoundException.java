package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class RepositoryNotFoundException extends Exception {

    public RepositoryNotFoundException(String repositoryURI) {
        super(repositoryURI + " not found");
    }
}
