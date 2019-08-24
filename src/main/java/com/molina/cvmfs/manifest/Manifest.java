package com.molina.cvmfs.manifest;

import com.molina.cvmfs.certificate.Certificate;
import com.molina.cvmfs.manifest.exception.ManifestValidityError;
import com.molina.cvmfs.manifest.exception.UnknownManifestField;
import com.molina.cvmfs.rootfile.RootFile;
import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Date;

/**
 * @author Jose Molina Colmenero
 *         <p/>
 *         Wraps information from .cvmfspublished
 */
public class Manifest extends RootFile {

    protected String rootCatalog;
    protected String rootHash;
    protected int rootCatalogSize;
    protected String certificate;
    protected String historyDatabase;
    protected Date lastModified;
    protected int ttl;
    protected int revision;
    protected String repositoryName;
    protected String microCatalog;
    protected boolean garbageCollectable;
    protected boolean alternativeName;

    public Manifest(File fileObject) throws RootFileException, IOException {
        super(fileObject);
    }

    public static Manifest open(String manifestPath)
            throws RootFileException, IOException {
        return new Manifest(new File(manifestPath));
    }

    public boolean hasHistory() {
        return historyDatabase != null;
    }

    protected boolean verifySignature(Certificate certificate) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return certificate.verify(signature, signatureChecksum);
    }

    /**
     * Parse lines that appear in .cvmfspublished
     *
     * @param line line of .cvmfspublished
     */
    @Override
    protected void readLine(String line) throws UnknownManifestField {
        char key = line.charAt(0);
        String data = line.substring(1);
        switch (key) {
            case 'C':
                rootCatalog = data;
                break;
            case 'R':
                rootHash = data;
                break;
            case 'B':
                rootCatalogSize = Integer.parseInt(data);
                break;
            case 'X':
                certificate = data;
                break;
            case 'H':
                historyDatabase = data;
                break;
            case 'T':
                lastModified = new Date(Long.parseLong(data));
                break;
            case 'D':
                ttl = Integer.parseInt(data);
                break;
            case 'S':
                revision = Integer.parseInt(data);
                break;
            case 'N':
                repositoryName = data;
                break;
            case 'L':
                microCatalog = data;
                break;
            case 'G':
                garbageCollectable = data.equals("yes");
                break;
            case 'A':
                alternativeName = data.equals("yes");
                break;
            default:
                throw new UnknownManifestField(key);
        }
    }

    /**
     * Checks that all mandatory fields are found in .cvmfspublished
     */
    @Override
    protected void checkValidity() throws ManifestValidityError {
        if (rootCatalog == null)
            throw new ManifestValidityError(
                    "Manifest lacks a root catalog entry");
        if (rootHash == null)
            throw new ManifestValidityError("Manifest lacks a root hash entry");
        if (ttl == 0)
            throw new ManifestValidityError("Manifest lacks a TTL entry");
        if (revision == 0)
            throw new ManifestValidityError("Manifest lacks a revision entry");
        if (repositoryName == null)
            throw new ManifestValidityError("Manifest lacks a repository name");
    }

    @Override
    protected boolean verifySignatureFromEntity(Object publicEntity) {
        if (publicEntity instanceof Certificate) {
            try {
                return ((Certificate) publicEntity).verify(signature, signatureChecksum);
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException | SignatureException | InvalidKeyException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String getRootCatalog() {
        return rootCatalog;
    }

    public String getRootHash() {
        return rootHash;
    }

    public int getRootCatalogSize() {
        return rootCatalogSize;
    }

    public String getCertificate() {
        return certificate;
    }

    public String getHistoryDatabase() {
        return historyDatabase;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public int getTTL() {
        return ttl;
    }

    public int getRevision() {
        return revision;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getMicroCatalog() {
        return microCatalog;
    }

    public boolean isGarbageCollectable() {
        return garbageCollectable;
    }
}
