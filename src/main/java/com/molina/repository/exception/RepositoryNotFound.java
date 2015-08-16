package com.molina.repository.exception;

/**
 * Created by Jose Molina Colmenero
 */
public class RepositoryNotFound extends Exception {

    public RepositoryNotFound(String repositoryURI) {
        super(repositoryURI + " not found");
    }
}
