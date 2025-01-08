package com.tramchester.dataimport.rail.download;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.OpenRailDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DoesPostRequest;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@LazySingleton
public class AuthenticateOpenRailData {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticateOpenRailData.class);

    private final boolean enabled;
    private final DoesPostRequest doesPostRequest;
    private final ObjectMapper mapper;
    private final OpenRailDataConfig openRailDataConfig;

    @Inject
    public AuthenticateOpenRailData(TramchesterConfig config, DoesPostRequest doesPostRequest) {
        this.doesPostRequest = doesPostRequest;
        this.mapper = new ObjectMapper();

        openRailDataConfig = config.getOpenRailDataConfig();
        enabled = (openRailDataConfig!=null);

    }

    @PostConstruct
    public void start() {
        if (enabled) {
            if (openRailDataConfig==null) {
                logger.error("Missing config for OpenRailData");
            } else {
                logger.info("Started");
            }
        }
    }

    public String getToken() {
        if (!enabled) {
            logger.error("getToken() invoked but not enabled, config missing?");
            return "";
        }

        final String username = openRailDataConfig.getUsername();
        final String password = openRailDataConfig.getPassword();
        final String url = openRailDataConfig.getAuthURL();

        final URI uri = URI.create(url);

        final String payload = String.format("username=%s&password=%s", username, password);
        final String body = URLEncoder.encode(payload, StandardCharsets.US_ASCII);

        String jsonResponse = doesPostRequest.post(uri, body);

        return parseJsonResponseForAuthToken(jsonResponse, username);

    }

    private @NotNull String parseJsonResponseForAuthToken(final String jsonResponse, final String requestedUsername) {
        logger.info("Parsing json of size " + jsonResponse.length());
        try(JsonParser parser = mapper.getFactory().createParser(jsonResponse)) {
            parser.nextToken();
            String authToken = "";
            while(parser.hasCurrentToken()) {
                JsonToken symbol = parser.currentToken();
                if (symbol.name().equals("FIELD_NAME")) {
                    final String fieldName = parser.getValueAsString();
                    if ("username".equals(fieldName)) {
                        final String receivedUsername = nextTokenValue(parser);
                        if (requestedUsername.equals(receivedUsername)) {
                            logger.info("Got matching username " + receivedUsername);
                        } else {
                            logger.error("Got mismatch on username " + requestedUsername + " in request but received " + receivedUsername);
                        }
                    }
                    if ("token".equals(fieldName)) {
                        final String receivedToken = nextTokenValue(parser);
                        if (receivedToken.isEmpty()) {
                            logger.error("Received empty token");
                        }
                        if (!authToken.isEmpty()) {
                            logger.warn("Received more than one token");
                        }
                        authToken = receivedToken;
                    }
                }
                parser.nextToken();
            }
            if (authToken.isEmpty()) {
                logger.error("Failed to extract token from received JSON " + jsonResponse);
            } else {
                logger.info("Received token of size " + authToken.length());
            }
            return authToken;
        } catch (IOException e) {
            logger.error("Unable to parse response (not logging, might contain auth token)", e);
            return "";
        }
    }

    private String nextTokenValue(JsonParser parser) throws IOException {
        final JsonToken token = parser.nextToken();
        if ("VALUE_STRING".equals(token.name())) {
            return parser.getValueAsString();
        }
        else {
            logger.error("Unexpected token " + token + " at " + parser.currentLocation());
            return "";
        }
    }
}
