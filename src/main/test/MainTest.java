import com.molina.cvmfs.catalog.Catalog;
import com.molina.cvmfs.catalog.CatalogReference;
import com.molina.cvmfs.directoryentry.DirectoryEntryWrapper;
import com.molina.cvmfs.repository.Repository;
import com.molina.cvmfs.repository.exception.CacheDirectoryNotFound;
import com.molina.cvmfs.repository.exception.FailedToLoadSourceException;
import com.molina.cvmfs.repository.exception.NestedCatalogNotFoundException;
import com.molina.cvmfs.rootfile.exception.RootFileException;

import java.io.IOException;

public class MainTest {


    public static void main(String[] args)
            throws RootFileException, CacheDirectoryNotFound,
            FailedToLoadSourceException, IOException, NestedCatalogNotFoundException {
        long start = System.currentTimeMillis();
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
        for (DirectoryEntryWrapper wrapper : repo) {
            System.out.println(wrapper.getPath());
        }
        long total_time = System.currentTimeMillis() - start;
        System.out.println("\n\n\nThe execution took " + total_time + " milliseconds");
    }
}
