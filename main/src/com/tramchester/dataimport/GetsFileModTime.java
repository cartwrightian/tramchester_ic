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

@LazySingleton
public class GetsFileModTime {
    private static final Logger logger = LoggerFactory.getLogger(GetsFileModTime.class);

    public LocalDateTime getFor(Path filePath) {
        long localModMillis = filePath.toFile().lastModified();
        if (localModMillis==0) {
            String msg = "Local mode time 0 for " + filePath + " is suspect";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        LocalDateTime result = LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis / 1000), TramchesterConfig.TimeZoneId);
        logger.info(format("Got milli: %s time %s for %s ", localModMillis, result ,filePath));
        return result;
    }

    public LocalDateTime getFor(DownloadedConfig config) {
        Path downloadPath = config.getDownloadPath();
        return getFor(downloadPath);
    }

    public boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    public boolean update(Path filePath, LocalDateTime modTime) {
        ZonedDateTime zoned = modTime.atZone(TramchesterConfig.TimeZoneId);
        long millis = zoned.toInstant().toEpochMilli();
        logger.info(format("Set mod time for %s to %s millis: %s", filePath, modTime, millis));

        return filePath.toFile().setLastModified(millis);
    }

}
