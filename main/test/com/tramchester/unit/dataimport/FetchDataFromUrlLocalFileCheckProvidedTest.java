package com.tramchester.unit.dataimport;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.config.DatabaseRemoteDataSourceConfig;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FetchDataFromUrlLocalFileCheckProvidedTest extends EasyMockSupport {

    private final DataSourceID dataSourceID = DataSourceID.database;
    private final URI expectedDownloadURL = URI.create(TestEnv.getDatabaseRemoteURL());

    private FetchDataFromUrl fetchDataFromUrl;
    private GetsFileModTime getsFileModTime;
    private Path destinationFile;
    private ProvidesNow providesLocalNow;
    private DownloadedRemotedDataRepository downloadedDataRepository;
    private ZonedDateTime startTime;
    private ZonedDateTime expiredFileTime;
    private RemoteDataSourceConfig remoteDataSourceConfig;
    private S3DownloadAndModTime s3Downloader;
    private Path statusCheckFile;
    private List<Pair<String, String>> expectedHeaders;
    private HeaderForDatasourceFactory headerFactory;
    private List<Pair<String, String>> emptyHeaders;

    @BeforeEach
    void beforeEachTestRuns() {

        TramchesterConfig config = new LocalTestConfig(Path.of("dataFolder"));

        providesLocalNow = createMock(ProvidesNow.class);
        HttpDownloadAndModTime httpDownloader = createMock(HttpDownloadAndModTime.class);
        getsFileModTime = createMock(GetsFileModTime.class);
        s3Downloader = createMock(S3DownloadAndModTime.class);
        headerFactory = createMock(HeaderForDatasourceFactory.class);

        remoteDataSourceConfig = config.getDataRemoteSourceConfig(dataSourceID);

        final String targetZipFilename = remoteDataSourceConfig.getDownloadFilename();
        final Path path = remoteDataSourceConfig.getDownloadPath();

        destinationFile = path.resolve(targetZipFilename);
        statusCheckFile = path.resolve(remoteDataSourceConfig.getModTimeCheckFilename());

        downloadedDataRepository = new DownloadedRemotedDataRepository();
        fetchDataFromUrl = new FetchDataFromUrl(httpDownloader, s3Downloader, config, providesLocalNow, downloadedDataRepository,
                getsFileModTime, headerFactory);

        startTime = TestEnv.UTCNow();
        expiredFileTime = startTime.minus(remoteDataSourceConfig.getDefaultExpiry()).minusDays(1);

        // currently not set for data sources
        expectedHeaders = new ArrayList<>();
        expectedHeaders.add(Pair.of("someHeader", "someValue"));

        // for S3
        emptyHeaders = Collections.emptyList();
    }

    @Test
    void shouldHaveConfigWithLocalFileCheckPresent() {
        assertFalse(remoteDataSourceConfig.getModTimeCheckFilename().isEmpty());
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingIfExpiryTimePast() throws IOException {
        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(statusCheckFile)).andReturn(expiredFileTime);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);
        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, expiredFileTime, false, emptyHeaders)).andReturn(status);
        EasyMock.expect(s3Downloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime, emptyHeaders)).andReturn(status);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(dataSourceID));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(dataSourceID));

    }

    @Test
    void shouldFetchIfModTimeIsNewer() throws IOException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(statusCheckFile)).andReturn(startTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime.plusMinutes(30));
        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, startTime, false, emptyHeaders)).andReturn(status);
        EasyMock.expect(s3Downloader.downloadTo(destinationFile, expectedDownloadURL, startTime, emptyHeaders)).andReturn(status);
        EasyMock.expect(getsFileModTime.update(destinationFile, startTime.plusMinutes(30))).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(dataSourceID));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(dataSourceID));
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException {

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime);
        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, URLStatus.invalidTime, false, emptyHeaders)).andReturn(status);

        EasyMock.expect(s3Downloader.downloadTo(destinationFile, expectedDownloadURL, URLStatus.invalidTime, emptyHeaders)).andReturn(status);
        EasyMock.expect(getsFileModTime.update(destinationFile, startTime)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(dataSourceID));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(dataSourceID));
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() {

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(statusCheckFile)).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime.minusDays(1));
        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, startTime, false, emptyHeaders)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(dataSourceID));
        assertEquals(statusCheckFile, downloadedDataRepository.fileFor(dataSourceID));

    }

    @Test
    void shouldHandleNoModTimeIsAvailableByNotDownloadingIfExpiryOK() {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        ZonedDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(statusCheckFile)).andReturn(modTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, modTime, false, emptyHeaders)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(dataSourceID));
        assertTrue(downloadedDataRepository.hasFileFor(dataSourceID));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingWhenExpired() throws IOException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(statusCheckFile)).andReturn(expiredFileTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, expiredFileTime, false, emptyHeaders)).andReturn(status);
        EasyMock.expect(s3Downloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime, emptyHeaders)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(dataSourceID));
        assertTrue(downloadedDataRepository.hasFileFor(dataSourceID));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByStillTryingDownloadWhenNoLocalFile() throws IOException {

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        URLStatus statusWithoutValidModTime = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, URLStatus.invalidTime, false, emptyHeaders)).andReturn(statusWithoutValidModTime);
        EasyMock.expect(s3Downloader.downloadTo(destinationFile, expectedDownloadURL, URLStatus.invalidTime, emptyHeaders)).andReturn(statusWithoutValidModTime);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(dataSourceID));
        assertTrue(downloadedDataRepository.hasFileFor(dataSourceID));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByStillTryingDownloadWhenNoLocalFileDownloadFails() throws IOException {

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        URLStatus statusWithoutValidModTime = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, URLStatus.invalidTime, false, emptyHeaders)).andReturn(statusWithoutValidModTime);
        EasyMock.expect(s3Downloader.downloadTo(destinationFile, expectedDownloadURL, URLStatus.invalidTime, emptyHeaders)).
                andReturn(new URLStatus(expectedDownloadURL, 404));

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(dataSourceID));
        assertFalse(downloadedDataRepository.hasFileFor(dataSourceID));
    }

    @Test
    void shouldHandleRemoteIs404NoLocalFile() {

        EasyMock.expect(getsFileModTime.exists(statusCheckFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(dataSourceID)).andReturn(expectedHeaders);

        URLStatus status = new URLStatus(expectedDownloadURL, 404);

        EasyMock.expect(s3Downloader.getStatusFor(expectedDownloadURL, URLStatus.invalidTime, false,
                emptyHeaders)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.hasFileFor(dataSourceID));

    }


    private static class LocalTestConfig extends TestConfig {
        private final Path dataPath;

        private LocalTestConfig(Path dataPath) {
            this.dataPath = dataPath;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(new DatabaseRemoteDataSourceConfig(dataPath));
        }
    }
}
