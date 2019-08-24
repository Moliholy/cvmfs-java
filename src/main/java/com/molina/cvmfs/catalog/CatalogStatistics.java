package com.molina.cvmfs.catalog;

import com.molina.cvmfs.catalog.exception.CounterNotFound;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jose Molina Colmenero
 *         Provides convenience data wrapper around catalog statistics
 */
public class CatalogStatistics {

    protected Catalog catalog;
    protected Map<String, Integer> stats;

    public CatalogStatistics(Catalog catalog) throws SQLException {
        this.catalog = catalog;
        this.stats = new HashMap<>();
        if (this.catalog.getSchema() >= 2.1)
            readStatistics();
    }

    public int numEntries() throws CounterNotFound {
        return getStat("regular") + getStat("dir") + getStat("symlink");
    }

    public int numSubtreeEntries() throws CounterNotFound {
        return getStat("all_regular") + getStat("all_dir") + getStat("all_symlink");
    }

    public int numChunkedFiles() throws CounterNotFound {
        return getStat("chunked");
    }

    public int numSubTreeChunked() throws CounterNotFound {
        return getStat("all_chunked");
    }

    public int numFileChunks() throws CounterNotFound {
        return getStat("chunks");
    }

    public int numSubtreeFileChunks() throws CounterNotFound {
        return getStat("all_chunks");
    }

    public int dataSize() throws CounterNotFound {
        return getStat("file_size");
    }

    public int subtreeDataSize() throws CounterNotFound {
        return getStat("all_file_size");
    }

    public int[] getAllFields() throws CounterNotFound {
        int[] result = new int[8];
        result[0] = getStat("all_regular");
        result[1] = getStat("all_dir");
        result[2] = getStat("all_symlink");
        result[3] = getStat("all_file_size");
        result[4] = getStat("all_chunked");
        result[5] = getStat("all_chunked_size");
        result[6] = getStat("all_chunks");
        result[7] = getStat("all_nested");
        return result;
    }

    protected void readStatistics() throws SQLException {
        Statement statement = catalog.createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM statistics ORDER BY counter;");
        while (rs.next()) {
            String stat = rs.getString(1);
            Integer value = rs.getInt(2);
            if (stat.startsWith("self_"))
                stats.put(stat.substring(5), value);
            else if (stat.startsWith("subtree_")) {
                String substr = stat.substring(8);
                stats.put("all_" + substr, value + stats.get(substr));
            }
        }
        rs.close();
        statement.close();
    }

    protected Integer getStat(String statName) throws CounterNotFound {
        if (!stats.containsKey(statName))
            throw new CounterNotFound(statName);
        return stats.get(statName);
    }
}
