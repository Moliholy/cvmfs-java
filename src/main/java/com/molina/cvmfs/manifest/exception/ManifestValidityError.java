package com.molina.cvmfs.manifest.exception;

/**
 * @author Jose Molina Colmenero
 */
public class ManifestValidityError extends Exception {

    public ManifestValidityError(String message) {
        super(message);
    }
}
