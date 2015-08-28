package com.molina.cvmfs.whitelist.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

/**
 * @author Jose Molina Colmenero
 */
public class UnknownWhitelistLine extends RootFileException {

    public UnknownWhitelistLine(String message) {
        super(message);
    }
}
