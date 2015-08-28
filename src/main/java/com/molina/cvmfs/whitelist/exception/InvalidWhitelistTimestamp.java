package com.molina.cvmfs.whitelist.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.util.Date;

/**
 * @author Jose Molina Colmenero
 */
public class InvalidWhitelistTimestamp extends RootFileException {

    public InvalidWhitelistTimestamp(Date date) {
        super(date.toString());
    }
}
