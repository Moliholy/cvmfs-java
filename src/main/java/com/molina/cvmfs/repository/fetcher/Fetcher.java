package com.molina.cvmfs.repository.fetcher;

import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepository;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.*;

/**
 * @author Jose Molina Colmenero
 */
public class Fetcher {
    protected Cache cache;
    protected URL source;

    public Fetcher(String source, String cacheDirectory) throws CacheDirectoryNotFound, IOException {
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
        byte[] inputBuffer = new byte[bufferSize];
        FileOutputStream fos = new FileOutputStream(cachedFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, bufferSize);
        InflaterInputStream decompresserStream = new InflaterInputStream(new BufferedInputStream(is));
        int bytesRead;
        try {
            while ((bytesRead = decompresserStream.read(inputBuffer)) != -1) {
                dest.write(inputBuffer, 0, bytesRead);
            }
        } finally {
            dest.close();
            decompresserStream.close();
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
        URLConnection connection = url.openConnection();
        InputStream rawStream = connection.getInputStream();
        Fetcher.decompress(rawStream, cachedFile);
    }

    protected File retrieveFileFromSource(String fileName) throws FileNotFoundInRepository {
        String fileURL = makeFileURL(fileName);
        File cachedFile = cache.add(fileName);
        try {
            Fetcher.downloadContentAndDecompress(cachedFile, fileURL);
        } catch (IOException e) {
            throw new FileNotFoundInRepository(fileName);
        }
        return cache.get(fileName);
    }

    /**
     * Method to retrieve a file from the cahe if exists, or from
     * the repository if it doesn't. In case it has to be retrieved from
     * the repository it won't be decompressed
     * @param fileName name of the file in the repository
     * @return a read-only file object that represents the cached file
     * @throws IOException if the file doesn't exists in the repository
     */
    public File retrieveRawFile(String fileName) throws IOException {
        File cachedFile = cache.add(fileName);
        String fileURL = makeFileURL(fileName);
        Fetcher.downloadContentAndStore(cachedFile, fileURL);
        return cache.get(fileName);
    }

    public File retrieveFile(String fileName) throws FileNotFoundInRepository {
        File cachedFile = cache.get(fileName);
        if (cachedFile == null) {
            return retrieveFileFromSource(fileName);
        }
        return cachedFile;
    }
}
