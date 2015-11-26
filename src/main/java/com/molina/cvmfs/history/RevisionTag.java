package com.molina.cvmfs.history;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Jose Molina Colmenero
 */
public class RevisionTag {

    private String name;
    private String hash;
    private int revision;
    private long timestamp;
    private int channel;
    private String description;

    public RevisionTag(ResultSet rs) throws SQLException {
        name = rs.getString(1);
        hash = rs.getString(2);
        revision = rs.getInt(3);
        timestamp = rs.getLong(4);
        channel = rs.getInt(5);
        description = rs.getString(6);
    }

    private static String databaseFields() {
        return "name, hash, revision, timestamp, channel, description";
    }

    public static String sqlQueryAll() {
        return "SELECT " + RevisionTag.databaseFields() +
                " FROM tags ORDER BY timestamp DESC";
    }

    public static String sqlQueryName(String name) {
        return "SELECT " + RevisionTag.databaseFields() +
                " FROM tags WHERE name=" + name + " LIMIT 1";
    }

    public static String sqlQueryRevision(int revision) {
        return "SELECT " + RevisionTag.databaseFields() +
                " FROM tags WHERE revision=" + revision + " LIMIT 1";
    }

    public static String sqlQueryDate(long timestamp) {
        return "SELECT " + RevisionTag.databaseFields() +
                " FROM tags WHERE timestamp >" + timestamp +
                " ORDER BY timestamp ASC LIMIT 1";
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public int getRevision() {
        return revision;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }
}
