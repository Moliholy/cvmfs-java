package com.molina.cvmfs.test;

import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.rootfile.exception.RootFileException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryTest {

    private static final String TEST_CACHE_PATH = "/tmp/cvmfs_test_cache";

    private Repository repo;

    @Test
    public void initialization()
            throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        Assert.assertEquals("boss.cern.ch", repo.getFqrn());
        Assert.assertEquals(1, repo.getOpenedCatalogs().size());
        Assert.assertEquals(TEST_CACHE_PATH, repo.getStorageLocation());
        Assert.assertNotNull(repo.getFetcher());
        Assert.assertNotNull(repo.retrieveRootCatalog());
        Assert.assertTrue(repo.unloadCatalogs());
    }

    @Test
    public void retrieveCatalogTree()
            throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        Assert.assertEquals(1, repo.getOpenedCatalogs().size());
        repo.retrieveCatalogTree();
        Assert.assertFalse(repo.getOpenedCatalogs().isEmpty());
        Assert.assertTrue(repo.unloadCatalogs());
    }

    @Test
    public void lookup() throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        DirectoryEntry result = repo.lookup("/");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isDirectory());

        result = repo.lookup("");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isDirectory());

        result = repo.lookup("/.-.");
        Assert.assertNull(result);
    }

    @Test
    public void list() throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        List<DirectoryEntry> result = repo.listDirectory("/");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void cat() throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        List<DirectoryEntry> result = repo.listDirectory("/");
        for (DirectoryEntry dirent : result) {
            if (dirent.isFile()) {
                File file = repo.getFile("/" + dirent.getName());
                Assert.assertNotNull(file);
                Assert.assertTrue(file.exists());
                Assert.assertTrue(file.isFile());
            }
        }
    }
}
