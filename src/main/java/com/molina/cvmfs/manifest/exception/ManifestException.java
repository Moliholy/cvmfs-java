package com.molina.cvmfs.manifest.exception;

import com.molina.cvmfs.rootfile.exception.RootFileException;

/**
 * @author Jose Molina Colmenero
 */
public class ManifestException extends RootFileException {

    public ManifestException() {
        super("Error while processing the manifest");
    }
}
