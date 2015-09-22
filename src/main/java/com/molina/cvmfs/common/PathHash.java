package com.molina.cvmfs.common;

/**
 * @author Jose Molina Colmenero
 */
public class PathHash {

    private long hash1;
    private long hash2;

    public PathHash(long hash1, long hash2) {
        this.hash1 = hash1;
        this.hash2 = hash2;
    }

    public long getHash1() {
        return hash1;
    }

    public long getHash2() {
        return hash2;
    }
}
