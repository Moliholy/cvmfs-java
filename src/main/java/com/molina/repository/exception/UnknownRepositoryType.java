package com.molina.repository.exception;

/**
 * Created by Jose Molina Colmenero
 */
public class UnknownRepositoryType extends Exception {

    public UnknownRepositoryType(String repoFQRN, String repoType) {
        super(repoFQRN + " (" + repoType + ")");
    }
}
