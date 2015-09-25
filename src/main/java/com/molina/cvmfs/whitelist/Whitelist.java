package com.molina.cvmfs.whitelist;

import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.rootfile.RootFile;
import com.molina.cvmfs.rootfile.exception.RootFileException;
import com.molina.cvmfs.whitelist.exception.UnknownWhitelistLine;
import com.molina.cvmfs.whitelist.exception.WhitelistValidityErrorException;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * @author Jose Molina Colmenero
 */
public class Whitelist extends RootFile {

    protected static final String FINGERPRINT_REGEX =
            "^(([0-9A-F]{2}:){19}[0-9A-F]{2}).*";
    protected static final String TIMESTAMP_REGEX = "[0-9]{14}";
    protected ArrayList<String> fingerprints;
    protected Long expires;
    protected String repositoryName;
    protected Long lastModified;

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

    public boolean contains(Certificate certificate) {
        try {
            return fingerprints.contains(certificate.getFingerPrint());
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the whitelist has expired
     *
     * @return true if the whitelist has expired, or false otherwise
     */
    public boolean hasExpired() {
        long now = new Date().getTime();
        return (expires - now) < 0L;
    }

    /**
     * Lookup a certificate fingerprint in the whitelist
     *
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
     *
     * @param line line of .cvmfswhitelist to be parsed
     * @throws RootFileException if a error occur during the parse operation
     */
    @Override
    protected void readLine(String line) throws RootFileException {
        // Note: .cvmfswhitelist contains a last_modified field that does not
        //       have a key_char. However, it is a timestamp starting with the
        //       full year. We use '2' as the key, assuming that CernVM-FS will
        //       not sustain the next 1000 years.
        //
        // Note: the whitelist contains a list of certificate fingerprints that
        //       are not prepended by a key either. We use a regex to detect them
        char keyChar = line.charAt(0);
        String data = line.substring(1);
        boolean match = line.matches(FINGERPRINT_REGEX);
        if (match)
            fingerprints.add(line);
        else if (keyChar == '2')
            lastModified = readTimestamp(line);
        else if (keyChar == 'E')
            expires = readTimestamp(data);
        else if (keyChar == 'N')
            repositoryName = data;
        else
            throw new UnknownWhitelistLine(line);
    }

    private Long readTimestamp(String dateString) {
        Date date;
        try {
            date = new SimpleDateFormat("yyyyMMddkkmmss", Locale.ENGLISH)
                    .parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return date.getTime();
    }

    /**
     * Checks that all mandatory fields are found in .cvmfspublished
     * @throws RootFileException if it has been impossible to read the file
     */
    @Override
    protected void checkValidity() throws RootFileException {
        if (lastModified == null)
            throw new WhitelistValidityErrorException("Whitelist without a timestamp");
        if (expires == null)
            throw new WhitelistValidityErrorException("Whitelist without expiry date");
        if (repositoryName == null)
            throw new WhitelistValidityErrorException("Whitelist without repository name");
        if (fingerprints.isEmpty())
            throw new WhitelistValidityErrorException("No fingerprints are white-listed");
    }

    @Override
    protected boolean verifySignatureFromEntity(Object publicEntity) {
        return false;
    }
}
