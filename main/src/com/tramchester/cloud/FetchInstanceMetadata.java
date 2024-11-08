package com.tramchester.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
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
    private static final String USER_DATA_PATH = "/latest/user-data";
    private static final String USER_DATA_TOKEN_PATH = "/latest/api/token";
    private final TramchesterConfig config;
    private final URI baseURI;

    @Inject
    public FetchInstanceMetadata(TramchesterConfig tramchesterConfig) {
        this.config = tramchesterConfig;
        final String urlFromConfig = config.getInstanceDataUrl();
        if (urlFromConfig.isEmpty()) {
            baseURI = null;
            logger.info("No metadata URL was set in the config");
        } else {
            baseURI = URI.create(urlFromConfig);
            logger.info("metadata base URI is " + baseURI);
        }
    }

    public String getUserData() {
        return getUserData("");
    }

    public String getUserData(String accessToken) {
        if (baseURI==null) {
            logger.info("No url for instance meta data, returning empty");
            return "";
        }

        try {
            final URL url = baseURI.resolve(USER_DATA_PATH).toURL();
            return getDataFrom(url, accessToken);
        } catch (MalformedURLException e) {
            logger.warn(format("Unable to fetch instance metadata from %s and %s", baseURI, USER_DATA_PATH),e);
            return "";
        }
    }

    private String getDataFrom(final URL url, String accessToken) {
        final String host = url.getHost();
        final int port = (url.getPort()==-1) ? 80 : url.getPort();

        String baseMsg = format("Attempt to get instance user data from host:%s port%s url:%s", host, port, url);
        if (accessToken.isEmpty()) {
            logger.info(baseMsg);
        } else {
            logger.info(baseMsg + " with access token set");
        }

        HttpClient httpClient = HttpClient.newBuilder().
                connectTimeout(TIMEOUT).
                build();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(url.toURI()).GET();

            if (!accessToken.isEmpty()) {
                builder.setHeader("X-aws-ec2-metadata-token", accessToken);
            }

            final HttpRequest getRequest = builder.build();

            HttpResponse<byte[]> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofByteArray());
            final int statusCode = response.statusCode();
            if (statusCode == HttpServletResponse.SC_OK) {
                byte[] contents = response.body();
                logger.info("Got metadata ok");
                return new String(contents);
            } else if (statusCode == HttpServletResponse.SC_UNAUTHORIZED) {
                if (accessToken.isEmpty()) {
                    logger.warn("Got " + statusCode + " so retry with access token");
                    return getDataWithAccessToken();
                } else {
                    logger.error("Got " + statusCode + " even with access token set");
                    return "";
                }
            }
            else {
                logger.error("Got status code " + statusCode);
                return "";
            }
        } catch (URISyntaxException e) {
            String msg = format("URISyntaxException, Unable to get data from to %s:%s [%s]", host, port, url);
            logger.warn(msg, e);
        } catch (IOException e) {
            String exceptionMsg = e.getMessage();
            if ("HTTP connect timed out".equals(exceptionMsg)) {
                logger.info(format("Timed out connecting to %s, assume not running in cloud", host));
            } else {
                String msg = format("IOException, Unable to get data from to %s:%s [%s] msg:'%s'", host, port, url, exceptionMsg);
                logger.warn(msg, e);
            }
        } catch (InterruptedException e) {
            String msg = format("Interrupted during connection to %s:%s [%s] failed: Connection refused", host, port, url);
            logger.error(msg, e);
        }
        return "";

    }

    private String getDataWithAccessToken() {
        final URI url;
        url = baseURI.resolve(USER_DATA_TOKEN_PATH);

        logger.info("Get metadata access token from " + url);

        HttpRequest putRequest = HttpRequest.newBuilder().
                uri(url).PUT(HttpRequest.BodyPublishers.noBody()).
                setHeader("X-aws-ec2-metadata-token-ttl-seconds",  "21600").
                build();

        HttpClient httpClient = HttpClient.newBuilder().
                connectTimeout(TIMEOUT).
                build();

        final String token;
        try {
            HttpResponse<byte[]> response = httpClient.send(putRequest, HttpResponse.BodyHandlers.ofByteArray());
            final int statusCode = response.statusCode();
            if (statusCode == HttpServletResponse.SC_OK) {
                byte[] contents = response.body();
                token = new String(contents);
            } else {
                logger.error("Failed to get access token from " + url + " status " + statusCode);
                return "";
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to get token from " + url, e);
            return "";
        }

        logger.info("Successfully got access token from " + url);

        return getUserData(token);

    }

}
