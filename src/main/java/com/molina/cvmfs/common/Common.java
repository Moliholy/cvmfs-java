package com.molina.cvmfs.common;

import javax.crypto.Mac;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Jose Molina Colmenero
 */
public class Common {

    public static final String REPO_CONFIG_PATH = "/etc/cvmfs/repositories.d";
    public static final String SERVER_CONFIG_NAME = "server.conf";

    public static final String REST_CONNECTOR = "control";

    public static final String MANIFEST_NAME = ".cvmfspublished";
    public static final String LAST_REPLICATION_NAME = ".cvmfs_last_snapshot";
    public static final String REPLICATING_NAME = ".cvmfs_is_snapshotting";

    public static String binaryBufferToHexString(byte[] binaryBuffer) {
        StringBuilder sb = new StringBuilder();
        for (byte b : binaryBuffer)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String pathToMd5(String path) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(path.getBytes());
            return binaryBufferToHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static PathHash splitMd5(byte[] md5Digest) {
        long hi = 0;
        long lo = 0;
        for (int i = 0; i < 8; i++)
            lo |= ((long) (md5Digest[i] & 0xFF)) << (i * 8);
        for (int i = 8; i < 16; i++) {
            hi |= ((long) (md5Digest[i] & 0xFF)) << ((i - 8) * 8);
        }
        return new PathHash(lo, hi);
    }
}
