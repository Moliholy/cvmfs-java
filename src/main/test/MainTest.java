import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.catalog.exception.StopIterationException;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.RepositoryIterator;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.exception.NestedCatalogNotFound;
import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.io.IOException;

public class MainTest {


    public static void main(String[] args)
            throws RootFileException, CacheDirectoryNotFound,
            FailedToLoadSourceException, IOException, NestedCatalogNotFound,
            StopIterationException {
        Repository repo = new Repository("http://cvmfs-stratum-one.cern.ch/opt/boss",
                "/tmp/cache_01");
        System.out.println("Last revision: " +
                repo.getManifest().getRevision() + "    " +
                repo.getManifest().getLastModified());
        Catalog rootCatalog = repo.retrieveRootCatalog();
        System.out.println("Catalog schema: " + rootCatalog.getSchema() + "\n");

        for (CatalogReference nestedCatalogRef : rootCatalog.listNested()) {
            System.out.println("Nested catalog at: " + nestedCatalogRef.getRootPath());
        }

        System.out.println("\nListing repository");
        RepositoryIterator iterator = new RepositoryIterator(repo);
        while (iterator.hasMore()) {
            DirectoryEntryWrapper wrapper = iterator.next();
            System.out.println(wrapper.getPath());
        }
    }
}
