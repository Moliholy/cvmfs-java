package com.molina.cvmfs.fetcher;

import com.molina.cvmfs.common.Common;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepositoryException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * @author Jose Molina Colmenero
 */
public class Fetcher {
    private static final int FETCHER_BUFFER_SIZE = 8192;

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

    private static void decompress(InputStream is, File cachedFile) throws IOException {
        byte[] inputBuffer = new byte[FETCHER_BUFFER_SIZE];
        byte[] outputBuffer = new byte[FETCHER_BUFFER_SIZE * 2];
        boolean hashesMatch = true;
        FileOutputStream fos = new FileOutputStream(cachedFile);
        BufferedOutputStream dest = new BufferedOutputStream(fos, FETCHER_BUFFER_SIZE);
        BufferedInputStream stream = new BufferedInputStream(is, FETCHER_BUFFER_SIZE);
        int pos = cachedFile.getAbsolutePath().lastIndexOf(File.separator);
        String hash = cachedFile.getAbsolutePath()
                .substring(pos - 2)
                .replace(File.separator, "")
                .substring(0, 40);
        Inflater inflater = new Inflater();
        int bytesRead;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            while ((bytesRead = stream.read(inputBuffer)) != -1) {
                digest.update(inputBuffer, 0, bytesRead);
                inflater.setInput(inputBuffer, 0, bytesRead);
                while (!inflater.needsInput()) {
                    int bytesDecompressed = inflater.inflate(outputBuffer);
                    dest.write(outputBuffer, 0, bytesDecompressed);
                }
            }
            String encodedDigest = Common.binaryBufferToHexString(digest.digest());
            if (!encodedDigest.equals(hash)) {
                System.err.println("Downloaded hashes do not match!\n" +
                "\tDownloaded: " + encodedDigest + "\n" +
                "\tExpected:   " + hash);
                hashesMatch = false;
            }
        } catch (DataFormatException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            hashesMatch = false;
        } finally {
            dest.close();
            stream.close();
            inflater.end();
            if (!hashesMatch) {
                // remove the recently downloaded file because it is corrupted
                cachedFile.delete();
            }
        }

    }

    protected static void downloadContentAndStore(File cachedFile, String fileURL) throws IOException {
        BufferedInputStream bin = null;
        BufferedOutputStream fout = null;
        try {
            bin = new BufferedInputStream(new URL(fileURL).openStream(), FETCHER_BUFFER_SIZE);
            fout = new BufferedOutputStream(new FileOutputStream(cachedFile), FETCHER_BUFFER_SIZE);

            byte data[] = new byte[FETCHER_BUFFER_SIZE];
            int count;
            while ((count = bin.read(data, 0, FETCHER_BUFFER_SIZE)) != -1) {
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

    protected String makeFileURL(String fileName) {
        return source.toString() + File.separator + fileName;
    }

    protected File retrieveFileFromSource(String fileName) throws FileNotFoundInRepositoryException {
        String fileURL = makeFileURL(fileName);
        File cachedFile = cache.add(fileName);
        try {
            Fetcher.downloadContentAndDecompress(cachedFile, fileURL);
        } catch (IOException e) {
            throw new FileNotFoundInRepositoryException(fileName);
        }
        return cache.get(fileName);
    }

    /**
     * Method to retrieve a file from the cache if exists, or from
     * the repository if it doesn't. In case it has to be retrieved from
     * the repository it won't be decompressed
     *
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

    public File retrieveFile(String fileName) throws FileNotFoundInRepositoryException {
        File cachedFile = cache.get(fileName);
        if (cachedFile == null) {
            return retrieveFileFromSource(fileName);
        }
        return cachedFile;
    }
}
