package com.molina.cvmfs.repository.fetcher;


import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * @author Jose Molina Colmenero
 */
public class Cache {
    File cacheDirectory;

    public Cache(String cacheDirectoryPath) throws CacheDirectoryNotFound {
        cacheDirectory = new File(cacheDirectoryPath).getAbsoluteFile();
        if (cacheDirectory.isDirectory() && cacheDirectory.canWrite()) {
            createCacheStructure();
        } else {
            throw new CacheDirectoryNotFound(cacheDirectory.toString());
        }
    }

    protected void cleanup_metadata() {
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().startsWith(".cvmfs");
            }
        };
        for (File f : cacheDirectory.listFiles(fileFilter)) {
            f.delete();
        }
    }

    protected boolean createDirectory(String path) {
        String cacheFullPath = cacheDirectory.getAbsolutePath() + File.pathSeparator + path;
        File newDirectory = new File(cacheFullPath);
        if (!newDirectory.exists() && newDirectory.mkdir()) {
            return true;
        }
        return false;
    }

    protected void createCacheStructure() {
        createDirectory("data");
        for (int i = 0x00; i <= 0xff; i++) {
            String newFolder = Integer.toHexString(i).substring(2, 4);
            File newFile = new File(cacheDirectory.getAbsolutePath() + File.pathSeparator + "data" +
                    File.pathSeparator + newFolder);
            newFile.mkdir();
        }
    }

    public String getCachePath() {
        return cacheDirectory.getAbsolutePath();
    }

    public File add(String fileName) {
        String fullPath = cacheDirectory.getAbsolutePath() + File.separator + fileName;
        File file = new File(fullPath);
        if (!file.isFile()) {  // we return the file if it doesn't exist
            return file;
        }
        return null;
    }

    public File get(String fileName) {
        String fullPath = cacheDirectory.getAbsolutePath() + File.separator + fileName;
        File file = new File(fullPath);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    public void evict() {
        String dataPath = cacheDirectory.getAbsolutePath() + File.separator + "data";
        File dataFile = new File(dataPath);
        if (dataFile.exists() && dataFile.isDirectory()) {
            try {
                FileUtils.deleteDirectory(dataFile);
                createCacheStructure();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
