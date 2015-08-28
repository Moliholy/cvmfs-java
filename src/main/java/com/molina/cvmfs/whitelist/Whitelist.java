package com.molina.cvmfs.whitelist;

import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.rootfile.RootFile;
import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Jose Molina Colmenero
 */
public class Whitelist extends RootFile {

    protected static final String FINGERPRINT_REGEX =
            "^(([0-9A-F]{2}:){19}[0-9A-F]{2}).*";
    protected static final String TIMESTAMP_regex = "[0-9]{14}";
    protected ArrayList<String> fingerprints;
    protected long expires;
    protected String repositoryName;
    protected long lastModified;

    /**
     * Wraps information from .cvmfswhitelist
     *
     * @param fileObject file that contains the necessary information to
     *                   initialize the object
     */
    public Whitelist(File fileObject) throws RootFileException, IOException {
        super(fileObject);
        fingerprints = new ArrayList<String>();
    }

    public static Whitelist open(String whitelistPath)
            throws RootFileException, IOException {
        return new Whitelist(new File(whitelistPath));
    }

    /**
     * Checks if the whitelist has expired
     * @return true if the whitelist has expired, or false otherwise
     */
    public boolean hasExpired() {
        long now = new Date().getTime();
        return (expires - now) < 0L;
    }

    /**
     * Lookup a certificate fingerprint in the whitelist
     * @param certificate certificate to check
     * @return true if the certificate fingerprint is in the whitelist
     */
    public boolean containsCertificate(Certificate certificate)
            throws CertificateEncodingException {
        String fingerPrint = certificate.getFingerPrint();
        for (String fp : fingerprints)
            if (fingerPrint.equals(fp))
                return true;
        return false;
    }

    /**
     * Parses lines that appear in .cvmfswhitelist
     * @param line line of .cvmfswhitelist to be parsed
     * @throws RootFileException if a
     */
    @Override
    protected void readLine(String line) throws RootFileException {

    }

    @Override
    protected void checkValidity() throws RootFileException {

    }

    @Override
    protected boolean verifySignatureFromEntity(String publicEntity) {
        return false;
    }
}
