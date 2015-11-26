package com.molina.cvmfs.history.exception;

/**
 * @author Jose Molina Colmenero
 */
public class HistoryNotFoundException extends Exception {

    public HistoryNotFoundException() {
        super("This repository does not contain an history");
    }

}
