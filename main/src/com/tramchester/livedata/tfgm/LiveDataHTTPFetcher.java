package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
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

    @Override
    public String getData() {

        HttpClient httpClient = HttpClient.newBuilder().
                connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS)).
                build();

        String configLiveDataUrl = config.getDataUrl();
        String liveDataSubscriptionKey = config.getDataSubscriptionKey();
        if (liveDataSubscriptionKey == null) {
            liveDataSubscriptionKey = "";
        }

        URI uri = URI.create(configLiveDataUrl);
        HttpRequest get = HttpRequest.newBuilder().
                uri(uri).
                setHeader("Ocp-Apim-Subscription-Key", liveDataSubscriptionKey).
                GET().build();

        try {
            logger.debug("Get live tram data from " + uri);
            HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                logger.info(format("Get from %s response status is %s size is %s", uri, statusCode, response.body().length()));
                return response.body();
            } else {
                logger.error(format("Got unexpected status code %s from uri %s", statusCode, uri));
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Caught exception while attempt to get live data from " + uri, e);
        }

        return "";
    }

}
