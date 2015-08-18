package com.molina.cvmfs.manifest.exception;

/**
 * @author Jose Molina Colmenero
 */
public class ManifestException extends Exception {

    public ManifestException() {
        super("Error while processing the manifest");
    }
}
