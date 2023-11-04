package com.tramchester.integration;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.dataexport.Zipper;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.S3DownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.S3Test;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@S3Test
public class S3DownloaderTest {
    private static ClientForS3 clientForS3;

    private Path temporaryFile;
    private S3DownloadAndModTime downloadAndModTime;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        Zipper zipper = new Zipper();
        clientForS3 = new ClientForS3(new GetsFileModTime(), zipper);
        clientForS3.start();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        clientForS3.stop();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        downloadAndModTime = new S3DownloadAndModTime(clientForS3);

        temporaryFile = Paths.get(FileUtils.getTempDirectoryPath(), "downloadAFile");
        tidyFile();
    }

    @AfterEach
    void afterEachTestRuns() {
        tidyFile();
    }

    private void tidyFile() {
        if (temporaryFile.toFile().exists()) {
            temporaryFile.toFile().delete();
        }
    }

    @Test
    void shouldDownloadSomething() throws IOException {
        URI url = URI.create(TestEnv.getBucketUrl()+"testing/ForTestSupport.txt");

        LocalDateTime localModTime = LocalDateTime.MIN;
        URLStatus result = downloadAndModTime.getStatusFor(url, localModTime, true);
        assertTrue(result.isOk());

        LocalDateTime modTime = result.getModTime();
        assertTrue(modTime.isBefore(TestEnv.LocalNow()));
        assertTrue(modTime.isAfter(LocalDateTime.of(2000,1,1,12,59,22)));

        downloadAndModTime.downloadTo(temporaryFile, url, localModTime);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

    @Test
    void shouldHaveExpectedStatusForMissingKey() {
        URI url = URI.create(TestEnv.getBucketUrl() + "SHOULDBEMISSING");
        LocalDateTime localModTime = LocalDateTime.MIN;
        URLStatus result = downloadAndModTime.getStatusFor(url, localModTime, true);
        assertFalse(result.isOk());

        assertEquals(404,result.getStatusCode());
    }

}
