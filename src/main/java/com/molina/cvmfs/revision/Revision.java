package com.molina.cvmfs.revision;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogIterator;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.history.RevisionTag;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.exception.FileNotFoundInRepositoryException;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper around a CVMFS Repository revision.
 * A Revision is a concrete instantiation in time of the Repository. It
 * represents the concrete status of the repository in a certain period of
 * time. Revision data is contained in the so-called Tags, which are stored in
 * the History database.
 *
 * @author Jose Molina Colmenero
 */
public class Revision {

    private Repository repository;
    private RevisionTag tag;

    public Revision(Repository repository, RevisionTag revisionTag) {
        this.repository = repository;
        this.tag = revisionTag;
    }

    public Repository getRepository() {
        return repository;
    }

    public RevisionTag getTag() {
        return tag;
    }

    public int getRevisionNumber() {
        return tag.getRevision();
    }

    public String getName() {
        return tag.getName();
    }

    public long getTimestamp() {
        return tag.getTimestamp();
    }

    public String getRootHash() {
        return tag.getHash();
    }

    public Catalog retrieveRootCatalog() {
        return retrieveCatalog(getRootHash());
    }

    /**
     * Retrieve and open a catalog that belongs to this revision
     *
     * @param catalogHash hash of the catalog
     * @return the catalog with the given hash
     */
    public Catalog retrieveCatalog(String catalogHash) {
        return repository.retrieveCatalog(catalogHash);
    }

    public CatalogIterator catalogs() {
        return new CatalogIterator(retrieveRootCatalog());
    }

    /**
     * Recursively walk down the Catalogs and find the best fit for a path
     *
     * @param needlePath path where the catalog can be found
     * @return the catalog stored in the given path for this revision, or null
     * if not such a catalog is present
     */
    public Catalog retrieveCatalogForPath(String needlePath) {
        Catalog catalog = retrieveRootCatalog();
        while (true) {
            CatalogReference newNestedReference = catalog.findNestedForPath(needlePath);
            if (newNestedReference == null) {
                break;
            }
            catalog = retrieveCatalog(newNestedReference.getCatalogHash());
        }
        return catalog;
    }

    /**
     * Lookups in all existing catalogs for this path's best fit
     *
     * @param path path to search for
     * @return the DirectoryEntry that corresponds to the given path if
     * it is found in the already loaded catalogs, or None otherwise
     */
    public DirectoryEntry lookup(String path) {
        if (path.equals("/"))
            path = "";
        Catalog bestFit = retrieveCatalogForPath(path);
        return bestFit.findDirectoryEntry(path);
    }


    public File getFile(String path) {
        DirectoryEntry result = lookup(path);
        if (result != null && result.isFile()) {
            try {
                return repository.retrieveObject(result.contentHashString());
            } catch (FileNotFoundInRepositoryException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * List all the entries in a directory
     *
     * @param path path of the directory
     * @return a list of DirectoryEntry representing all the entries for the
     * given directory, or None if such a directory does not exist
     */
    public List<DirectoryEntry> listDirectory(String path) {
        DirectoryEntry dirent = lookup(path);
        if (dirent != null && dirent.isDirectory()) {
            Catalog bestFit = retrieveCatalogForPath(path);
            return bestFit.listDirectory(path);
        }
        return null;
    }


    public Iterator<DirectoryEntryWrapper> iterator() {
        return new RevisionIterator(this);
    }
}
