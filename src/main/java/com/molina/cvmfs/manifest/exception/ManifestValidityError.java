package com.molina.cvmfs.manifest.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

/**
 * @author Jose Molina Colmenero
 */
public class ManifestValidityError extends RootFileException {

    public ManifestValidityError(String message) {
        super(message);
    }
}
