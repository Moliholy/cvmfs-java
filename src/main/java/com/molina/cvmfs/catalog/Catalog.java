package com.molina.cvmfs.catalog;

import com.molina.cvmfs.catalog.exception.CatalogInitializationException;
import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.common.DatabaseObject;
import com.molina.cvmfs.common.PathHash;
import com.molina.cvmfs.directoryentry.Chunk;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.directoryentry.exception.ChunkFileDoesNotMatch;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Jose Molina Colmenero
 *         <p/>
 *         Wraps the basic functionality of CernVM-FS Catalogs
 */
public class Catalog extends DatabaseObject implements Iterable<DirectoryEntryWrapper> {

    public static final String CATALOG_ROOT_PREFIX = "C";
    protected static final String LISTING_QUERY = "SELECT " + DirectoryEntry.catalogDatabaseFields() +
            " FROM catalog" +
            " WHERE parent_1 = ? AND" +
            " parent_2 = ?" +
            " ORDER BY name ASC;";
    protected static final String NESTED_COUNT = "SELECT count(*) FROM nested_catalogs;";
    protected static final String READ_CHUNK = "SELECT " + Chunk.catalogDatabaseFields() +
            " FROM chunks WHERE md5path_1 = ? AND md5path_2 = ? ORDER BY offset ASC";
    protected static final String FIND_MD5_PATH = "SELECT " + DirectoryEntry.catalogDatabaseFields() +
            " FROM catalog WHERE md5path_1 = ? AND md5path_2 = ? LIMIT 1;";
    protected float schema;
    protected float schemaRevision;
    protected int revision;
    protected String hash;
    protected Date lastModified;
    protected String rootPrefix;
    protected String previousRevision;
    private PreparedStatement listStatement;
    private PreparedStatement nestedCountStatement;
    private PreparedStatement readChunkStatement;
    private PreparedStatement findMd5PathStatement;
    private PreparedStatement listNestedStatement;

    public Catalog(File databaseFile, String catalogHash)
            throws SQLException, CatalogInitializationException {
        super(databaseFile);
        hash = catalogHash;
        schemaRevision = 0;
        readProperties();
        guessRootPrefixIfNeeded();
        guessLastModifiedIfNeeded();
        checkValidity();
        prepareStatements();
    }

    private void prepareStatements() throws SQLException {
        listStatement = createPreparedStatement(LISTING_QUERY);
        nestedCountStatement = createPreparedStatement(NESTED_COUNT);
        readChunkStatement = createPreparedStatement(READ_CHUNK);
        findMd5PathStatement = createPreparedStatement(FIND_MD5_PATH);
        boolean newVersion = (schema <= 1.2 && schemaRevision > 0);
        String sqlQuery;
        if (newVersion) {
            sqlQuery = "SELECT path, sha1, size FROM nested_catalogs";
        } else {
            sqlQuery = "SELECT path, sha1 FROM nested_catalogs";
        }
        listNestedStatement = createPreparedStatement(sqlQuery);
    }

    public float getSchema() {
        return schema;
    }

    /**
     * Check all crucial properties have been found in the database
     */
    protected void checkValidity() throws CatalogInitializationException {
        if (revision == 0)
            throw new CatalogInitializationException(
                    "Catalog lacks a revision entry");
        if (schema == 0.0f)
            throw new CatalogInitializationException(
                    "Catalog lacks a schema entry");
        if (rootPrefix == null)
            throw new CatalogInitializationException(
                    "Catalog lacks a root prefix entry");
        if (lastModified == null)
            throw new CatalogInitializationException(
                    "Catalog lacks a last modification entry");
    }

    /**
     * Catalogs w/o a last_modified field, we set it to 0
     */
    protected void guessLastModifiedIfNeeded() {
        if (lastModified == null)
            lastModified = new Date(0);
    }

    /**
     * Root catalogs don't have a root prefix property
     */
    protected void guessRootPrefixIfNeeded() {
        if (rootPrefix == null)
            rootPrefix = File.separator;
    }

