package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.CatalogIterator;
import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.repository.exception.NestedCatalogNotFoundException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Iterates through all directory entries in a whole Repository
 */
public class RepositoryIterator implements Iterator<DirectoryEntryWrapper> {

    private Repository repository;
    private Deque<CatalogIterator> catalogStack;

    public RepositoryIterator(Repository repository, String catalogHash) {
        this.repository = repository;
        this.catalogStack = new ArrayDeque<CatalogIterator>();
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
                fetchAndPushCatalog(result.getPath());
            } catch (NestedCatalogNotFoundException e) {
                e.printStackTrace();
                return null;
            }
            return next();
        }
        return result;
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
            throw new NestedCatalogNotFoundException(repository.getFqrn());
        }
        Catalog newCatalog = nestedRef.retrieveFrom(repository);
        pushCatalog(newCatalog);
    }

    public boolean hasNext() {
        return !catalogStack.isEmpty();
    }

    private void pushCatalog(Catalog catalog) {
        catalogStack.push(new CatalogIterator(catalog));
    }

    private CatalogIterator getCurrentCatalogIterator() {
        return catalogStack.peekLast();
    }

    private CatalogIterator popCatalog() {
        return catalogStack.pop();
    }


}
