package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasDataPath;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static java.lang.String.format;

@LazySingleton
public class FetchFileModTime {
    private static final Logger logger = LoggerFactory.getLogger(FetchFileModTime.class);

    public LocalDateTime getFor(Path filePath) {
        long localModMillis = filePath.toFile().lastModified();
        LocalDateTime result = LocalDateTime.ofInstant(Instant.ofEpochSecond(localModMillis / 1000), TramchesterConfig.TimeZoneId);
        logger.info(format("Got milli: %s time %s for %s ", localModMillis, result ,filePath));
        return result;
    }

    public LocalDateTime getFor(HasDataPath config) {
        Path dataPath = config.getDataPath();
        return getFor(dataPath);
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
