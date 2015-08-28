package com.molina.cvmfs.whitelist.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

/**
 * @author Jose Molina Colmenero
 */
public class WhitelistValidityError extends RootFileException {

    public WhitelistValidityError(String message) {
        super(message);
    }
}
