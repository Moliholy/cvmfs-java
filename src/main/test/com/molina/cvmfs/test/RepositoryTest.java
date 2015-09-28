package com.molina.cvmfs.test;

import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.io.IOException;

public class RepositoryTest {

    private static Repository repo;

    public static void main(String[] args)
            throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss");
        Catalog rootCatalog = repo.retrieveRootCatalog();
        System.out.println("Downloading the root catalog");
        retrieveCatalogTree(rootCatalog);
    }

    private static void retrieveCatalogTree(Catalog catalog) {
        CatalogReference[] refs = catalog.listNested();
        for (CatalogReference ref : refs) {
            System.out.println("Downloading the catalog in " + ref.getRootPath());
            Catalog newCatalog = ref.retrieveFrom(repo);
            retrieveCatalogTree(newCatalog);
        }
    }
}
