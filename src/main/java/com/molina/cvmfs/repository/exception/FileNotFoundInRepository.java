package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class FileNotFoundInRepository extends Exception {

    public FileNotFoundInRepository(String fileName) {
        super(fileName);
    }
}
