package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class UnknownRepositoryType extends Exception {

    public UnknownRepositoryType(String repoFQRN, String repoType) {
        super(repoFQRN + " (" + repoType + ")");
    }
}
