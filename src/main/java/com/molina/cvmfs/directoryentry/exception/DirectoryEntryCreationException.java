package com.molina.cvmfs.directoryentry.exception;

/**
 * @author Jose Molina Colmenero
 */
public class DirectoryEntryCreationException extends Exception {

    public DirectoryEntryCreationException() {
        super("Couldn't instantiate a directory entry (ResultSet empty?)");
    }
}
