package com.molina.cvmfs.directoryentry.exception;

/**
 * @author Jose Molina Colmenero
 */
public class ChunkFileDoesNotMatch extends Exception {

    public ChunkFileDoesNotMatch() {
        super("Result set doesn't match when creating a chunk");
    }
}
