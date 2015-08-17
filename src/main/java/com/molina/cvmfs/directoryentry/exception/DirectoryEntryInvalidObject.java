package com.molina.cvmfs.directoryentry.exception;

/**
 * @author Jose Molina Colmenero
 */
public class DirectoryEntryInvalidObject extends Exception {

    public DirectoryEntryInvalidObject() {
        super("Cannot retrieve symlinks nor directories");
    }
}
