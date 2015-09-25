package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class FileNotFoundInRepositoryException extends Exception {

    public FileNotFoundInRepositoryException(String fileName) {
        super(fileName);
    }
}
