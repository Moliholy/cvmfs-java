package com.molina.cvmfs.rootfile.exception;

import com.molina.cvmfs.common.exceptions.WarningException;

/**
 * @author Jose Molina Colmenero
 */
public class IncompleteRootFileSignature extends RootFileException {

    public IncompleteRootFileSignature(String message) {
        super(message);
    }
}
