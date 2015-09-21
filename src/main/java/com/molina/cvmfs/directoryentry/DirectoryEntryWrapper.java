package com.molina.cvmfs.directoryentry;


public class DirectoryEntryWrapper {

    private DirectoryEntry directoryEntry;
    private String path;

    public DirectoryEntryWrapper(DirectoryEntry directoryEntry, String path) {
        this.directoryEntry = directoryEntry;
        this.path = path;
    }

    public DirectoryEntry getDirectoryEntry() {
        return directoryEntry;
    }

    public String getPath() {
        return path;
    }

    public void setDirectoryEntry(DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
