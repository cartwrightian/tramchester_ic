package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.lang.String.format;

@LazySingleton
public class LiveDataHTTPFetcher extends LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHTTPFetcher.class);
    public static final int CONNECT_TIMEOUT_MS = 30 * 1000;

    private final TfgmTramLiveDataConfig config;

    @Inject
    public LiveDataHTTPFetcher(TramchesterConfig config) {
        this.config = config.getLiveDataConfig();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting for " + config);
    }

    // NOTE For testing - wire up for periodic fetching is the Main, it will not happen on it's own
    @Override
    public String getData() {

        final HttpClient.Builder builder = HttpClient.newBuilder().
                connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS));

        final String configLiveDataUrl = config.getDataUrl();
        String liveDataSubscriptionKey = config.getDataSubscriptionKey();
        if (liveDataSubscriptionKey == null) {
            liveDataSubscriptionKey = "";
        }

        final URI uri = URI.create(configLiveDataUrl);
        final HttpRequest get = HttpRequest.newBuilder().
                uri(uri).
                setHeader("Ocp-Apim-Subscription-Key", liveDataSubscriptionKey).
                GET().build();

        try (final HttpClient httpClient = builder.build()) {
            logger.debug("Get live tram data from " + uri);
            final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
            final int statusCode = response.statusCode();
            if (statusCode == 200) {
                logger.info(format("Get from %s response status is %s size is %s", uri, statusCode, response.body().length()));
                return response.body();
            } else {
                logger.error(format("Got unexpected status code %s from uri %s", statusCode, uri));
            }

        } catch (IOException | InterruptedException exception) {
            logger.error("Caught exception while attempt to get live data from " + uri, exception);
        }

        return "";
    }

    @Override
    boolean isEnabled() {
        // TODO Check for DataURL presence?
        return true;
    }

}
