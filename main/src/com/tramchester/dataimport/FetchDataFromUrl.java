package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasRemoteDataSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.time.ProvidesNow;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.neo4j.server.http.cypher.format.api.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
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

    // TODO Config?
    //public static final long DEFAULT_EXPIRY_MINS = 12 * 60;

    private final HttpDownloadAndModTime httpDownloader;
    private final S3DownloadAndModTime s3Downloader;
    private final List<RemoteDataSourceConfig> configs;
    private final ProvidesNow providesLocalNow;
    private final DownloadedRemotedDataRepository downloadedDataRepository;
    private final FetchFileModTime fetchFileModTime;

    @Inject
    public FetchDataFromUrl(HttpDownloadAndModTime httpDownloader, S3DownloadAndModTime s3Downloader,
                            HasRemoteDataSourceConfig config, ProvidesNow providesLocalNow,
                            DownloadedRemotedDataRepository downloadedDataRepository,
                            FetchFileModTime fetchFileModTime) {
        this.httpDownloader = httpDownloader;
        this.s3Downloader = s3Downloader;
        this.configs = config.getRemoteDataSourceConfig();
        this.providesLocalNow = providesLocalNow;

        this.downloadedDataRepository = downloadedDataRepository;
        this.fetchFileModTime = fetchFileModTime;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (this.configs==null) {
            throw new RuntimeException("configs should be null, use empty list if not sources needed");
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
            String targetFile = sourceConfig.getDownloadFilename();

            if (targetFile.isEmpty()) {
                String msg = format("Missing filename for %s ", dataSourceId);
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            final String prefix = "Source " + dataSourceId + ": ";
            Path downloadDirectory = sourceConfig.getDataPath();
            Path destination = downloadDirectory.resolve(targetFile);
            Path statusCheckFile = sourceConfig.hasModCheckFilename() ? downloadDirectory.resolve(sourceConfig.getModTimeCheckFilename()) : destination;
            DestAndStatusCheckFile destAndStatusCheckFile = new DestAndStatusCheckFile(destination, statusCheckFile);
            try {
                logger.info("Checking status for data source " + dataSourceId + " using  " + destAndStatusCheckFile);
                RefreshStatus refreshStatus = refreshDataIfNewerAvailable(sourceConfig, destAndStatusCheckFile);
                logger.info(format("%s Refresh status %s", prefix, refreshStatus));
                switch (refreshStatus) {
                    case Refreshed -> {
                        downloadedDataRepository.addFileFor(dataSourceId, destAndStatusCheckFile.destination);
                        downloadedDataRepository.markRefreshed(dataSourceId);
                    }
                    case NoNeedToRefresh, NotExpired, UnableToCheck -> downloadedDataRepository.addFileFor(dataSourceId, destAndStatusCheckFile.statusCheckFile);
                    case Missing -> logger.error("Unable to derive status for " + dataSourceId);
                }

            } catch (IOException | InterruptedException exception) {
                logger.warn(prefix + "Unable to refresh data for config: " + sourceConfig, exception);
            } catch (ConnectionException connectionException) {
                logger.error(prefix + "Unable to refresh data for config: " + sourceConfig, connectionException);
            }
        });
    }

    private RefreshStatus refreshDataIfNewerAvailable(RemoteDataSourceConfig sourceConfig, DestAndStatusCheckFile destAndStatusCheckFile) throws IOException, InterruptedException {
        final DataSourceID dataSourceId = sourceConfig.getDataSourceId();

        logger.info("Refresh data if newer is available for " + dataSourceId);

        final boolean filePresent = fetchFileModTime.exists(destAndStatusCheckFile.statusCheckFile);

        if (filePresent) {
            logger.info(format("Source %s file %s is present", dataSourceId, destAndStatusCheckFile.statusCheckFile));
            return refreshDataIfNewerAvailableHasFile(sourceConfig, destAndStatusCheckFile);
        } else {
            logger.info(format("Source %s file %s is NOT present", dataSourceId, destAndStatusCheckFile.statusCheckFile));
            return refreshDataIfNewerAvailableNoFile(sourceConfig, destAndStatusCheckFile);
        }

    }

    private RefreshStatus refreshDataIfNewerAvailableNoFile(RemoteDataSourceConfig sourceConfig, DestAndStatusCheckFile destAndStatusCheckFile) throws IOException, InterruptedException {
        DataSourceID dataSourceId = sourceConfig.getDataSourceId();
        URI originalURL = URI.create(sourceConfig.getDataUrl());
        boolean isS3 = sourceConfig.getIsS3();

        LocalDateTime localModTime = LocalDateTime.MIN;

        // download
        URLStatus status = getUrlStatus(originalURL, isS3, localModTime, dataSourceId);
        if (status == null) {
            logger.warn(format("No local file %s and unable to check url status", destAndStatusCheckFile));
            return RefreshStatus.Missing;
        }

        String actualURL = status.getActualURL();
        Path downloadDirectory = sourceConfig.getDataPath();

        logger.info(dataSourceId + ": no local file " + destAndStatusCheckFile.statusCheckFile + " so down loading new data from " + actualURL);
        FileUtils.forceMkdir(downloadDirectory.toAbsolutePath().toFile());
        if (downloadTo(destAndStatusCheckFile.destination, actualURL, isS3, localModTime).isOk()) {
            return RefreshStatus.Refreshed;
        } else {
            return RefreshStatus.Missing;
        }

    }

    private RefreshStatus refreshDataIfNewerAvailableHasFile(RemoteDataSourceConfig sourceConfig, DestAndStatusCheckFile destAndStatusCheckFile) throws IOException, InterruptedException {
        // already has the source file locally
        DataSourceID dataSourceId = sourceConfig.getDataSourceId();
        boolean isS3 = sourceConfig.getIsS3();

        LocalDateTime localMod = getFileModLocalTime(destAndStatusCheckFile.statusCheckFile);
        LocalDateTime localNow = providesLocalNow.getDateTime();

        boolean expired = localMod.plus(sourceConfig.getDefaultExpiry()).isBefore(localNow);
        logger.info(format("%s %s Local mod time: %s Current Local Time: %s ", dataSourceId, destAndStatusCheckFile.statusCheckFile, localMod, localNow));

        // not locally expired, and no url available to check remotely for expiry
        if (sourceConfig.getDataCheckUrl().isBlank() && !expired) {
            logger.info(format("%s file: %s is not expired, skip download", dataSourceId, destAndStatusCheckFile));
            return RefreshStatus.NoNeedToRefresh;
        }

        logger.info("Check remote status for originalURL:'"+sourceConfig.getDataUrl()+"'");
        URI originalURL = URI.create(sourceConfig.getDataUrl());
        URLStatus status = getUrlStatus(originalURL, isS3, localMod, dataSourceId);
        if (status == null) return RefreshStatus.UnableToCheck;
        String actualURL = status.getActualURL();

        LocalDateTime serverMod = status.getModTime();
        // No Server mod time is available
        if (serverMod.isEqual(LocalDateTime.MIN)) {
            logger.warn(format("%s: Unable to get mod time from server for %s", dataSourceId, actualURL));
            if (expired) {
                boolean downloaded = attemptDownload(actualURL, destAndStatusCheckFile.destination, isS3, localMod);
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
                if (downloadTo(destAndStatusCheckFile.destination, actualURL, isS3, localMod).isOk()) {
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

    private URLStatus getUrlStatus(URI originalURL, boolean isS3, LocalDateTime localModTime, DataSourceID dataSourceId) throws IOException, InterruptedException {
        URLStatus status = getStatusFor(originalURL, isS3, localModTime);
        if (!status.isOk()) {
            if (status.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                logger.warn("SC_METHOD_NOT_ALLOWED was unable to query using HEAD for " + dataSourceId);
            } else {
                logger.warn("Could not download for " + dataSourceId + " status was " + status);
                // TODO
                return null;
            }
        } else {
            logger.info(format("Got remote status %s for %s", status, dataSourceId));
        }
        return status;
    }

    private URLStatus downloadTo(Path destination, String url, boolean isS3,
                            LocalDateTime localModTime) throws IOException, InterruptedException {
        URI uri = URI.create(url);

        if (isS3) {
            return s3Downloader.downloadTo(destination, uri, localModTime);
        } else {
            return downloadFollowRedirects(destination, uri, localModTime);
        }
    }

    private URLStatus downloadFollowRedirects(Path destination, URI initialURL, LocalDateTime localModTime) throws IOException, InterruptedException {
        RedirectStrategy redirectStrategy = new RedirectStrategy() {
            @Override
            public URLStatus action(URI actualURL) {
                return httpDownloader.downloadTo(destination, actualURL, localModTime);
            }
        };

        return redirectStrategy.followRedirects(initialURL);
    }

    private URLStatus getStatusFor(URI url, boolean isS3, LocalDateTime localModTime) throws IOException, InterruptedException {
        if (isS3) {
            return s3Downloader.getStatusFor(url, localModTime);
        }
        return getStatusFollowRedirects(url, localModTime);
    }

    private URLStatus getStatusFollowRedirects(URI url, LocalDateTime localModTime) throws IOException, InterruptedException {
        RedirectStrategy redirectStrategy = new RedirectStrategy() {
            @Override
            public URLStatus action(URI actualURI) throws InterruptedException, IOException {
                return httpDownloader.getStatusFor(actualURI, localModTime);
            }
        };

        return redirectStrategy.followRedirects(url);
    }

    private boolean attemptDownload(String url, Path destination, boolean isS3,
                                    LocalDateTime currentModTime)  {
        try {
            logger.info(destination + " expired downloading from " + url);
            URLStatus status = downloadTo(destination, url, isS3, currentModTime);
            return status.isOk();
        }
        catch (IOException | InterruptedException e) {
            logger.error("Cannot download from " + url);
            return false;
        }
    }

    private LocalDateTime getFileModLocalTime(Path destination) {
        return fetchFileModTime.getFor(destination);
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

    private class DestAndStatusCheckFile {
        private final Path destination;
        private final Path statusCheckFile;

        public DestAndStatusCheckFile(Path destination, Path statusCheckFile) {

            this.destination = destination;
            this.statusCheckFile = statusCheckFile;
        }

        @Override
        public String toString() {
            return "DestAndStatusCheckFile{" +
                    "destination=" + destination +
                    ", statusCheckFile=" + statusCheckFile +
                    '}';
        }
    }
}
