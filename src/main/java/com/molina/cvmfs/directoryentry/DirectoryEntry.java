package com.molina.cvmfs.directoryentry;

import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.common.PathHash;
import com.molina.cvmfs.directoryentry.exception.ChunkFileDoesNotMatch;
import com.molina.cvmfs.directoryentry.exception.DirectoryEntryCreationException;
import com.molina.cvmfs.directoryentry.exception.DirectoryEntryInvalidObject;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepository;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author Jose Molina Colmenero
 *
 * Wrapper around a DirectoryEntry as it is saved in the Catalogs
 */
public class DirectoryEntry {

    protected int md5path_1;
    protected int md5path_2;
    protected int parent_1;
    protected int parent_2;
    protected String contentHash;
    protected int flags;
    protected int size;
    protected int mode;
    protected long mtime;
    protected String name;
    protected String symlink;
    protected ArrayList<Chunk> chunks;
    protected int contentHashType;

    public DirectoryEntry(ResultSet resultSet) throws SQLException {
        // see DirectoryEntry.catalogDatabaseFields()
        chunks = new ArrayList<Chunk>();
        md5path_1 = resultSet.getInt("md5path_1");
        md5path_2 = resultSet.getInt("md5path_2");
        parent_1 = resultSet.getInt("parent_1");
        parent_2 = resultSet.getInt("parent_2");
        contentHash = resultSet.getString("hash");
        flags = resultSet.getInt("flags");
        size = resultSet.getInt("size");
        mode = resultSet.getInt("mode");
        mtime = resultSet.getLong("mtime");
        name = resultSet.getString("name");
        symlink = resultSet.getString("symlink");
        readContentHashType();
    }

    public File retrieveFrom(Repository repository)
            throws DirectoryEntryInvalidObject, FileNotFoundInRepository {
        if (isSymplink() || isDirectory())
            throw new DirectoryEntryInvalidObject();
        return repository.retrieveObject(contentHashString());
    }

    public boolean isDirectory() {
        return (flags & Flags.DIRECTORY) > 0;
    }

    public boolean isNestedCatalogMountpoint() {
        return (flags & Flags.NESTED_CATALOG_MOUNTPOINT) > 0;
    }

    public boolean isNestedCatalogRoot() {
        return (flags & Flags.NESTED_cATALOG_ROOT) > 0;
    }

    public boolean isFile() {
        return (flags & Flags.FILE) > 0;
    }

    public boolean isSymplink() {
        return (flags & Flags.LINK) > 0;
    }

    public PathHash pathHash() {
        return new PathHash(md5path_1, md5path_2);
    }

    public PathHash parentHash() {
        return new PathHash(parent_1, parent_2);
    }

    public String contentHashString() {
        String suffix = ContentHashTypes.toSuffix(contentHashType);
        return Common.binaryBufferToHexString(contentHash.getBytes()) + suffix;
    }

    public boolean hasChunks() {
        return !chunks.isEmpty();
    }

    public void addChunks(ResultSet resultSet)
            throws SQLException, ChunkFileDoesNotMatch {
        chunks.clear();
        while (resultSet.next()) {
            Object[] chunkData = new Object[5];
            for (int i = 0; i < chunkData.length; i++)
                chunkData[i] = resultSet.getObject(i + 1);
            chunks.add(new Chunk(chunkData, contentHashType));
        }
    }

    protected void readContentHashType() {
        int bitMask = Flags.CONTENT_HASH_TYPE;
        int rightShifts = 0;
        while ((bitMask & 1) == 0) {
            bitMask = bitMask >> 1;
            rightShifts += 1;
        }
        int hashType = ((flags & Flags.CONTENT_HASH_TYPE) >> rightShifts) + 1;
        if (hashType > 0 && hashType < ContentHashTypes.UPPPER_BOUND)
            contentHashType = hashType;
        else
            contentHashType = ContentHashTypes.UNKNOWN;
    }

    public static String catalogDatabaseFields() {
        // see the constructor of this class
        return "md5path_1, md5path_2, parent_1, parent_2, hash, flags, " +
                "size, mode, mtime, name, symlink";
    }

    public int getMd5path_1() {
        return md5path_1;
    }

    public int getMd5path_2() {
        return md5path_2;
    }

    public int getParent_1() {
        return parent_1;
    }

    public int getParent_2() {
        return parent_2;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getFlags() {
        return flags;
    }

    public int getSize() {
        return size;
    }

    public int getMode() {
        return mode;
    }

    public long getMtime() {
        return mtime;
    }

    public String getName() {
        return name;
    }

    public String getSymlink() {
        return symlink;
    }

    public ArrayList<Chunk> getChunks() {
        return chunks;
    }

    public int getContentHashType() {
        return contentHashType;
    }
}
