package com.molina.cvmfs.revision;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogIterator;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.repository.exception.NestedCatalogNotFoundException;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Iterates through all directory entries in a whole Repository
 */
public class RevisionIterator implements Iterator<DirectoryEntryWrapper> {

    private Revision revision;
    private Deque<CatalogIterator> catalogStack;

    public RevisionIterator(Revision revision, String catalogHash) {
        this.revision = revision;
        this.catalogStack = new LinkedList<CatalogIterator>();
        Catalog rootCatalog;
        if (catalogHash == null || catalogHash.isEmpty()) {
            rootCatalog = revision.retrieveRootCatalog();
        } else {
            rootCatalog = revision.retrieveCatalog(catalogHash);
        }
        pushCatalog(rootCatalog);
    }

    public RevisionIterator(Revision revision) {
        this(revision, null);
    }

    public DirectoryEntryWrapper next() {
        DirectoryEntryWrapper result = getNextDirent();
        DirectoryEntry dirent = result.getDirectoryEntry();
        if (dirent.isNestedCatalogMountpoint()) {
            try {
                String path = result.getPath();
                Catalog mountedCatalog = revision.retrieveCatalog(path);
                if (mountedCatalog == null) {
                    fetchAndPushCatalog(path);
                } else {
                    pushCatalog(mountedCatalog);
                }
                return next();
            } catch (NestedCatalogNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return result;
    }

    public void remove() {
        // do nothing
    }

    private DirectoryEntryWrapper getNextDirent() {
        CatalogIterator currentCatalog = getCurrentCatalogIterator();
        DirectoryEntryWrapper wrapper = currentCatalog.next();
        while (currentCatalog != null && !currentCatalog.hasNext()) {
            popCatalog();
            currentCatalog = getCurrentCatalogIterator();
        }
        return wrapper;
    }

    private void fetchAndPushCatalog(String catalogMountpoint) throws NestedCatalogNotFoundException {
        Catalog currentCatalog = getCurrentCatalogIterator().getCatalog();
        CatalogReference nestedRef = currentCatalog.findNestedForPath(catalogMountpoint);
        if (nestedRef == null) {
            throw new NestedCatalogNotFoundException(catalogMountpoint);
        }
        Catalog newCatalog = nestedRef.retrieveFrom(revision.getRepository());
        pushCatalog(newCatalog);
    }

    public boolean hasNext() {
        return !catalogStack.isEmpty();
    }

    private void pushCatalog(Catalog catalog) {
        catalogStack.addLast(new CatalogIterator(catalog));
    }

    private CatalogIterator getCurrentCatalogIterator() {
        return catalogStack.peekFirst();
    }

    private CatalogIterator popCatalog() {
        return catalogStack.removeFirst();
    }


}
