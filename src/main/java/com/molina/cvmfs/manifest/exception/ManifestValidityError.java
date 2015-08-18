package com.molina.cvmfs.manifest.exception;

import com.molina.cvmfs.common.exceptions.WarningException;

/**
 * @author Jose Molina Colmenero
 */
public class ManifestValidityError extends WarningException {

    public ManifestValidityError(String message) {
        super(message);
    }
}
