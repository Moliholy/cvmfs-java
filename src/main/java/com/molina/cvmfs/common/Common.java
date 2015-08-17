package com.molina.cvmfs.common;

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
}