    /**
     * Detect catalog properties and store them
     */
    protected void readProperties() throws SQLException {
        Map<String, Object> map = readPropertiesTable();
        for (String key : map.keySet()) {
            readProperty(key, map.get(key));
        }
    }

    public float getSchemaRevision() {
        return schemaRevision;
    }

    public int getRevision() {
        return revision;
    }

    public String getHash() {
        return hash;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getRootPrefix() {
        return rootPrefix;
    }

    public String getPreviousRevision() {
        return previousRevision;
    }

    protected void readProperty(String key, Object value) {
        if (key.equals("revision"))
            revision = Integer.valueOf((String) value);
        else if (key.equals("schema"))
            schema = Float.valueOf((String) value);
        else if (key.equals("schema_revision"))
            schemaRevision = Float.valueOf((String) value);
        else if (key.equals("last_modified")) {
            Long valueLong = Long.valueOf((String) value);
            lastModified = new Date(valueLong);
        } else if (key.equals("previous_revision"))
            previousRevision = (String) value;

        else if (key.equals("root_prefix"))
            rootPrefix = (String) value;
    }

    public boolean hasNested() {
        return nestedCount() > 0;
    }

    public boolean isRoot() {
        return rootPrefix.equals(File.separator);
    }

    /**
     * Returns the number of nested catalogs in the catalog
     *
     * @return the number of nested catalogs in this catalog
     */
    public int nestedCount() {
        ResultSet rs = null;
        int numCatalogs = 0;
        try {
            rs = nestedCountStatement.executeQuery();
            if (rs.next())
                numCatalogs = rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return numCatalogs;
    }

    /**
     * List CatalogReferences to all contained nested catalogs
     *
     * @return array of CatalogReference containing all nested catalogs in this catalog
     */
    public CatalogReference[] listNested() {
        boolean newVersion = (schema <= 1.2 && schemaRevision > 0);
        ResultSet rs = null;
        ArrayList<CatalogReference> arr = new ArrayList<CatalogReference>();
        try {
            rs = listNestedStatement.executeQuery();
            while (rs.next()) {
                String path = rs.getString(1);
                String sha1 = rs.getString(2);
                int size = 0;
                if (newVersion)
                    size = rs.getInt(3);
                arr.add(new CatalogReference(path, sha1, size));
            }
        } catch (SQLException e) {
            return new CatalogReference[0];
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return arr.toArray(new CatalogReference[arr.size()]);
    }

    public CatalogStatistics getStatistics() {
        try {
            return new CatalogStatistics(this);
        } catch (SQLException e) {
            return null;
        }
    }

    private boolean pathSanitized(String needlePath, String catalogPath) {
        return needlePath.length() == catalogPath.length() ||
                (needlePath.length() > catalogPath.length() &&
                needlePath.charAt(catalogPath.length()) == File.separatorChar);
    }

    /**
     * Find the best matching nested CatalogReference for a given path
     *
     * @param needlePath path to search in the catalog
     * @return The catalogs that best matches the given path
     */
    public CatalogReference findNestedForPath(String needlePath) {
        CatalogReference[] catalogRefs = listNested();
        CatalogReference bestMatch = null;
        int bestMatchScore = 0;
        String realNeedlePath = Common.canonicalizePath(needlePath);
        for (CatalogReference nestedCatalog : catalogRefs) {
            if (realNeedlePath.startsWith(nestedCatalog.getRootPath()) &&
                    nestedCatalog.getRootPath().length() > bestMatchScore &&
                    pathSanitized(needlePath, nestedCatalog.getRootPath())) {
                bestMatchScore = nestedCatalog.getRootPath().length();
                bestMatch = nestedCatalog;
            }
        }
        return bestMatch;
    }

    /**
     * Create a directory listing of the given directory path
     *
     * @param path path to be listed
     * @return a list with all the DirectoryEntries contained in that path
     */
    public List<DirectoryEntry> listDirectory(String path) {
        String realPath = Common.canonicalizePath(path);
        if (realPath.equals(File.separator))
            realPath = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            PathHash parentHash = Common.splitMd5(md.digest(realPath.getBytes()));
            return listDirectorySplitMd5(parentHash.getHash1(),
                    parentHash.getHash2());
        } catch (NoSuchAlgorithmException e) {
            return new ArrayList<DirectoryEntry>();
        } catch (SQLException e) {
            return new ArrayList<DirectoryEntry>();
        }
    }

    @Override
    public boolean open() {
        boolean result = super.open();
        try {
            readProperties();
            guessRootPrefixIfNeeded();
            guessLastModifiedIfNeeded();
            checkValidity();
            prepareStatements();
        } catch (SQLException e) {
            result = false;
        } catch (CatalogInitializationException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    @Override
    public boolean close() {
        boolean result = true;
        try {
            findMd5PathStatement.close();
            listNestedStatement.close();
            listStatement.close();
            nestedCountStatement.close();
            readChunkStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = false;
        }
        return result && super.close();
    }

    /**
     * Create a directory listing of DirectoryEntry items based on MD5 path
     *
     * @param parent1 first part of the parent MD5 hash
     * @param parent2 second part of the parent MD5 hash
     */
    public List<DirectoryEntry> listDirectorySplitMd5(long parent1, long parent2)
            throws SQLException {
        listStatement.setLong(1, parent1);
        listStatement.setLong(2, parent2);
        ResultSet rs = listStatement.executeQuery();
        ArrayList<DirectoryEntry> arr = new ArrayList<DirectoryEntry>();
        while (rs.next()) {
            arr.add(makeDirectoryEntry(rs));
        }
        rs.close();
        return arr;
    }

    private DirectoryEntry makeDirectoryEntry(ResultSet rs)
            throws SQLException {
        if (rs != null && !rs.isClosed()) {
            DirectoryEntry dirent = new DirectoryEntry(rs);
            readChunks(dirent);
            return dirent;
        }
        return null;
    }

    /**
     * Finds and adds the file chunk of a DirectoryEntry
     *
     * @param dirent DirectoryEntry that contains chunks
     */
    private void readChunks(DirectoryEntry dirent) throws SQLException {
        if (schema < 2.4)
            return;
        readChunkStatement.setLong(1, dirent.getMd5path_1());
        readChunkStatement.setLong(2, dirent.getMd5path_2());
        ResultSet rs = readChunkStatement.executeQuery();
        try {
            dirent.addChunks(rs);
        } catch (ChunkFileDoesNotMatch e) {
            e.printStackTrace();
        }
        rs.close();
    }

    /**
     * Finds the DirectoryEntry for a given path
     *
     * @param rootPath relative path of the DirectoryEntry to find
     * @return the DirectoryEntry that corresponds to path, or null if not found
     */
    public DirectoryEntry findDirectoryEntry(String rootPath) {
        String realPath = Common.canonicalizePath(rootPath);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Path = md.digest(realPath.getBytes());
            return findDirectoryEntryMd5(md5Path);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Finds a DirectoryEntry for a given Md5 hashed path
     *
     * @param md5Path md5 path of the DirectoryEntry to find
     * @return the DirectoryEntry that corresponds to md5Path, or null if not found
     */
    private DirectoryEntry findDirectoryEntryMd5(byte[] md5Path) {
        PathHash pathHash = Common.splitMd5(md5Path);
        return findDirectoryEntrySplitMd5(pathHash);
    }

    /**
     * Finds the DirectoryEntry for the given splitMd5 hashed path
     *
     * @param pathHash split md5 hashed path of the DirectoryEntry to find
     * @return the DirectoryEntry that corresponds to pathHash, or null if not found
     */
    private DirectoryEntry findDirectoryEntrySplitMd5(PathHash pathHash) {
        ResultSet rs = null;
        try {
            findMd5PathStatement.setLong(1, pathHash.getHash1());
            findMd5PathStatement.setLong(2, pathHash.getHash2());
            rs = findMd5PathStatement.executeQuery();
            rs.next();
            return makeDirectoryEntry(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public Iterator<DirectoryEntryWrapper> iterator() {
        return new CatalogIterator(this);
    }

}
