package com.molina.cvmfs.rootfile;

import com.molina.cvmfs.rootfile.exception.IncompleteRootFileSignature;
import com.molina.cvmfs.rootfile.exception.InvalidRootFileSignature;
import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Jose Molina Colmenero
 *         <p/>
 *         Base class for CernVM-FS repository's signed 'root files'
 *         <p/>
 *         A CernVM-FS repository has essential 'root files' that have a defined name and
 *         serve as entry points into the repository.
 *         Namely the manifest (.cvmfspublished) and the whitelist (.cvmfswhitelist) that
 *         both have class representations inheriting from RootFile and implementing the
 *         abstract methods defined here.
 *         Any 'root file' in CernVM-FS is a signed list of line-by-line key-value pairs
 *         where the key is represented by a single character in the beginning of a line
 *         directly followed by the value. The key-value part of the file is terminted
 *         either by EOF or by a termination line (--) followed by a signature.
 *         The signature follows directly after the termination line with a hash of the
 *         key-value line content (without the termination line) followed by an \n and a
 *         binary string containing the private-key signature terminated by EOF.
 */
public abstract class RootFile {

    protected boolean hasSignature;
    protected String signatureChecksum;
    protected String signature;

    /**
     * Initializes a root file object form a file pointer
     *
     * @param fileObject file that contains the necessary information to initialize the object
     */
    public RootFile(File fileObject)
            throws IOException, RootFileException {
        hasSignature = false;
        BufferedReader br = new BufferedReader(new FileReader(fileObject));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.length() == 0)
                continue;
            if (line.substring(0, 2).equals("--")) {
                hasSignature = true;
                signatureChecksum = br.readLine();
                if (signatureChecksum == null)
                    throw new IncompleteRootFileSignature(
                            "Signature not found");
                break;
            }
            readLine(line);
        }
        br.close();
        if (hasSignature && signature != null)
            readSignature(fileObject);
        checkValidity();
    }

    public boolean verifySignature(Object publicEntity) {
        return hasSignature && verifySignatureFromEntity(publicEntity);
    }

    private String hashOverContent(File fileObject) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileObject));
        MessageDigest hashSum;
        try {
            hashSum = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        String line;
        while ((line = br.readLine()) != null) {
            if (line.equals("--")) {
                br.readLine();  // no longer care about the checksum, we already got it
                signature = "";
                while ((line = br.readLine()) != null) {
                    signature += line;
                }
            }
            if (line != null)
                hashSum.update(line.getBytes());
        }
        byte[] digest = hashSum.digest();
        BigInteger hexDigest = new BigInteger(1, digest);
        return hexDigest.toString();
    }

    /**
     * Reads the signature's checksum and the binary signature string
     *
     * @param fileObject byte array containing the signature
     */
    protected void readSignature(File fileObject)
            throws IOException, IncompleteRootFileSignature,
            InvalidRootFileSignature {
        if (signatureChecksum.length() != 40)
            throw new IncompleteRootFileSignature("Signature checksum malformed");
        String messageDigest = hashOverContent(fileObject);
        if (messageDigest != null && !messageDigest.equals(signatureChecksum))
            throw new InvalidRootFileSignature("Signature checksum doesn't match");
        if (signature.length() == 0)
            throw new IncompleteRootFileSignature("Binary signature not found");

    }

    protected abstract void readLine(String line) throws RootFileException;

    protected abstract void checkValidity() throws RootFileException;

    protected abstract boolean verifySignatureFromEntity(Object publicEntity);
}
