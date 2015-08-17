package com.molina.cvmfs.catalog;

import com.molina.cvmfs.common.DatabaseObject;
import com.molina.cvmfs.directoryentry.DirectoryEntry;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author Jose Molina Colmenero
 *
 * Wraps the basic functionality of CernVM-FS Catalogs
 */
public class Catalog extends DatabaseObject {

    public static final char CATALOG_ROOT_PREFIX = 'C';

    protected float schema;
    protected int schemaRevision;
    protected String hash;

    public static Catalog open(String catalogPath) throws SQLException {
        return new Catalog(new File(catalogPath), "");
    }

    public float getSchema() {
        return schema;
    }

    public Catalog(File databaseFile, String catalogHash) throws SQLException {
        super(databaseFile);
        this.hash = catalogHash;
        readProperties();
        guessRootPrefixIfNeeded();
        guessLastModifiedIfNeeded();
        checkValidity();
    }

    protected void checkValidity() {

    }

    protected void guessLastModifiedIfNeeded() {
    }

    protected void guessRootPrefixIfNeeded() {

    }

    protected void readProperties() {
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

    }

    private static String cannonicalizePath(String path) {
        if (path == null)
            return "";
        return new File(path).getAbsolutePath();
    }
}
