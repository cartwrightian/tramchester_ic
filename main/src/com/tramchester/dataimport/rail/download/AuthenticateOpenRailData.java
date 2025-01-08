package com.tramchester.dataimport.rail.download;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.OpenRailDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DoesPostRequest;
import com.tramchester.domain.DataSourceID;
import jakarta.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@LazySingleton
public class AuthenticateOpenRailData {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticateOpenRailData.class);

    private final boolean enabled;
    private final TramchesterConfig config;
    private final DoesPostRequest doesPostRequest;
    private final ObjectMapper mapper;

    private OpenRailDataConfig openRailDataConfig;

    @Inject
    public AuthenticateOpenRailData(TramchesterConfig config, DoesPostRequest doesPostRequest) {
        this.config = config;
        this.doesPostRequest = doesPostRequest;
        this.mapper = new ObjectMapper();

        enabled = config.hasRemoteDataSourceConfig(DataSourceID.openRailData);
    }

    @PostConstruct
    public void start() {
        if (enabled) {
            openRailDataConfig = config.getOpenRailDataConfig();
            if (openRailDataConfig==null) {
                logger.error("Missing config for OpenRailData");
            } else {
                logger.info("Started");
            }
        }
    }

    public String getToken() {
        final String username = openRailDataConfig.getUsername();
        final String password = openRailDataConfig.getPassword();
        final String url = openRailDataConfig.getAuthURL();

        final URI uri = URI.createURI(url);

        final String payload = String.format("username=%s&password=%s", username, password);
        final String body = URLEncoder.encode(payload, StandardCharsets.US_ASCII);

        String jsonResponse = doesPostRequest.post(uri, body);

        try {
            JsonParser parser = mapper.getFactory().createParser(jsonResponse);
            return "";
        } catch (IOException e) {
            logger.error("Unable to parse response (not logging, might contain auth token)", e);
            return "";
        }

    }
}
