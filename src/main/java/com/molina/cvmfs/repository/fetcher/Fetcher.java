package com.molina.cvmfs.repository.fetcher;

import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipInputStream;

/**
 * Created by Jose Molina Colmenero
 */
public class Fetcher {
    protected Cache cache;
    protected URL source;

    public Fetcher(String source, String cacheDirectory) throws CacheDirectoryNotFound, MalformedURLException {
        this.cache = new Cache(cacheDirectory);
        File f = new File(source);
        if (f.exists() && f.isDirectory()) {
            this.source = new URL("file://" + f.getAbsolutePath());
        } else {
            this.source = new URL(source);
        }
    }

    protected String makeFileURL(String fileName) {
        return source.toString() + "/" + fileName;
    }

    private static void decompress(InputStream is, File cachedFile) throws IOException {
        int bufferSize = 2048;
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new BufferedInputStream(is));
            if (zis.getNextEntry() != null) {
                int count;
                byte data[] = new byte[bufferSize];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(cachedFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos, bufferSize);
                while ((count = zis.read(data, 0, bufferSize)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.close();
            }
        } finally {
            if (zis != null)
                zis.close();
        }

    }

    protected static void downloadContentAndStore(File cachedFile, String fileURL) throws IOException {
        BufferedInputStream bin = null;
        FileOutputStream fout = null;
        int bufferSize = 1024;
        try {
            bin = new BufferedInputStream(new URL(fileURL).openStream());
            fout = new FileOutputStream(cachedFile);

            byte data[] = new byte[bufferSize];
            int count;
            while ((count = bin.read(data, 0, bufferSize)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            if (bin != null)
                bin.close();
            if (fout != null)
                fout.close();
        }
    }

    protected static void downloadContentAndDecompress(File cachedFile, String fileURL) throws IOException {
        URL url = new URL(fileURL);
        InputStream openStream = url.openStream();
        Fetcher.decompress(openStream, cachedFile);
    }

    protected File retrieveFileFromSource(String fileName) throws IOException {
        String fileURL = makeFileURL(fileName);
        File cachedFile = cache.add(fileName);
        Fetcher.downloadContentAndDecompress(cachedFile, fileURL);
        return cache.get(fileName);
    }

    public File retrieveRawFile(String fileName) throws IOException {
        File cachedFile = cache.add(fileName);
        String fileURL = makeFileURL(fileName);
        Fetcher.downloadContentAndStore(cachedFile, fileURL);
        return cache.get(fileName);
    }

    public File retrieveFile(String fileName) throws IOException {
        File cachedFile = cache.get(fileName);
        if (cachedFile == null) {
            return retrieveFileFromSource(fileName);
        }
        return cachedFile;
    }
}
