package com.molina.cvmfs.common.exceptions;

/**
 * @author Jose Molina Colmenero
 */
public class CvmfsNotInstalled extends Exception {

    public CvmfsNotInstalled() {
        super("It seems cvmfs is not installed on this machine");
    }
}
