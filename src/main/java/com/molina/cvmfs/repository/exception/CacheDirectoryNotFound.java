package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class CacheDirectoryNotFound extends Exception {

    public CacheDirectoryNotFound(String cacheDirectory) {
        super("Cannot find the path " + cacheDirectory);
    }
}
