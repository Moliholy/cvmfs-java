package com.molina.cvmfs.whitelist.exception;

import java.util.Date;

/**
 * @author Jose Molina Colmenero
 */
public class InvalidWhitelistTimestamp extends Exception {

    public InvalidWhitelistTimestamp(Date date) {
        super(date.toString());
    }
}
