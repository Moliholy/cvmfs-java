package com.molina.cvmfs.history;

import com.molina.cvmfs.common.DatabaseObject;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around CernVM-FS 2.1.x repository history databases
 *
 * @author Jose Molina Colmenero
 */
public class History extends DatabaseObject {

    private String schema;
    private String fqrn;

    public History(File databaseFile) throws IllegalStateException, SQLException {
        super(databaseFile);
        readProperties();
    }

    public static History open(String historyPath) throws SQLException {
        return new History(new File(historyPath));
    }

    public String getSchema() {
        return schema;
    }

    public String getFqrn() {
        return fqrn;
    }

    private void readProperties() throws SQLException {
        Map<String, Object> properties = readPropertiesTable();
        assert (properties.containsKey("schema") &&
                properties.get("schema").equals("1.0"));
        if (properties.containsKey("fqrn"))
            fqrn = (String) properties.get("fqrn");
        schema = (String) properties.get("schema");
    }

    private RevisionTag getTagByQuery(String query) throws SQLException {
        ResultSet result = runSQL(query);
        if (result != null) {
            return new RevisionTag(result);
        }
        return null;
    }

    public List<RevisionTag> listTags() throws SQLException {
        ResultSet results = runSQL(RevisionTag.sqlQueryAll());
        List<RevisionTag> tags = new ArrayList<RevisionTag>();
        while (results.next()) {
            tags.add(new RevisionTag(results));
        }
        return tags;
    }

    public RevisionTag getTagByName(String name) throws SQLException {
        return getTagByQuery(RevisionTag.sqlQueryName(name));
    }

    public RevisionTag getTagByRevision(int revision) throws SQLException {
        return getTagByQuery(RevisionTag.sqlQueryRevision(revision));
    }

    public RevisionTag getTagByDate(long timestamp) throws SQLException {
        return getTagByQuery(RevisionTag.sqlQueryDate(timestamp));
    }

}
