package com.molina.cvmfs.whitelist.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

/**
 * @author Jose Molina Colmenero
 */
public class WhitelistValidityErrorException extends RootFileException {

    public WhitelistValidityErrorException(String message) {
        super(message);
    }
}
