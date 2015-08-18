package com.molina.cvmfs.common.exceptions;

/**
 * @author Jose Molina Colmenero
 */
public abstract class WarningException extends Exception {

    public WarningException(String message) {
        super("WARNING: " + message);
    }
}
