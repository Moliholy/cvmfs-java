package com.molina.cvmfs.test;

import com.molina.cvmfs.directoryentry.DirectoryEntry;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.exception.RepositoryNotFoundException;
import com.molina.cvmfs.revision.Revision;
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

    public RepositoryTest() throws IOException {
        File cache = new File(TEST_CACHE_PATH);
        cache.mkdirs();
    }

    @Test
    public void initialization()
            throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException, RepositoryNotFoundException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        Revision rev = repo.getCurrentRevision();
        Assert.assertEquals("boss.cern.ch", repo.getFqrn());
        Assert.assertEquals(0, repo.getOpenedCatalogs().size());
        Assert.assertNotNull(repo.getFetcher());
        Assert.assertNotNull(rev.retrieveRootCatalog());
        Assert.assertTrue(repo.unloadCatalogs());
    }

    @Test
    public void lookup() throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException, RepositoryNotFoundException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        Revision rev = repo.getCurrentRevision();
        DirectoryEntry result = rev.lookup("/");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isDirectory());

        result = rev.lookup("");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isDirectory());

        result = rev.lookup("/.cvmfsdirtab");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isFile());

        result = rev.lookup("/.-.");
        Assert.assertNull(result);
    }

    @Test
    public void list() throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException, RepositoryNotFoundException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        Revision rev = repo.getCurrentRevision();
        List<DirectoryEntry> result = rev.listDirectory("/");
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void cat() throws RootFileException, CacheDirectoryNotFound, FailedToLoadSourceException, IOException, RepositoryNotFoundException {
        repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss", TEST_CACHE_PATH);
        Revision rev = repo.getCurrentRevision();
        List<DirectoryEntry> result = rev.listDirectory("/");
        for (DirectoryEntry dirent : result) {
            Assert.assertNotNull(dirent);
            if (dirent.isFile()) {
                File file = rev.getFile("/" + dirent.getName());
                Assert.assertNotNull(file);
                Assert.assertTrue(file.exists());
                Assert.assertTrue(file.isFile());
            }
        }
    }
}
