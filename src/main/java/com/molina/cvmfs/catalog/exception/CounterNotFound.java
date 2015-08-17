package com.molina.cvmfs.catalog.exception;

/**
 * @author Jose Molina Colmenero
 */
public class CounterNotFound extends Exception {

    public CounterNotFound(String counterName) {
        super("Statictic " + counterName + " not provided");
    }
}
