package com.tramchester.integration.dataimport;

import com.tramchester.config.HasDataPath;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class FetchFileModTimeTest {

    private FetchFileModTime fetchFileModTime;
    private LocalDateTime when;
    private Path tmpFile;

    @BeforeEach
    void onceBeforeEachTest() throws IOException {
        when = TestEnv.LocalNow().truncatedTo(ChronoUnit.SECONDS);

        fetchFileModTime = new FetchFileModTime();
        tmpFile = Files.createTempFile("testing-", "only");

    }

    @AfterEach
    void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(tmpFile);
    }

    @Test
    void fileExists() throws IOException {
        assertTrue(fetchFileModTime.exists(tmpFile));

        Files.deleteIfExists(tmpFile);

        assertFalse(fetchFileModTime.exists(tmpFile));
    }

    @Test
    void shouldGetExpectedModTime() {

        long millis = getEpochMilli(when);
        assertTrue(tmpFile.toFile().setLastModified(millis));

        LocalDateTime result = fetchFileModTime.getFor(tmpFile);
        assertEquals(when, result);
    }

    @Test
    void shouldUpdate() {

        LocalDateTime target = when.plusDays(1);
        long targetMillis = getEpochMilli(target);

        assertNotEquals(targetMillis, tmpFile.toFile().lastModified());

        fetchFileModTime.update(tmpFile, target);
        assertEquals(targetMillis, tmpFile.toFile().lastModified());
    }

    @Test
    void shouldGetForConfig() {
        long millis = getEpochMilli(when);
        assertTrue(tmpFile.toFile().setLastModified(millis));

        fetchFileModTime.getFor(() -> tmpFile);

        LocalDateTime result = fetchFileModTime.getFor(tmpFile);
        assertEquals(when, result);
    }

    private long getEpochMilli(LocalDateTime dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(TramchesterConfig.TimeZoneId);
        return zonedDateTime.toInstant().toEpochMilli();
    }

}
