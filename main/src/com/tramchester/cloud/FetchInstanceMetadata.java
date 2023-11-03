package com.tramchester.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.lang.String.format;

@LazySingleton
public class FetchInstanceMetadata implements FetchMetadata {
    private static final Logger logger = LoggerFactory.getLogger(FetchInstanceMetadata.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(4);
    private static final java.lang.String USER_DATA_PATH = "/latest/user-data";
    private final TramchesterConfig config;

    @Inject
    public FetchInstanceMetadata(TramchesterConfig tramchesterConfig) {
        this.config = tramchesterConfig;
    }

    public String getUserData() {
        String urlFromConfig = config.getInstanceDataUrl();
        if (urlFromConfig.isEmpty()) {
            logger.warn("No url for instance meta data, returning empty");
            return "";
        }

        try {
            URL baseUrl = new URL(urlFromConfig);
            URL url = new URL(baseUrl, USER_DATA_PATH);
            return getDataFrom(url);
        } catch (MalformedURLException e) {
            logger.warn(format("Unable to fetch instance metadata from %s and %s", urlFromConfig, USER_DATA_PATH),e);
            return "";
        }
    }

    private String getDataFrom(URL url) {
        String host = url.getHost();
        int port = (url.getPort()==-1) ? 80 : url.getPort();

        logger.info(format("Attempt to getPlatformById instance user data from host:%s port%s url:%s",
                host, port, url));

        HttpClient httpClient = HttpClient.newBuilder().
                connectTimeout(TIMEOUT).
                build();

        try {
            HttpRequest getRequest = HttpRequest.newBuilder().
                    uri(url.toURI()).GET().
                    build();

            HttpResponse<byte[]> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofByteArray());
            byte[] contents = response.body();
            return new String(contents);

        } catch (URISyntaxException | IOException e) {
            String msg = format("Unable to get data from to %s:%s [/%s]", host, port, host);
            logger.warn(msg, e);
        } catch (InterruptedException e) {
            String msg = format("Interrupted during connection to %s:%s [/%s] failed: Connection refused", host, port, host);
            logger.error(msg, e);
        }
        return "";

    }

}
