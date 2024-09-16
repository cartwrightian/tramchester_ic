package com.tramchester.integration.dataimport;

import com.tramchester.config.DownloadedConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class GetsFileModTimeTest extends EasyMockSupport {

    private GetsFileModTime getsFileModTime;
    private ZonedDateTime when;
    private Path tmpFile;

    @BeforeEach
    void onceBeforeEachTest() throws IOException {
        when = TestEnv.UTCNow().truncatedTo(ChronoUnit.SECONDS);

        //ZoneOffset offset = TramchesterConfig.TimeZoneId.getRules().getOffset(when);

        getsFileModTime = new GetsFileModTime();
        tmpFile = Files.createTempFile("testing-", "only");

        // want to create time to be same as when....
        tmpFile.toFile().setLastModified(when.toEpochSecond()*1000);
        //tmpFile.toFile().setLastModified(when.toEpochSecond(offset)*1000);


    }

    @AfterEach
    void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(tmpFile);
    }

    @Test
    void fileExists() throws IOException {
        assertTrue(getsFileModTime.exists(tmpFile));

        Files.deleteIfExists(tmpFile);

        assertFalse(getsFileModTime.exists(tmpFile));
    }

    @Test
    void shouldGetExpectedModTime() {

        long millis = getEpochMilli(when);
        assertTrue(tmpFile.toFile().setLastModified(millis));

        ZonedDateTime result = getsFileModTime.getFor(tmpFile);
        assertEquals(when, result);
    }

    @Test
    void shouldUpdate() {

        ZonedDateTime target = when.plusDays(1);
        long targetMillis = getEpochMilli(target);

        assertNotEquals(targetMillis, tmpFile.toFile().lastModified());

        getsFileModTime.update(tmpFile, target);
        assertEquals(targetMillis, tmpFile.toFile().lastModified());
    }

    @Test
    void shouldGetForConfig() {
        long millis = getEpochMilli(when);
        assertTrue(tmpFile.toFile().setLastModified(millis));

        DownloadedConfig config = createMock(DownloadedConfig.class);

        EasyMock.expect(config.getDownloadPath()).andReturn(tmpFile);

        replayAll();
        ZonedDateTime result = getsFileModTime.getFor(config);
        verifyAll();

        assertEquals(when, result);
    }

    @Test void shouldGetForPath() {
        ZonedDateTime result = getsFileModTime.getFor(tmpFile);
        assertEquals(when, result);
    }

    private long getEpochMilli(ZonedDateTime dateTime) {
        return dateTime.toInstant().toEpochMilli();
//        ZonedDateTime zonedDateTime = dateTime.atZone(TramchesterConfig.TimeZoneId);
//        return zonedDateTime.toInstant().toEpochMilli();
    }

}
