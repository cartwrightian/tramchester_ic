package com.tramchester.unit.dataimport.rail;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.OpenRailDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DoesPostRequest;
import com.tramchester.dataimport.rail.download.AuthenticateOpenRailData;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticateOpenRailDataTest extends EasyMockSupport {

    private AuthenticateOpenRailData authenticator;
    private DoesPostRequest doesPostRequest;
    private URI uri;

    @BeforeEach
    void onceBeforeEachTestRuns() {

        doesPostRequest = createMock(DoesPostRequest.class);

        String url = "https://some.fake.url/auth";
        TramchesterConfig config = new ConfigWithOpenRailData(url);
        authenticator = new AuthenticateOpenRailData(config, doesPostRequest);
        uri = URI.createURI(url);

    }

    @Test
    void shouldGetTokenFromJSONResponse() {
        String json = "{\n" +
                "    \"username\":\"alice@example.com\",\n" +
                "    \"roles\":\n" +
                "    {\n" +
                "        \"ROLE_DARWIN\":true,\n" +
                "        \"ROLE_KB_REAL_TIME\":true,\n" +
                "        \"ROLE_STANDARD\":true,\n" +
                "        \"ROLE_KB_API\":true\n" +
                "    },\n" +
                "    \"token\":\"alice@example.com:1702858757000:TOKEN\"\n" +
                "}";

        // note the example on the open rail data website does not look properly URl encoded....
        EasyMock.expect(doesPostRequest.post(uri,"username%3Dalice%40example.com%26password%3Dsecret")).andReturn(json);

        replayAll();
        String token = authenticator.getToken();
        verifyAll();

        assertEquals("alice@example.com:1702858757000:TOKEN", token);
    }

    @Test
    void shouldReturnEmptyTokenIfJsonParseFails() {
        String json = "XXXXX";

        // note the example on the open rail data website does not look properly URl encoded....
        EasyMock.expect(doesPostRequest.post(uri,"username%3Dalice%40example.com%26password%3Dsecret")).andReturn(json);

        replayAll();
        String token = authenticator.getToken();
        verifyAll();

        assertTrue(token.isEmpty());
    }

    @Test
    void shouldReturnEmptyTokenIfResultFromPostIsEmpty() {
        String json = "";

        // note the example on the open rail data website does not look properly URl encoded....
        EasyMock.expect(doesPostRequest.post(uri,"username%3Dalice%40example.com%26password%3Dsecret")).andReturn(json);

        replayAll();
        String token = authenticator.getToken();
        verifyAll();

        assertTrue(token.isEmpty());
    }

    @Test
    void shouldReturnEmptyTokenIfJsonContainsNoToken() {
        String json = "{\n" +
                "    \"username\":\"alice@example.com\",\n" +
                "    \"roles\":\n" +
                "    {\n" +
                "        \"ROLE_DARWIN\":true,\n" +
                "        \"ROLE_KB_REAL_TIME\":true,\n" +
                "        \"ROLE_STANDARD\":true,\n" +
                "        \"ROLE_KB_API\":true\n" +
                "    }\n" +
                "}";

        // note the example on the open rail data website does not look properly URl encoded....
        EasyMock.expect(doesPostRequest.post(uri,"username%3Dalice%40example.com%26password%3Dsecret")).andReturn(json);

        replayAll();
        String token = authenticator.getToken();
        verifyAll();

        assertTrue(token.isEmpty());

    }

    private static class ConfigWithOpenRailData extends TestConfig {

        private final String url;

        public ConfigWithOpenRailData(String url) {
            this.url = url;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }

        @Override
        public OpenRailDataConfig getOpenRailDataConfig() {
            return new OpenRailDataConfig() {
                @Override
                public String getUsername() {
                    return "alice@example.com";
                }

                @Override
                public String getPassword() {
                    return "secret";
                }

                @Override
                public String getAuthURL() {
                    return url;
                }
            };
        }
    }
}
