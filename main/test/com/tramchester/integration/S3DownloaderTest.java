package com.tramchester.integration;

import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.dataexport.Zipper;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.S3DownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.S3Test;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@S3Test
public class S3DownloaderTest {
    private static ClientForS3 clientForS3;

    private Path temporaryFile;
    private S3DownloadAndModTime downloadAndModTime;
    private List<Pair<String, String>> headers;

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

        headers = Collections.emptyList();
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

        ZonedDateTime localModTime = URLStatus.invalidTime;
        URLStatus result = downloadAndModTime.getStatusFor(url, localModTime, true, headers);
        assertTrue(result.isOk());

        ZonedDateTime modTime = result.getModTime();
        assertTrue(modTime.isBefore(TestEnv.UTCNow()));
        assertTrue(modTime.isAfter(ZonedDateTime.of(2000,1,1,12,59,22,0,ZoneOffset.UTC)));

        downloadAndModTime.downloadTo(temporaryFile, url, localModTime, headers);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

    @Test
    void shouldHaveExpectedStatusForMissingKey() {
        URI url = URI.create(TestEnv.getBucketUrl() + "SHOULDBEMISSING");
        ZonedDateTime localModTime = URLStatus.invalidTime;
        URLStatus result = downloadAndModTime.getStatusFor(url, localModTime, true, headers);
        assertFalse(result.isOk());

        assertEquals(404,result.getStatusCode());
    }

}
