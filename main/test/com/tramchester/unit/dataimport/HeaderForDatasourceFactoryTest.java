package com.tramchester.unit.dataimport;

import com.tramchester.dataimport.HeaderForDatasourceFactory;
import com.tramchester.dataimport.rail.download.AuthenticateOpenRailData;
import com.tramchester.domain.DataSourceID;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeaderForDatasourceFactoryTest extends EasyMockSupport {

    private HeaderForDatasourceFactory headerForDatasourceFactory;
    private AuthenticateOpenRailData authenticateOpenRailData;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        authenticateOpenRailData = createMock(AuthenticateOpenRailData.class);
        headerForDatasourceFactory = new HeaderForDatasourceFactory(authenticateOpenRailData);
    }

    @Test
    void shouldNotHaveHeadersForTFGM() {
        replayAll();
        List<Pair<String, String>> headers = headerForDatasourceFactory.getFor(DataSourceID.tfgm);
        verifyAll();

        assertTrue(headers.isEmpty());
    }

    @Test
    void shouldGetHeadersForOpenRailData() {
        EasyMock.expect(authenticateOpenRailData.getToken()).andReturn("theToken");

        replayAll();
        List<Pair<String, String>> headers = headerForDatasourceFactory.getFor(DataSourceID.openRailData);
        verifyAll();
        assertEquals(1,headers.size());

        Pair<String,String> header = headers.getFirst();

        assertEquals("X-Auth-Token", header.getKey());
        assertEquals("theToken", header.getValue());
    }
}
