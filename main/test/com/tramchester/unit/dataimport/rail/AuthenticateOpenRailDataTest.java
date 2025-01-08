package com.tramchester.unit.dataimport.rail;

import com.tramchester.dataimport.rail.download.AuthenticateOpenRailData;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class AuthenticateOpenRailDataTest extends EasyMockSupport {

    private final String url = "https://opendata.nationalrail.co.uk/api/staticfeeds/3.0/timetable";

    private AuthenticateOpenRailData authenticator;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        authenticator = createMock(AuthenticateOpenRailData.class);
    }

    @Disabled("WIP")
    @Test
    void shouldTestSomething() throws InterruptedException {
       fail("WIP");
    }
}
