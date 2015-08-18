package com.molina.cvmfs.catalog;

import com.molina.cvmfs.catalog.exception.CatalogInitializationException;
import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.common.DatabaseObject;
import com.molina.cvmfs.common.PathHash;
import com.molina.cvmfs.directoryentry.Chunk;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.exception.ChunkFileDoesNotMatch;

import javax.crypto.Mac;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * @author Jose Molina Colmenero
 *
 * Wraps the basic functionality of CernVM-FS Catalogs
 */
public class Catalog extends DatabaseObject {

    public static final char CATALOG_ROOT_PREFIX = 'C';

    protected float schema;
    protected float schemaRevision;
    protected int revision;
    protected String hash;
    protected Date lastModified;
    protected String rootPrefix;
    protected int previousRevision;

    public static Catalog open(String catalogPath)
            throws SQLException, CatalogInitializationException {
        return new Catalog(new File(catalogPath), "");
    }

    public float getSchema() {
        return schema;
    }

    public Catalog(File databaseFile, String catalogHash)
            throws SQLException, CatalogInitializationException {
        super(databaseFile);
        hash = catalogHash;
        schemaRevision = 0;
        readProperties();
        guessRootPrefixIfNeeded();
        guessLastModifiedIfNeeded();
        checkValidity();
    }

    /**
     * Check all crucial properties have been found in the database
     */
    protected void checkValidity() throws CatalogInitializationException {
        if (revision == 0)
            throw new CatalogInitializationException(
                    "Catalog lacks a revisionn entry");
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
            rootPrefix = "/";
    }

    /**
     * Detect catalog properties and store them
     */
    protected void readProperties() throws SQLException {
        Map<String, Object> map = readPropertiesTable();
        for (String key : map.keySet()){
            readProperty(key, map.get(key));
        }
    }

    protected void readProperty(String key, Object value){
        if (key.equals("revision"))
            revision = (Integer) value;
        else if (key.equals("schema"))
            schema = (Float) value;
        else if (key.equals("schema_revision"))
            schemaRevision = (Float) value;
        else if (key.equals("last_modified"))
            lastModified = new Date((Long) value);
        else if (key.equals("previous_revision"))
            previousRevision = (Integer) value;
        else if (key.equals("root_prefix"))
            rootPrefix = (String) value;
    }

    public boolean hasNested() {
        return nestedCount() > 0;
    }

    /**
     * Returns the number of nested catalogs in the catalog
     * @return the number of nested catalogs in this catalog
     */
    public int nestedCount(){
        ResultSet rs = null;
        int numCatalogs = 0;
        try {
            rs = runSQL("SELECT count(*) FROM nested_catalogs;");
            if (rs.next())
                numCatalogs = rs.getInt(0);
        } catch (SQLException e) {
            return 0;
        }
        return numCatalogs;
    }

    /**
     * List CatalogReferences to all contained nested catalogs
     * @return array of CatalogReference containing all nested catalogs in this catalog
     */
    public CatalogReference[] listNested() {
        boolean newVersion = (schema <= 1.2 && schemaRevision > 0);
        String sqlQuery;
        if (newVersion) {
            sqlQuery = "SELECT path, sha1, size FROM nested_catalogs";
        } else {
            sqlQuery = "SELECT path, sha1 FROM nested_catalogs";
        }
        ResultSet rs;
        ArrayList<CatalogReference> arr = new ArrayList<CatalogReference>();
        try {
            rs = runSQL(sqlQuery);
            while (rs.next()) {
                String path = rs.getString(0);
                String sha1 = rs.getString(1);
                int size = 0;
                if (newVersion)
                    size = rs.getInt(2);
                arr.add(new CatalogReference(path, sha1, size));
            }
        } catch (SQLException e) {
            return new CatalogReference[0];
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

    /**
     * Find the best matching nested CatalogReference for a given path
     * @param needlePath path to search in the catalog
     * @return The catalogs that best matches the given path
     */
    public CatalogReference findNestedForPath(String needlePath) {
        CatalogReference[] catalogRefs = listNested();
        CatalogReference bestMatch = null;
        int bestMatchScore = 0;
        String realNeedlePath = cannonicalizePath(needlePath);
        for (CatalogReference nestedCatalog : catalogRefs) {
            if (realNeedlePath.startsWith(nestedCatalog.getRootPath()) &&
                    nestedCatalog.getRootPath().length() > bestMatchScore) {
                bestMatchScore = nestedCatalog.getRootPath().length();
                bestMatch = nestedCatalog;
            }
        }
        return bestMatch;
    }

    /**
     * Create a directory listing of the given directory path
     * @param path path to be listed
     * @return a list with all the DirectoryEntries contained in that path
     */
    public DirectoryEntry[] listDirectory(String path) {
        String realPath = cannonicalizePath(path);
        try {
            Mac md5Mac = Mac.getInstance("MD5");
            PathHash parentHash = Common.splitMd5(
                    md5Mac.doFinal(realPath.getBytes()));
            return listDirectorySplitMd5(parentHash.getHash1(),
                    parentHash.getHash2());
        } catch (NoSuchAlgorithmException e) {
            return new DirectoryEntry[0];
        } catch (SQLException e) {
            return new DirectoryEntry[0];
        }
    }

    /**
     * Create a directory listing of DirectoryEntry items based on MD5 path
     * @param parent1 first part of the parent MD5 hash
     * @param parent2 second part of the parent MD5 hash
     */
    public DirectoryEntry[] listDirectorySplitMd5(Integer parent1,
                                                  Integer parent2)
            throws SQLException {
        ResultSet rs = runSQL("SELECT " +
                DirectoryEntry.catalogDatabaseFields() + " FROM catalog " +
                "WHERE parent_1 = " + parent1.toString() +
                " AND parent_2 = " + parent2.toString() +
                " ORDER BY name ASC;");
        ArrayList<DirectoryEntry> arr = new ArrayList<DirectoryEntry>();
        while (rs.next()) {
            arr.add(makeDirectoryEntry(rs));
        }
        return arr.toArray(new DirectoryEntry[arr.size()]);
    }

    private DirectoryEntry makeDirectoryEntry(ResultSet rs)
            throws SQLException {
        DirectoryEntry dirent = new DirectoryEntry(rs);
        readChunks(dirent);
        return dirent;
    }

    /**
     * Finds and adds the file chunk of a DirectoryEntry
     * @param dirent DirectoryEntry that contains chunks
     */
    private void readChunks(DirectoryEntry dirent) throws SQLException {
        if (schema < 2.4)
            return;
        ResultSet rs = runSQL("SELECT " + Chunk.catalogDatabaseFields() +
                        " FROM chunks " +
                        "WHERE md5path_1 = " +
                        Integer.toString(dirent.getMd5path_1()) +
                        " AND md5path_2 = " +
                        Integer.toString(dirent.getMd5path_2()) +
                        " ORDER BY offset ASC");
        try {
            dirent.addChunks(rs);
        } catch (ChunkFileDoesNotMatch chunkFileDoesNotMatch) {
        }
    }

    private static String cannonicalizePath(String path) {
        if (path == null)
            return "";
        return new File(path).getAbsolutePath();
    }
}
