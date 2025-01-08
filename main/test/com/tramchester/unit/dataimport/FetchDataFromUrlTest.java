package com.tramchester.unit.dataimport;

import com.tramchester.config.DownloadedConfig;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.tfgm.TFGMRemoteDataSourceConfig;
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

import static com.tramchester.dataimport.URLStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class FetchDataFromUrlTest extends EasyMockSupport {

    private HttpDownloadAndModTime httpDownloader;
    private FetchDataFromUrl fetchDataFromUrl;
    private GetsFileModTime getsFileModTime;
    private Path destinationFile;
    private final URI expectedDownloadURL = URI.create(TestEnv.TFGM_TIMETABLE_URL);
    private ProvidesNow providesLocalNow;
    private DownloadedRemotedDataRepository downloadedDataRepository;
    private ZonedDateTime startTime;
    private ZonedDateTime expiredFileTime;
    private List<Pair<String, String>> headers;
    private HeaderForDatasourceFactory headerFactory;

    @BeforeEach
    void beforeEachTestRuns() {

        TramchesterConfig config = new LocalTestConfig(Path.of("dataFolder"));

        providesLocalNow = createMock(ProvidesNow.class);
        httpDownloader = createMock(HttpDownloadAndModTime.class);
        getsFileModTime = createMock(GetsFileModTime.class);
        S3DownloadAndModTime s3Downloader = createMock(S3DownloadAndModTime.class);
        headerFactory = createMock(HeaderForDatasourceFactory.class);

        DownloadedConfig remoteDataSourceConfig = config.getDataRemoteSourceConfig(DataSourceID.tfgm);

        final String targetZipFilename = remoteDataSourceConfig.getDownloadFilename();
        Path path = remoteDataSourceConfig.getDownloadPath();

        destinationFile = path.resolve(targetZipFilename);

        downloadedDataRepository = new DownloadedRemotedDataRepository();
        fetchDataFromUrl = new FetchDataFromUrl(httpDownloader, s3Downloader, config, providesLocalNow, downloadedDataRepository,
                getsFileModTime, headerFactory);

        startTime = TestEnv.UTCNow();
        expiredFileTime = startTime.minus(remoteDataSourceConfig.getDefaultExpiry()).minusDays(1);

        headers = new ArrayList<>();
        headers.add(Pair.of("Header", "HeaderValue"));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingIfExpiryTimePast() throws IOException, InterruptedException {
        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(expiredFileTime);

        URLStatus status = new URLStatus(expectedDownloadURL, OK);
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, expiredFileTime, true, headers)).andReturn(status);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime, headers)).andReturn(status);
        EasyMock.expectLastCall();

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldFetchIfModTimeIsNewer() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(startTime);

        URLStatus status = new URLStatus(expectedDownloadURL, OK, startTime.plusMinutes(30));
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, startTime, true, headers)).andReturn(status);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, startTime, headers)).andReturn(status);
        EasyMock.expect(getsFileModTime.update(destinationFile, startTime.plusMinutes(30))).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldFetchIfLocalFileNotPresent() throws IOException, InterruptedException {

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime);
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, invalidTime, true, headers)).andReturn(status);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, invalidTime, headers)).andReturn(status);
        EasyMock.expect(getsFileModTime.update(destinationFile, startTime)).andReturn(true);


        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldNotFetchIfModTimeIsNotNewer() throws IOException, InterruptedException {

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        URLStatus status = new URLStatus(expectedDownloadURL, 200, startTime.minusDays(1));
        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, startTime, true, headers)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldCopeWithRedirectsPermAndTemp() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        ZonedDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(modTime);

        URI redirectUrl1 = URI.create("https://resource.is.always.now.com/resource");
        URI redirectUrl2 = URI.create("https://resource.is.temp.now.com/resource");

        ZonedDateTime time = TestEnv.UTCNow().plusMinutes(1);

        URLStatus status1 = new URLStatus(redirectUrl1, MOVED_PERMANENTLY);
        URLStatus status2 = new URLStatus(redirectUrl2, MOVED_TEMPORARILY);
        URLStatus status3 = new URLStatus(redirectUrl2, OK, time);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime, true, headers)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl1, modTime, true, headers)).andReturn(status2);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl2, modTime, true, headers)).andReturn(status3);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, redirectUrl2, modTime, headers)).andReturn(status3);
        EasyMock.expect(getsFileModTime.update(destinationFile, time)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldCopeWithRedirectsOnDownloadPermAndTemp() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        ZonedDateTime fileModTime = expiredFileTime;

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(fileModTime);

        URI fromConfigURL = URI.create("https://resource.confifured.com/resource");
        URI redirectedURL = URI.create("https://resource.is.temp.moved.com/resource");

        ZonedDateTime remoteModTime = startTime.plusMinutes(2);

        // sometimes see 200 from a server for HEAD but then a redirect for the GET (!)
        URLStatus initialHeadStatus = new URLStatus(fromConfigURL, 200);
        URLStatus firstGetStatus = new URLStatus(redirectedURL, MOVED_TEMPORARILY);
        URLStatus secondHeadStatus = new URLStatus(redirectedURL, 200, remoteModTime);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, fileModTime, true, headers)).andReturn(initialHeadStatus);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, fromConfigURL, fileModTime, headers)).andReturn(firstGetStatus);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, redirectedURL, fileModTime, headers)).andReturn(secondHeadStatus);

        EasyMock.expect(getsFileModTime.update(destinationFile, remoteModTime)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();

        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldHandleTooManyDirectsByThrowing() throws IOException, InterruptedException {
        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        ZonedDateTime fileModTime = expiredFileTime;

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(fileModTime);

        URI redirectedURL = URI.create("https://resource.is.temp.moved.com/resource");

        // sometimes see 200 from a server for HEAD but then a redirect for the GET (!)
        URLStatus redirect = new URLStatus(redirectedURL, MOVED_TEMPORARILY);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, fileModTime, true, headers)).andReturn(redirect);
        EasyMock.expect(httpDownloader.getStatusFor(redirectedURL, fileModTime, true, headers)).andStubReturn(redirect);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();

    }

    @Test
    void shouldCopeWithRedirects307() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        ZonedDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(modTime);

        URI redirectUrl = URI.create("https://resource.is.always.now.com/resource");

        ZonedDateTime time = TestEnv.UTCNow().plusMinutes(1);

        URLStatus status1 = new URLStatus(redirectUrl, TEMPORARY_REDIRECT);
        URLStatus status2 = new URLStatus(redirectUrl, 200, time);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime, true, headers)).andReturn(status1);
        EasyMock.expect(httpDownloader.getStatusFor(redirectUrl, modTime, true, headers)).andReturn(status2);

        EasyMock.expect(httpDownloader.downloadTo(destinationFile, redirectUrl, modTime, headers)).andReturn(status2);
        EasyMock.expect(getsFileModTime.update(destinationFile, time)).andReturn(true);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertEquals(destinationFile, downloadedDataRepository.fileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldHandleNoModTimeIsAvailableByNotDownloadingIfExpiryOK() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        ZonedDateTime modTime = startTime.plusMinutes(1);

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(modTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, modTime, true, headers)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertTrue(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByDownloadingWhenExpired() throws IOException, InterruptedException {

        EasyMock.expect(providesLocalNow.getZoneDateTimeUTC()).andReturn(startTime);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(true);
        EasyMock.expect(getsFileModTime.getFor(destinationFile)).andReturn(expiredFileTime);

        URLStatus status = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, expiredFileTime, true, headers)).andReturn(status);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, expiredFileTime, headers)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertTrue(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByStillTryingDownloadWhenNoLocalFile() throws IOException, InterruptedException {

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        URLStatus statusWithoutValidModTime = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, invalidTime, true, headers)).andReturn(statusWithoutValidModTime);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, invalidTime, headers)).andReturn(statusWithoutValidModTime);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertTrue(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertTrue(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleNoModTimeIsAvailableByStillTryingDownloadWhenNoLocalFileDownloadFails() throws IOException, InterruptedException {

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        URLStatus statusWithoutValidModTime = new URLStatus(expectedDownloadURL, 200);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, invalidTime, true, headers)).andReturn(statusWithoutValidModTime);
        EasyMock.expect(httpDownloader.downloadTo(destinationFile, expectedDownloadURL, invalidTime, headers)).
                andReturn(new URLStatus(expectedDownloadURL, NOT_FOUND));

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.refreshed(DataSourceID.tfgm));
        assertFalse(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));
    }

    @Test
    void shouldHandleRemoteIs404NoLocalFile() throws IOException, InterruptedException {

        EasyMock.expect(getsFileModTime.exists(destinationFile)).andReturn(false);
        EasyMock.expect(headerFactory.getFor(DataSourceID.tfgm)).andReturn(headers);

        URLStatus status = new URLStatus(expectedDownloadURL, NOT_FOUND);

        EasyMock.expect(httpDownloader.getStatusFor(expectedDownloadURL, invalidTime, true, headers)).andReturn(status);

        replayAll();
        Assertions.assertAll(() -> fetchDataFromUrl.fetchData());
        verifyAll();
        assertFalse(downloadedDataRepository.hasFileFor(DataSourceID.tfgm));

    }

    @Test
    void shouldValidateAssumptionAboutSchemeFromURI() {

        URI s3URI = URI.create("s3://bucket/location");
        assertEquals("s3", s3URI.getScheme());

        URI otherUri = URI.create("https://bucket/location");
        assertEquals("https", otherUri.getScheme());

    }


    private static class LocalTestConfig extends TestConfig {
        private final Path downloadPath;

        private LocalTestConfig(Path downloadPath) {
            this.downloadPath = downloadPath;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return Collections.singletonList(TFGMRemoteDataSourceConfig.createFor(downloadPath));
        }
    }
}
