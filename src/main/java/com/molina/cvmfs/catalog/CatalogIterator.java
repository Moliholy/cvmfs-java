package com.molina.cvmfs.catalog;


import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.exception.StopIterationException;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Iterates through all directory entries of a Catalog
 */
public class CatalogIterator {

    private Catalog catalog;
    private Deque<DirectoryEntryWrapper> backlog;

    public CatalogIterator(Catalog catalog) {
        this.catalog = catalog;
        this.backlog = new ArrayDeque<DirectoryEntryWrapper>();
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
        return backlog.pop();
    }

    private boolean hasMore() {
        return !backlog.isEmpty();
    }

    private void push(DirectoryEntryWrapper directoryEntryWrapper) {
        backlog.push(directoryEntryWrapper);
    }

    public DirectoryEntryWrapper next() throws StopIterationException {
        if (!hasMore()) {
            throw new StopIterationException();
        }
        return recursionStep();
    }

    private DirectoryEntryWrapper recursionStep() {
        DirectoryEntryWrapper wrapper = pop();
        DirectoryEntry dirent = wrapper.getDirectoryEntry();
        if (dirent.isDirectory()) {
            try {
                DirectoryEntry[] newDirents = catalog.listDirectorySplitMd5(
                        dirent.getMd5path_1(),
                        dirent.getMd5path_2()
                );
                for (DirectoryEntry newDirent : newDirents) {
                    push(new DirectoryEntryWrapper(newDirent,
                            wrapper.getPath() + "/" + newDirent.getName()));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return wrapper;
    }


}
