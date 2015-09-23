package com.molina.cvmfs.rootfile.exception;

/**
 * @author Jose Molina Colmenero
 */
public class IncompleteRootFileSignature extends RootFileException {

    public IncompleteRootFileSignature(String message) {
        super(message);
    }
}
