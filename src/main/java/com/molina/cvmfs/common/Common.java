package com.molina.cvmfs.common;

import java.nio.ByteBuffer;

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
            sb.append(String.format("%02X ", b));
        return sb.toString().toLowerCase();
    }

    public static PathHash splitMd5(byte[] md5Digest) {
        int hi = 0;
        int lo = 0;
        for (int i = 0; i < 8; i++)
            lo |= (((int)((char) md5Digest[i])) << (i * 8));
        for (int i = 8; i < 16; i++)
            hi |= (((int)((char) md5Digest[i])) << ((i - 8) * 8));
        return new PathHash(new Long(lo).intValue(), new Long(hi).intValue());
    }
}
