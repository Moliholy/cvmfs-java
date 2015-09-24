package com.molina.cvmfs.common;

import org.sqlite.SQLiteConfig;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jose Molina Colmenero
 */
public class DatabaseObject {

    protected File databaseFile;
    private Connection connection;

    public DatabaseObject(File databaseFile) throws IllegalStateException, SQLException {
        this.databaseFile = databaseFile;
        if (this.databaseFile != null && this.databaseFile.exists()) {
            openDatabase();
        } else {
            throw new IllegalStateException("Database file is null or doesn't exist");
        }
    }

    /**
     * Create and configure a database handle to the Catalog
     */
    protected void openDatabase() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            connection = null;
            return;
        }
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        connection = config.createConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        connection.setAutoCommit(false);
    }

    protected PreparedStatement createPreparedStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    protected ResultSet executePreparedStatement(PreparedStatement pe) throws SQLException {
        return pe.executeQuery();
    }

    public boolean open() {
        try {
            openDatabase();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean recycle() {
        return isOpened() && close() && open();
    }

    public boolean isOpened() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean close() {
        try {
            connection.close();
            connection = null;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long databaseSize() {
        return databaseFile.length();
    }

    /**
     * Retrieve all properties stored in the 'properties' table
     */
    public Map<String, Object> readPropertiesTable() throws SQLException {
        ResultSet rs = runSQL("SELECT key, value FROM properties;");
        Map<String, Object> result = new HashMap<String, Object>();
        while (rs.next()) {
            result.put(rs.getString(1), rs.getObject(2));
        }
        rs.close();
        rs.getStatement().close();
        return result;
    }

    /**
     * Run an arbitrary SQL query on the catalog database
     *
     * @param sqlQuery query to run in the database
     * @return the ResultSet obtained after executing the query
     */
    public ResultSet runSQL(String sqlQuery) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(sqlQuery);
    }
}
