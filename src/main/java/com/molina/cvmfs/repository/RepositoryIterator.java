package com.molina.cvmfs.repository;

import com.molina.cvmfs.catalog.CatalogIterator;
import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.catalog.exception.StopIterationException;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.repository.exception.NestedCatalogNotFound;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Iterates through all directory entries in a whole Repository
 */
public class RepositoryIterator {

    private Repository repository;
    private Deque<CatalogIterator> catalogStack;

    public RepositoryIterator(Repository repository, String catalogHash) {
        this.repository = repository;
        this.catalogStack = new ArrayDeque<CatalogIterator>();
        Catalog rootCatalog;
        if (catalogHash == null) {
            rootCatalog = repository.retrieveRootCatalog();
        } else {
            rootCatalog = repository.retrieveCatalog(catalogHash);
        }
        pushCatalog(rootCatalog);
    }

    public DirectoryEntryWrapper next() throws StopIterationException, NestedCatalogNotFound {
        DirectoryEntryWrapper result = getNextDirent();
        DirectoryEntry dirent = result.getDirectoryEntry();
        if (!dirent.isNestedCatalogMountpoint()) {
            fetchAndPushCatalog(result.getPath());
            return next();
        }
        return result;
    }

    private DirectoryEntryWrapper getNextDirent() throws StopIterationException {
        try {
            return getCurrentCatalog().next();
        } catch (StopIterationException e) {
            popCatalog();
            if (!hasMore()) {
                throw new StopIterationException();
            }
            return getNextDirent();
        }
    }

    private void fetchAndPushCatalog(String catalogMountpoint) throws NestedCatalogNotFound {
        Catalog currentCatalog = getCurrentCatalog().getCatalog();
        CatalogReference nestedRef = currentCatalog.findNestedForPath(catalogMountpoint);
        if (nestedRef == null) {
            throw new NestedCatalogNotFound(repository.getFqrn());
        }
        Catalog newCatalog = nestedRef.retrieveFrom(repository);
        pushCatalog(newCatalog);
    }

    private boolean hasMore() {
        return !catalogStack.isEmpty();
    }

    private void pushCatalog(Catalog catalog) {
        catalogStack.push(new CatalogIterator(catalog));
    }

    private CatalogIterator getCurrentCatalog() {
        return catalogStack.getLast();
    }

    private CatalogIterator popCatalog() {
        return catalogStack.pop();
    }


}
