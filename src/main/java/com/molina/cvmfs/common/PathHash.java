package com.molina.cvmfs.common;

/**
 * @author Jose Molina Colmenero
 */
public class PathHash {

    private int hash1;
    private int hash2;

    public PathHash(int hash1, int hash2) {
        this.hash1 = hash1;
        this.hash2 = hash2;
    }

    public int getHash1() {
        return hash1;
    }

    public int getHash2() {
        return hash2;
    }
}
