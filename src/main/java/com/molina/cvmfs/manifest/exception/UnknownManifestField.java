package com.molina.cvmfs.manifest.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

/**
 * @author Jose Molina Colmenero
 */
public class UnknownManifestField extends RootFileException {

    public UnknownManifestField(char key) {
        super("Unknown manifest field: " + key);
    }
}
