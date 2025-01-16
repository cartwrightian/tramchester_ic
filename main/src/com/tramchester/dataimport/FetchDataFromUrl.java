package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DownloadedConfig;
import com.tramchester.config.HasRemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class FetchDataFromUrl {
    private static final Logger logger = LoggerFactory.getLogger(FetchDataFromUrl.class);

    private static final int MAX_REDIRECTS = 6;

    private enum RefreshStatus {
        Refreshed,
        NoNeedToRefresh,
        NotExpired,
        UnableToCheck,
        Missing
    }

    private final HttpDownloadAndModTime httpDownloader;
    private final S3DownloadAndModTime s3Downloader;
    private final List<DownloadedConfig> configs;
    private final ProvidesNow providesLocalNow;
    private final DownloadedRemotedDataRepository downloadedDataRepository;
    private final GetsFileModTime getsFileModTime;
    private final HeaderForDatasourceFactory headerFactory;

    @Inject
    public FetchDataFromUrl(HttpDownloadAndModTime httpDownloader, S3DownloadAndModTime s3Downloader,
                            HasRemoteDataSourceConfig config, ProvidesNow providesLocalNow,
                            DownloadedRemotedDataRepository downloadedDataRepository,
                            GetsFileModTime getsFileModTime, HeaderForDatasourceFactory headerFactory) {
        this.httpDownloader = httpDownloader;
        this.s3Downloader = s3Downloader;
        this.configs = new ArrayList<>(config.getRemoteDataSourceConfig());
        this.providesLocalNow = providesLocalNow;

        this.downloadedDataRepository = downloadedDataRepository;
        this.getsFileModTime = getsFileModTime;
        this.headerFactory = headerFactory;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (this.configs==null) {
            throw new RuntimeException("configs was null, use empty list if no sources needed");
        }
        fetchData();
        logger.info("started");
    }

    // force construction via guice to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    public void fetchData() {

        configs.forEach(sourceConfig -> {
            final DataSourceID dataSourceId = sourceConfig.getDataSourceId();
            final String targetFile = sourceConfig.getDownloadFilename();

            if (targetFile.isEmpty()) {
                String msg = format("Missing filename for %s ", dataSourceId);
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            final String prefix = "Source " + dataSourceId + ": ";
            final Path downloadDirectory = sourceConfig.getDownloadPath();
            final Path destination = downloadDirectory.resolve(targetFile);
            final Path statusCheckFile = sourceConfig.hasModCheckFilename() ? downloadDirectory.resolve(sourceConfig.getModTimeCheckFilename()) : destination;
            final DestAndStatusCheckFile destAndStatusCheckFile = new DestAndStatusCheckFile(destination, statusCheckFile);
            logger.info("Checking status for data source " + dataSourceId + " using  " + destAndStatusCheckFile);

            RefreshStatus refreshStatus;
            try {
                refreshStatus = refreshDataIfNewerAvailable(sourceConfig, destAndStatusCheckFile);
            }
            catch (IOException | InterruptedException exception) {
                logger.warn(prefix + "Unable to check status or refresh data for config: " + sourceConfig, exception);
                refreshStatus = RefreshStatus.UnableToCheck;
            }

            logger.info(format("%s Refresh status %s", prefix, refreshStatus));
            switch (refreshStatus) {
                case Refreshed -> {
                    downloadedDataRepository.addFileFor(dataSourceId, destAndStatusCheckFile.destination);
                    downloadedDataRepository.markRefreshed(dataSourceId);
                }
                case NoNeedToRefresh, NotExpired, UnableToCheck ->
                        downloadedDataRepository.addFileFor(dataSourceId, destAndStatusCheckFile.statusCheckFile);
                case Missing -> logger.error("Unable to derive status for " + dataSourceId);
            }

        });
    }

    private RefreshStatus refreshDataIfNewerAvailable(DownloadedConfig sourceConfig, DestAndStatusCheckFile destAndStatusCheckFile) throws IOException, InterruptedException {
        final DataSourceID dataSourceId = sourceConfig.getDataSourceId();

        logger.info("Refresh data if newer is available for " + dataSourceId);

        final boolean filePresent = getsFileModTime.exists(destAndStatusCheckFile.statusCheckFile);

        if (filePresent) {
            logger.info(format("Source %s file %s is present", dataSourceId, destAndStatusCheckFile.statusCheckFile));
            return refreshDataIfNewerAvailableHasFile(sourceConfig, destAndStatusCheckFile);
        } else {
            // not used for S3, but we don't know at this stage if data source is S3 or not
            final List<Pair<String,String>> headers = headerFactory.getFor(dataSourceId);
            logger.info(format("Source %s file %s is NOT present", dataSourceId, destAndStatusCheckFile.statusCheckFile));
            return refreshDataIfNewerAvailableNoFile(sourceConfig, destAndStatusCheckFile, headers);
        }
    }

    private RefreshStatus refreshDataIfNewerAvailableNoFile(DownloadedConfig sourceConfig, DestAndStatusCheckFile destAndStatusCheckFile,
                                                            List<Pair<String, String>> headers) throws IOException, InterruptedException {
        DataSourceID dataSourceId = sourceConfig.getDataSourceId();
        URI originalURL = URI.create(sourceConfig.getDataUrl());

        ZonedDateTime localModTime = URLStatus.invalidTime;

        // download
        boolean isS3 = isS3(originalURL);
        final URLStatus status = getUrlStatus(originalURL, isS3, localModTime, sourceConfig, headers);
        if (status == null) {
            logger.warn(format("No local file %s and unable to check url status", destAndStatusCheckFile));
            return RefreshStatus.Missing;
        }

        final String actualURL = status.getActualURL();
        final Path downloadDirectory = sourceConfig.getDownloadPath();

        logger.info(dataSourceId + ": no local file " + destAndStatusCheckFile.statusCheckFile + " so down loading new data from " + actualURL);
        FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
        if (downloadTo(destAndStatusCheckFile.destination, actualURL, isS3, localModTime, headers).isOk()) {
            return RefreshStatus.Refreshed;
        } else {
            return RefreshStatus.Missing;
        }

    }

    private static boolean isS3(final URI originalURL) {
        return originalURL.getScheme().equalsIgnoreCase("s3");
    }

    private RefreshStatus refreshDataIfNewerAvailableHasFile(DownloadedConfig sourceConfig, DestAndStatusCheckFile destAndStatusCheckFile) throws IOException, InterruptedException {
        // already has the source file locally
        final DataSourceID dataSourceId = sourceConfig.getDataSourceId();

        final ZonedDateTime localMod = getFileModLocalTime(destAndStatusCheckFile.statusCheckFile);
        final ZonedDateTime localNow = providesLocalNow.getZoneDateTimeUTC();

        boolean expired = localMod.plus(sourceConfig.getDefaultExpiry()).isBefore(localNow);
        logger.info(format("%s %s Local mod time: %s Current Local Time: %s ", dataSourceId, destAndStatusCheckFile.statusCheckFile, localMod, localNow));

        // not locally expired, and no url available to check remotely for expiry
        if (sourceConfig.getDataCheckUrl().isBlank() && !expired) {
            logger.info(format("%s file: %s is not expired, skip download", dataSourceId, destAndStatusCheckFile));
            return RefreshStatus.NoNeedToRefresh;
        }

        if (sourceConfig.checkOnlyIfExpired() && !expired) {
            logger.info(format("%s file: %s is not expired, and only checking for updates if expired", dataSourceId, destAndStatusCheckFile));
            return RefreshStatus.NoNeedToRefresh;
        }

        logger.info("Check remote status for originalURL:'"+sourceConfig.getDataUrl()+"'");
        final URI originalURL = URI.create(sourceConfig.getDataUrl());
        final boolean isS3 = isS3(originalURL);

        final List<Pair<String, String>> headers;
        if (isS3) {
            headers = Collections.emptyList();
        } else {
            headers = headerFactory.getFor(dataSourceId);
        }

        final URLStatus status = getUrlStatus(originalURL, isS3, localMod, sourceConfig, headers);
        if (status == null) {
            return RefreshStatus.UnableToCheck;
        }
        final String actualURL = status.getActualURL();

        final ZonedDateTime serverMod = status.getModTime();
        // No Server mod time is available
        if (serverMod.isEqual(URLStatus.invalidTime)) {
            logger.warn(format("%s: Unable to get mod time from server for %s", dataSourceId, actualURL));
            if (expired) {
                final boolean downloaded = attemptDownload(actualURL, destAndStatusCheckFile.destination, isS3, localMod, headers);
                if (downloaded) {
                    return RefreshStatus.Refreshed;
                } else {
                    logger.warn(dataSourceId + " Unable to download from " + actualURL);
                    return RefreshStatus.Missing;
                }
            } else {
                return RefreshStatus.NotExpired;
            }
        }

        logger.info(format("%s: Server mod time: %s File mod time: %s ", dataSourceId, serverMod, localMod));

        // Have server mod time
        try {
            if (serverMod.isAfter(localMod)) {
                logger.warn(dataSourceId + ": server time is after local, downloading new data");
                if (downloadTo(destAndStatusCheckFile.destination, actualURL, isS3, localMod, headers).isOk()) {
                    return RefreshStatus.Refreshed;
                } else {
                    return RefreshStatus.Missing;
                }
            }
            logger.info(dataSourceId + ": no newer data");
            return RefreshStatus.NoNeedToRefresh;
        }
        catch (UnknownHostException disconnected) {
            logger.error(dataSourceId + " cannot connect to check or refresh data " + sourceConfig, disconnected);
            return RefreshStatus.UnableToCheck;
        }

    }

    private URLStatus getUrlStatus(final URI originalURL, final boolean isS3, final ZonedDateTime localModTime,
                                   final DownloadedConfig config,
                                   final List<Pair<String, String>> headers) throws IOException, InterruptedException {
        final DataSourceID dataSourceId = config.getDataSourceId();
        final boolean warnIfMissing = config.isMandatory();
        final URLStatus status = getStatusFor(originalURL, isS3, localModTime, warnIfMissing, headers);
        if (!status.isOk()) {
            if (status.getStatusCode() == 405 ) { // METHOD_NOT_ALLOWED
                logger.warn("METHOD_NOT_ALLOWED was unable to query using HEAD for " + dataSourceId);
            } else {
                if (config.isMandatory()) {
                    logger.warn("Could not download for " + dataSourceId + " status was " + status);
                }
                // TODO
                return null;
            }
        } else {
            logger.info(format("Got remote status %s for %s", status, dataSourceId));
        }
        return status;
    }

    private URLStatus downloadTo(Path destination, String url, boolean isS3, ZonedDateTime zonedDateTime,
                                 List<Pair<String, String>> headers) throws IOException, InterruptedException {
        URI uri = URI.create(url);

        URLStatus result;

        if (isS3) {
            if (!headers.isEmpty()) {
                logger.warn("Ignoring headers for S3");
            }
            result = s3Downloader.downloadTo(destination, uri, zonedDateTime, Collections.emptyList());
        } else {
            result = downloadFollowRedirects(destination, uri, zonedDateTime, headers);
        }

        if (result.hasModTime()) {
            ZonedDateTime modTime = result.getModTime();
            if (getsFileModTime.update(destination, modTime)) {
                logger.info(String.format("Updated mod time to %s for %s downloaded from %s", modTime, destination, uri));
            } else {
                logger.warn(String.format("Failed to update mod time to %s for %s downloaded from %s", modTime, destination, uri));
            }
        }

        return result;

    }

    private URLStatus downloadFollowRedirects(Path destination, URI initialURL, ZonedDateTime localModTime, 
                                              final List<Pair<String, String>> headers) throws IOException, InterruptedException {
        RedirectStrategy redirectStrategy = new RedirectStrategy() {
            @Override
            public URLStatus action(URI actualURL) throws IOException {
                return httpDownloader.downloadTo(destination, actualURL, localModTime, headers);
            }
        };

        return redirectStrategy.followRedirects(initialURL);
    }

    private URLStatus getStatusFor(final URI url, final boolean isS3, final ZonedDateTime localModTime,
                                   final boolean warnIfMissing, final List<Pair<String, String>> headers) throws IOException, InterruptedException {
        if (isS3) {
            if (!headers.isEmpty()) {
                logger.warn("Ignoring headers for S3");
            }
            return s3Downloader.getStatusFor(url, localModTime, warnIfMissing, Collections.emptyList());
        }
        return getStatusFollowRedirects(url, localModTime, warnIfMissing, headers);
    }

    private URLStatus getStatusFollowRedirects(final URI url, final ZonedDateTime localModTime, final boolean warnIfMissing,
                                               final List<Pair<String, String>> headers) throws IOException, InterruptedException {
        RedirectStrategy redirectStrategy = new RedirectStrategy() {
            @Override
            public URLStatus action(final URI actualURI) throws InterruptedException, IOException {
                return httpDownloader.getStatusFor(actualURI, localModTime, warnIfMissing, headers);
            }
        };

        return redirectStrategy.followRedirects(url);
    }

    private boolean attemptDownload(String url, Path destination, boolean isS3,
                                    ZonedDateTime currentModTime, List<Pair<String, String>> headers)  {
        try {
            logger.info(destination + " expired downloading from " + url);
            final URLStatus status = downloadTo(destination, url, isS3, currentModTime, headers);
            return status.isOk();
        }
        catch (IOException | InterruptedException e) {
            logger.error("Cannot download from " + url);
            return false;
        }
    }

    private ZonedDateTime getFileModLocalTime(final Path destination) {
        return getsFileModTime.getFor(destination);
    }

    public static class Ready {
        private Ready() {

        }
    }

    private static abstract class RedirectStrategy {
        public abstract URLStatus action(URI uri) throws InterruptedException, IOException;

        /***
         * Prefer version taking URI
         */
        @Deprecated
        public URLStatus followRedirects(String initial) throws IOException, InterruptedException {
            return followRedirects(URI.create(initial));
        }

        private URLStatus followRedirects(URI initialURL) throws InterruptedException, IOException {
            int numRedirects = 0;
            URLStatus status = action(initialURL);
            URI previous = initialURL;

            // TODO will not handle redirect loops other than by throwing after too many
            while (status.isRedirect()) {
                numRedirects ++;
                if (numRedirects > MAX_REDIRECTS) {
                    String msg = format("Too many redirects(%S) for %s", numRedirects, initialURL);
                    logger.error(msg);
                    throw new RemoteException(msg);
                }
                final URI redirectUrl = createRedirectURL(status, previous);
                logger.warn(String.format("Status code %s Following redirect to %s", status.getStatusCode(), redirectUrl));
                previous = redirectUrl;
                status = action(redirectUrl);
            }

            String message = String.format("Status: %s final url: '%s'", status.getStatusCode(), status.getActualURL());

            if (status.isOk()) {
                logger.info(message);
            } else {
                logger.error(message);
            }

            return status;
        }

        private URI createRedirectURL(URLStatus status, URI previous) {
            String responseURL = status.getActualURL();
            URI uri = URI.create(responseURL);
            if (uri.isAbsolute()) {
                return uri;
            }
            URI resolved = previous.resolve(uri);
            logger.info("Relative URL " + uri + " resolved to " + resolved);
            return resolved;
        }
    }

    private record DestAndStatusCheckFile(Path destination, Path statusCheckFile) {

        @Override
            public String toString() {
                return "DestAndStatusCheckFile{" +
                        "destination=" + destination +
                        ", statusCheckFile=" + statusCheckFile +
                        '}';
            }
        }
}
