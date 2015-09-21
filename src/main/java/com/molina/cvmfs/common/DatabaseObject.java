package com.molina.cvmfs.common;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jose Molina Colmenero
 */
public class DatabaseObject {

    protected File databaseFile;
    protected Connection connection;

    public DatabaseObject(File databaseFile) throws SQLException {
        this.databaseFile = databaseFile;
        openDatabase();
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
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        connection.setAutoCommit(false);
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
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
            result.put(rs.getString(0), rs.getObject(1));
        }
        rs.close();
        return result;
    }

    /**
     * Run an arbitrary SQL query on the catalog database
     * @param sqlQuery query to run in the database
     * @return the ResultSet obtained after executing the query
     */
    public ResultSet runSQL(String sqlQuery) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sqlQuery);
        statement.close();
        return rs;
    }
}
