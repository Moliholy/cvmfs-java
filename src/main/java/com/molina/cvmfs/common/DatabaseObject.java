package com.molina.cvmfs.common;

import android.database.sqlite.SQLiteDatabase;
import org.sqldroid.SQLDroidDriver;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jose Molina Colmenero
 */
public class DatabaseObject {

    protected File databaseFile;
    private Connection connection;
    private boolean onAndroid;

    public DatabaseObject(File databaseFile) throws IllegalStateException, SQLException {
        this.databaseFile = databaseFile;
        if (this.databaseFile != null && this.databaseFile.exists()) {
            openDatabase();
        } else {
            throw new IllegalStateException("Database file is null or doesn't exist");
        }
    }

    private void loadDriver() throws ClassNotFoundException {
        String driverName = "";
        if (System.getProperty("java.vm.name").equalsIgnoreCase("Dalvik")) {
            // we are on android
            // this library can be found here: https://github.com/SQLDroid/SQLDroid/
            Class.forName("org.sqldroid.SQLDroidDriver");
            onAndroid = true;
        } else {
            // we are in normal java
            Class.forName("org.sqlite.JDBC");
            onAndroid = false;
        }
    }

    /**
     * Create and configure a database handle to the Catalog
     */
    protected void openDatabase() throws SQLException {
        try {
            loadDriver();
        } catch (ClassNotFoundException e) {
            connection = null;
            return;
        }
        String connectionURL = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        if (!onAndroid) {
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            config.setOpenMode(SQLiteOpenMode.NOMUTEX);
            config.setOpenMode(SQLiteOpenMode.PRIVATECACHE);
            config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
            connection = config.createConnection(connectionURL);
        } else {
            Properties p = new Properties();
            p.put(SQLDroidDriver.DATABASE_FLAGS,
                    SQLiteDatabase.CREATE_IF_NECESSARY
                            | SQLiteDatabase.OPEN_READONLY
                            | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            connection = DriverManager.getConnection(connectionURL, p);
            connection.setReadOnly(true);
        }
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
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
        Statement statement = createStatement();
        ResultSet rs = statement.executeQuery("SELECT key, value FROM properties;");
        Map<String, Object> result = new HashMap<String, Object>();
        while (rs.next()) {
            result.put(rs.getString(1), rs.getObject(2));
        }
        rs.close();
        statement.close();
        return result;
    }

    /**
     * Create a new statement for this database
     *
     * @return the ResultSet obtained after executing the query
     */
    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }
}
