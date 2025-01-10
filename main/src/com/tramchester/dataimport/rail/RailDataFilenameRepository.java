package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.Unzipper;
import com.tramchester.domain.DataSourceID;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@LazySingleton
public class RailDataFilenameRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailDataFilenameRepository.class);

    public static final String PREFIX = "RJTTF";
    public static final String TIMETABLE_FILE_EXT = ".MCA";
    public static final String STATIONS_FILE_EXT = ".MSN";
    public static final String MISSING = "MISSING";

    private final RailConfig railConfig;
    private final RemoteDataAvailable remoteDataRefreshed;
    private final Unzipper unzipper;
    private String version;


    @Inject
    public RailDataFilenameRepository(TramchesterConfig config, RemoteDataAvailable remoteDataRefreshed, FetchDataFromUrl.Ready ready, Unzipper unzipper) {
        railConfig = config.hasRailConfig() ? config.getRailConfig() : null;
        this.remoteDataRefreshed = remoteDataRefreshed;
        this.unzipper = unzipper;
    }

    @PostConstruct
    void start() {
        if (railConfig==null) {
            logger.warn("No rail config");
        } else {
            version = fetchVersionNumberFromZip();
        }
    }

    private String fetchVersionNumberFromZip() {

        final DataSourceID sourceId = railConfig.getDataSourceId();

        if (!remoteDataRefreshed.hasFileFor(sourceId)) {
            logger.error("download for " + sourceId + " is missing");
            return MISSING;
        }

        final Path downloadFilename = remoteDataRefreshed.fileFor(sourceId);

        logger.info("Find rail data version number from " + downloadFilename.toAbsolutePath() + " for " + sourceId);

        final List<Path> contents = unzipper.getContents(downloadFilename);
        final Optional<Path> findTimetablePath = contents.stream().
                filter(path -> path.getFileName().toString().endsWith(TIMETABLE_FILE_EXT)).findFirst();

        if (findTimetablePath.isPresent()) {
            final Path timetablePath = findTimetablePath.get();
            final String filename = timetablePath.getFileName().toString();
            logger.info("Found timetable file " + filename + " from " + timetablePath);
            final String clean = filename.replace(TIMETABLE_FILE_EXT,"").replace(PREFIX, "");
            if (clean.isEmpty()) {
                logger.error("failed to find version from " + timetablePath + " and " + filename);
                return MISSING;
            }
            return clean;
        } else {
            logger.error("Could not find timetable file from extension "+ TIMETABLE_FILE_EXT + " within " + contents);
            return MISSING;
        }
    }

    private void guardAgainstMissingConfig() {
        if (railConfig==null) {
            throw new RuntimeException("Not rail config available");
        }
    }

    public String getCurrentVersion() {
        guardAgainstMissingConfig();

        return version;
    }

    private String getFilename() {
        return PREFIX+getCurrentVersion();
    }

    public Path getTimetable() {
        guardAgainstMissingConfig();

        final Path dataPath = railConfig.getDataPath();
        return dataPath.resolve(getFilename() + TIMETABLE_FILE_EXT);
    }



    public Path getStations() {
        guardAgainstMissingConfig();

        final Path dataPath = railConfig.getDataPath();
        return dataPath.resolve(getFilename() + STATIONS_FILE_EXT);
    }
}
