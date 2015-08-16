package com.molina.cvmfs.repository.exception;

/**
 * @author Jose Molina Colmenero
 */
public class ConfigurationNotFound extends Exception {

    public ConfigurationNotFound(String repository, String configField) {
        super(repository + " " + configField);
    }
}
