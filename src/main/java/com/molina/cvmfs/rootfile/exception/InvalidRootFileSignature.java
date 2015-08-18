package com.molina.cvmfs.rootfile.exception;

import com.molina.cvmfs.common.exceptions.WarningException;

/**
 * @author Jose Molina Colmenero
 */
public class InvalidRootFileSignature extends WarningException {

    public InvalidRootFileSignature(String message) {
        super(message);
    }
}
