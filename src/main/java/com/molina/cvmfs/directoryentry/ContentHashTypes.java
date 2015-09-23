package com.molina.cvmfs.directoryentry;

/**
 * @author Jose Molina Colmenero
 *         <p/>
 *         Enumeration of supported content hash types
 */
public class ContentHashTypes {

    public static int UNKNOWN = -1;
    // public static int MD5 = 0  // MD5 is not used as a content hash!
    public static int SHA1 = 1;
    public static int RIPEMD160 = 2;
    public static int UPPPER_BOUND = 3;

    /**
     * Figures out the hash suffix in CVMFS's CAS
     *
     * @param contentHashType hash type to analyze
     * @return the hash suffix that corresponds to the contentHashType
     */
    public static String toSuffix(int contentHashType) {
        if (contentHashType == ContentHashTypes.RIPEMD160)
            return "-rmd160";
        return "";
    }
}
