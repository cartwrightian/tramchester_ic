package com.tramchester.integration.cloud;

import com.tramchester.cloud.FetchInstanceMetadata;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FetchInstanceMetadataTest {

    @Test
    void shouldFetchInstanceMetadata() throws Exception {

        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(new ConfigWithMetaDataUrl("http://localhost:8080"));

        StubbedAWSServer server = new StubbedAWSServer();
        server.run("someSimpleMetaData");

        String data = fetcher.getUserData();
        server.stopServer();

        assertEquals(data, "someSimpleMetaData");
        assertEquals(server.getCalledUrl(), "http://localhost:8080/latest/user-data");
    }

    @Test
    void shouldReturnEmptyIfNoMetaDataAvailable() {
        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(new ConfigWithMetaDataUrl("http://localhost:8080"));

        String result = fetcher.getUserData();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyIfNoUrl() {
        FetchInstanceMetadata fetcher = new FetchInstanceMetadata(new ConfigWithMetaDataUrl(""));

        String result = fetcher.getUserData();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private static class ConfigWithMetaDataUrl extends IntegrationTramTestConfig {

        private final String url;

        private ConfigWithMetaDataUrl(String url) {
            super(LiveData.Disabled, Caching.Disabled);
            this.url = url;
        }

        @Override
        public String getInstanceDataUrl() {
            return url;
        }
    }
}
