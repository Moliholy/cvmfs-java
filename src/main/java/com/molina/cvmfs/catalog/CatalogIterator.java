package com.molina.cvmfs.catalog;


import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;

import java.io.File;
import java.sql.SQLException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Iterates through all directory entries of a Catalog
 */
public class CatalogIterator implements Iterator<DirectoryEntryWrapper> {

    private Catalog catalog;
    private Deque<DirectoryEntryWrapper> backlog;

    public CatalogIterator(Catalog catalog) {
        this.catalog = catalog;
        this.backlog = new LinkedList<DirectoryEntryWrapper>();
        String rootPath = "";
        if (!this.catalog.isRoot()) {
            rootPath = this.catalog.getRootPrefix();
        }
        DirectoryEntryWrapper rootFile =
                new DirectoryEntryWrapper(catalog.findDirectoryEntry(rootPath), rootPath);
        push(rootFile);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    private DirectoryEntryWrapper pop() {
        return backlog.removeFirst();
    }

    public boolean hasNext() {
        return !backlog.isEmpty();
    }

    private void push(DirectoryEntryWrapper directoryEntryWrapper) {
        backlog.addFirst(directoryEntryWrapper);
    }

    public DirectoryEntryWrapper next() {
        if (!hasNext()) {
            throw new UnsupportedOperationException("No more elements");
        }
        return recursionStep();
    }

    public void remove() {
        throw new UnsupportedOperationException("Cannot remove an element");
    }

    private DirectoryEntryWrapper recursionStep() {
        DirectoryEntryWrapper wrapper = pop();
        DirectoryEntry dirent = wrapper.getDirectoryEntry();
        if (dirent.isDirectory()) {
            try {
                List<DirectoryEntry> newDirents = catalog.listDirectorySplitMd5(
                        dirent.getMd5path_1(),
                        dirent.getMd5path_2()
                );
                for (DirectoryEntry newDirent : newDirents) {
                    push(new DirectoryEntryWrapper(newDirent,
                            wrapper.getPath() + File.separator +
                                    newDirent.getName()));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return wrapper;
    }


}
