package com.molina.cvmfs.common;

/**
 * @author Jose Molina Colmenero
 */
public class PathHash {

    private String hash1;
    private String hash2;

    public PathHash(String hash1, String hash2) {
        this.hash1 = hash1;
        this.hash2 = hash2;
    }

    public String getHash1() {
        return hash1;
    }

    public String getHash2() {
        return hash2;
    }
}
