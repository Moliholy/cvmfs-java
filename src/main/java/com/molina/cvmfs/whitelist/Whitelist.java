package com.molina.cvmfs.whitelist;

import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.rootfile.RootFile;
import com.molina.cvmfs.rootfile.exception.IncompleteRootFileSignature;
import com.molina.cvmfs.rootfile.exception.InvalidRootFileSignature;

import java.io.File;
import java.io.IOException;

/**
 * @author Jose Molina Colmenero
 */
public class Whitelist extends RootFile {

    /**
     * Initializes a root file object form a file pointer
     *
     * @param fileObject file that contains the necessary information to
     *                   initialize the object
     */
    public Whitelist(File fileObject) throws IOException, IncompleteRootFileSignature, InvalidRootFileSignature, ManifestValidityError, UnknownManifestField {
        super(fileObject);
    }

    @Override
    protected void readLine(String line) throws UnknownManifestField {

    }

    @Override
    protected void checkValidity() throws ManifestValidityError {

    }

    @Override
    protected boolean verifySignatureFromEntity(String publicEntity) {
        return false;
    }
}
