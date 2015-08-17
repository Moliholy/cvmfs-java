package com.molina.cvmfs.catalog;

import com.molina.cvmfs.common.DatabaseObject;

import java.io.File;
import java.sql.SQLException;

/**
 * @author Jose Molina Colmenero
 */
public class Catalog extends DatabaseObject {

    public static final char CATALOG_ROOT_PREFIX = 'C';

    protected float schema;

    public static Catalog open(String catalogPath) throws SQLException, ClassNotFoundException {
        return new Catalog(new File(catalogPath));
    }

    public float getSchema() {
        return schema;
    }

    public Catalog(File databaseFile) throws SQLException, ClassNotFoundException {
        super(databaseFile);
    }
}
