package com.molina.cvmfs.repository.exception;

/**
 * Created by Jose Molina Colmenero
 */
public class ConfigurationNotFound extends Exception {

    public ConfigurationNotFound(String repository, String configField) {
        super(repository + " " + configField);
    }
}
