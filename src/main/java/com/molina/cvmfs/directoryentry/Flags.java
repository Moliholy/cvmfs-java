package com.molina.cvmfs.directoryentry;

/**
 * @author Jose Molina Colmenero
 *         <p/>
 *         Definition of used dirent flags
 */
public class Flags {

    public static int DIRECTORY = 1;
    public static int NESTED_CATALOG_MOUNTPOINT = 2;
    public static int NESTED_cATALOG_ROOT = 32;
    public static int FILE = 4;
    public static int LINK = 8;
    public static int FILE_STAT = 16;  // unused
    public static int FILE_CHUNK = 64;
    public static int CONTENT_HASH_TYPE = 256 + 512 + 1024;

}
