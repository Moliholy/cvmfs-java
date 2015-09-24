package com.molina.cvmfs.repository;

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
public class RepositoryIterator implements Iterator<DirectoryEntryWrapper> {

    private Repository repository;
    private Deque<CatalogIterator> catalogStack;

    public RepositoryIterator(Repository repository, String catalogHash) {
        this.repository = repository;
        this.catalogStack = new LinkedList<CatalogIterator>();
        Catalog rootCatalog;
        if (catalogHash == null || catalogHash.isEmpty()) {
            rootCatalog = repository.retrieveRootCatalog();
        } else {
            rootCatalog = repository.retrieveCatalog(catalogHash);
        }
        pushCatalog(rootCatalog);
    }

    public RepositoryIterator(Repository repository) {
        this(repository, null);
    }

    public DirectoryEntryWrapper next() {
        DirectoryEntryWrapper result = getNextDirent();
        DirectoryEntry dirent = result.getDirectoryEntry();
        if (dirent.isNestedCatalogMountpoint()) {
            try {
                String path = result.getPath();
                Catalog mountedCatalog = repository.getMountedCatalog(path);
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

    private DirectoryEntryWrapper getNextDirent() {
        CatalogIterator currentCatalog = getCurrentCatalogIterator();
        DirectoryEntryWrapper wrapper = currentCatalog.next();
        while (currentCatalog != null && !currentCatalog.hasNext()) {
            CatalogIterator ci = popCatalog();
            ci.getCatalog().close();
            currentCatalog = getCurrentCatalogIterator();
            System.gc();
        }
        return wrapper;
    }

    private void fetchAndPushCatalog(String catalogMountpoint) throws NestedCatalogNotFoundException {
        Catalog currentCatalog = getCurrentCatalogIterator().getCatalog();
        CatalogReference nestedRef = currentCatalog.findNestedForPath(catalogMountpoint);
        if (nestedRef == null) {
            throw new NestedCatalogNotFoundException(catalogMountpoint);
        }
        Catalog newCatalog = nestedRef.retrieveFrom(repository);
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
