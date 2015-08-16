package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class RepositoryNotFound extends Exception {

    public RepositoryNotFound(String repositoryURI) {
        super(repositoryURI + " not found");
    }
}
