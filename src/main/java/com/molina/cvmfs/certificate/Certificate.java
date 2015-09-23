package com.molina.cvmfs.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author Jose Molina Colmenero
 *         Wraps an X.509 certificate object as stored in CVMFS repositories
 */
public class Certificate {
    protected File certificateFile;
    protected X509Certificate opensslCertificate;

    public Certificate(File certificateFile) throws CertificateException, FileNotFoundException {
        this.certificateFile = certificateFile;
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        opensslCertificate = (X509Certificate) certFactory.generateCertificate(new FileInputStream(certificateFile));
    }

    public File getCertificateFile() {
        return certificateFile;
    }

    public X509Certificate getOpensslCertificate() {
        return opensslCertificate;
    }

    public String getFingerPrint() throws CertificateEncodingException {
        return getFingerPrint("SHA-1");
    }

    public String getFingerPrint(String algorithm) throws CertificateEncodingException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md.update(opensslCertificate.getEncoded());
        byte[] digest = md.digest();
        BigInteger hexDigest = new BigInteger(1, digest);
        return hexDigest.toString();
    }

    /**
     * Verify a given signature to an expected 'message' string
     *
     * @param signature signature to verify
     * @param message   message to verify against
     * @return true if the signature is verified through the message
     */
    public boolean verify(String signature, String message) throws NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, UnsupportedEncodingException {
        PublicKey pubkey = opensslCertificate.getPublicKey();
        final Signature sig = Signature.getInstance("SHA-1");
        sig.initVerify(pubkey);
        sig.update(message.getBytes(Charset.defaultCharset()));
        return sig.verify(signature.getBytes());
    }
}
