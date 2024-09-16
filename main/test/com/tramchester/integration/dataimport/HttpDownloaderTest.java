package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.HttpDownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HttpDownloaderTest {

    private Path temporaryFile;
    private HttpDownloadAndModTime urlDownloader;
    private ZonedDateTime localModTime;

    @BeforeEach
    void beforeEachTestRuns() {
        localModTime = URLStatus.invalidTime;
        urlDownloader = new HttpDownloadAndModTime();

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
    void shouldDownloadSomething() throws IOException, InterruptedException {
        URI url = URI.create("https://github.com/fluidicon.png");

        URLStatus headStatus = urlDownloader.getStatusFor(url, localModTime, true);
        assertTrue(headStatus.isOk());
        ZonedDateTime modTime = headStatus.getModTime();
        assertTrue(modTime.isBefore(TestEnv.UTCNow()));
        assertTrue(modTime.isAfter(ZonedDateTime.of(2000,1,1,12,59,22,0, ZoneOffset.UTC)), modTime.toString());

        URLStatus getStatus = urlDownloader.downloadTo(temporaryFile, url, modTime);

        assertTrue(getStatus.isOk(), "unexpected status:" + getStatus);

        // looks like load balancing between servers can cause a few seconds diff here
        //assertEquals(headStatus.getModTime(), getStatus.getModTime());
        long diff = headStatus.getModTime().toEpochSecond() - getStatus.getModTime().toEpochSecond();
        assertTrue(diff < 2000L, "mod time too far out of range" + diff);

        assertTrue(temporaryFile.toFile().exists());
        assertTrue(temporaryFile.toFile().length()>0);
    }

    @Test
    void shouldHaveValidModTimeForTimetableData() throws IOException, InterruptedException {

        URI url = URI.create(TestEnv.TFGM_TIMETABLE_URL);
        URLStatus result = urlDownloader.getStatusFor(url, localModTime, true);

        assertTrue(result.getModTime().getYear()>1970, "Unexpected date " + result.getModTime());
    }

    @Test
    void shouldHave404StatusForMissingUrlHead() throws InterruptedException, IOException {
        URI url = URI.create("http://www.google.com/nothere");

        URLStatus headStatus = urlDownloader.getStatusFor(url, localModTime, true);

        assertFalse(headStatus.isOk());
        assertFalse(headStatus.isRedirect());

        assertEquals(404, headStatus.getStatusCode());
    }

    @Test
    void shouldHave404StatusForMissingDownload() {
        URI url = URI.create("http://www.google.com/nothere");

        ZonedDateTime modTime = URLStatus.invalidTime;
        URLStatus getStatus = urlDownloader.downloadTo(temporaryFile, url, modTime);

        assertFalse(getStatus.isOk());
        assertFalse(getStatus.isRedirect());

        assertEquals(404, getStatus.getStatusCode());

    }

    @Test
    void shouldHaveRedirectStatusAndURL() throws IOException, InterruptedException {
        URI url = URI.create("http://news.bbc.co.uk");

        URLStatus result = urlDownloader.getStatusFor(url, localModTime, true);

        assertFalse(result.isOk());
        assertTrue(result.isRedirect());

        assertEquals("https://www.bbc.co.uk/news", result.getActualURL());

    }
}
