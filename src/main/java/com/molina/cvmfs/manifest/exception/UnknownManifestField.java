package com.molina.cvmfs.manifest.exception;

/**
 * @author Jose Molina Colmenero
 */
public class UnknownManifestField extends Exception{

    public UnknownManifestField(char key) {
        super("Unknown manifest field: " + key);
    }
}
