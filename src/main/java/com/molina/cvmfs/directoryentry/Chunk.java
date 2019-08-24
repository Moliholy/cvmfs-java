package com.molina.cvmfs.directoryentry;

import com.molina.cvmfs.directoryentry.exception.ChunkFileDoesNotMatch;
import org.sqldroid.SQLDroidBlob;

import java.sql.SQLException;

/**
 * @author Jose Molina Colmenero
 *         <p/>
 *         Wrapper around file chunks in the CVMFS catalogs
 */
public class Chunk {

    protected int offset;
    protected int size;
    protected String contentHash;
    protected int contentHashType;


    public Chunk(Object[] chunkData, int contentHashType) throws ChunkFileDoesNotMatch {
        if (chunkData.length != 5)
            throw new ChunkFileDoesNotMatch();
        this.offset = (Integer) chunkData[2];
        this.size = (Integer) chunkData[3];
        this.contentHashType = contentHashType;
        if (chunkData[4] instanceof byte[]) {
            this.contentHash = new String((byte[]) chunkData[4]);
        } else if (chunkData[4] instanceof SQLDroidBlob){
            SQLDroidBlob blob = (SQLDroidBlob) chunkData[4];
            try {
                this.contentHash = new String(blob.getBytes(0, (int) blob.length()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static String catalogDatabaseFields() {
        return "md5path_1, md5path_2, offset, size, hash";
    }

    public String contentHashString() {
        String suffix = ContentHashTypes.toSuffix(contentHashType);
        StringBuilder sb = new StringBuilder();
        for (byte b : contentHash.getBytes())
            sb.append(String.format("%02X ", b));
        return sb.toString().toLowerCase() + suffix;
    }
}
