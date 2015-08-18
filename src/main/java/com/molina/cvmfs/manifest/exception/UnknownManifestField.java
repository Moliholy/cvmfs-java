package com.molina.cvmfs.manifest.exception;

import com.molina.cvmfs.common.exceptions.WarningException;

/**
 * @author Jose Molina Colmenero
 */
public class UnknownManifestField extends WarningException {

    public UnknownManifestField(char key) {
        super("Unknown manifest field: " + key);
    }
}
