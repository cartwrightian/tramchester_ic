package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DownloadedConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;

@LazySingleton
public class GetsFileModTime {
    private static final Logger logger = LoggerFactory.getLogger(GetsFileModTime.class);

    /***
     * @param filePath file to get mod time for
     * @return Mod time in UTC
     */
    public ZonedDateTime getFor(final Path filePath) {
        final long localModMillis = filePath.toFile().lastModified(); // millis since epoch
        if (localModMillis==0) {
            String msg = "Local mode time 0 for " + filePath + " is suspect";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        final ZonedDateTime result = ZonedDateTime.ofInstant(Instant.ofEpochSecond(localModMillis / 1000), UTC);
        logger.info(format("Got milli: %s time %s for %s ", localModMillis, result ,filePath));
        return result;
    }

    public ZonedDateTime getFor(final DownloadedConfig config) {
        final Path downloadPath = config.getDownloadPath();
        return getFor(downloadPath);
    }

    public boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    // use version taking ZonedDateTime
    @Deprecated
    public boolean update(Path filePath, LocalDateTime modTime) {
        ZonedDateTime zoned = modTime.atZone(TramchesterConfig.TimeZoneId);
        long millis = zoned.toInstant().toEpochMilli();
        logger.info(format("Set mod time for %s to %s millis: %s", filePath, modTime, millis));

        return filePath.toFile().setLastModified(millis);
    }

    public boolean update(final Path filePath, final ZonedDateTime modTime) {
        long millis = modTime.toInstant().toEpochMilli();
        logger.info(format("Set mod time for %s to %s millis: %s", filePath, modTime, millis));

        return filePath.toFile().setLastModified(millis);
    }

}
