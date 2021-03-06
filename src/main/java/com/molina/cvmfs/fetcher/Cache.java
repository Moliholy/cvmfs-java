package com.molina.cvmfs.fetcher;


import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * @author Jose Molina Colmenero
 */
public class Cache {
    protected File cacheDirectory;

    public Cache(String cacheDirectoryPath) throws CacheDirectoryNotFound, IOException {
        cacheDirectory = new File(cacheDirectoryPath).getAbsoluteFile();
        if (!cacheDirectory.exists()) {
            throw new CacheDirectoryNotFound(cacheDirectory.toString());
        }
        if (cacheDirectory.isDirectory() && cacheDirectory.canWrite()) {
            createCacheStructure();
        } else {
            throw new CacheDirectoryNotFound(cacheDirectory.toString());
        }
    }

    protected void cleanup_metadata() throws IOException {
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().startsWith(".cvmfs");
            }
        };
        for (File f : cacheDirectory.listFiles(fileFilter)) {
            if (!f.delete())
                throw new IOException(f.getAbsolutePath());
        }
    }

    protected boolean createDirectory(String path) {
        String cacheFullPath = cacheDirectory.getAbsolutePath() + File.separator + path;
        File newDirectory = new File(cacheFullPath);
        return !newDirectory.exists() && newDirectory.mkdir();
    }

    protected void createCacheStructure() throws IOException {
        createDirectory("data");
        for (int i = 0x00; i <= 0xff; i++) {
            String newFolder = Integer.toHexString(i);
            if (newFolder.length() == 1)
                newFolder = "0" + newFolder;
            File newFile = new File(cacheDirectory.getAbsolutePath() + File.separator + "data" +
                    File.separator + newFolder);
            if (!newFile.exists() && !newFile.mkdir())
                throw new IOException("Cannot open " + newFile.getAbsolutePath());
        }
    }

    public String getCachePath() {
        return cacheDirectory.getAbsolutePath();
    }

    public File add(String fileName) {
        String fullPath = cacheDirectory.getAbsolutePath() + File.separator + fileName;
        return new File(fullPath);
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
