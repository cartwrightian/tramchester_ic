package com.tramchester.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@LazySingleton
public class SignalToCloudformationReady {
    private static final Logger logger = LoggerFactory.getLogger(SignalToCloudformationReady.class);
    private final URI url;

    @Inject
    public SignalToCloudformationReady(ConfigFromInstanceUserData providesConfig) {
        String text = providesConfig.get("WAITURL");
        if (text!=null) {
            logger.info("Have URL for cloud formation triggered " + text);
        }
        this.url = text==null ? null : URI.create(text);
    }

    public void send() {
        if (url==null) {
            logger.info("Not sending cloud formation triggered as URL is not set");
            return;
        }

        logger.info("Attempt to send PUT to cloud formation to signal code ready " + url);
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().
                uri(url).
                PUT(HttpRequest.BodyPublishers.ofByteArray(createEntity()));

        httpRequestBuilder.setHeader("Content-Type", "");  // aws docs say empty content header is required

        HttpRequest httpRequest = httpRequestBuilder.build();

        try {
            HttpResponse<Void> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                logger.error("Unexpected status for cloud formation triggered " + statusCode);
            } else {
                logger.info("cloud formation POST made OK");
            }
        } catch (IOException | InterruptedException exception) {
            logger.error("Error sending cloud formation triggered to " + url, exception);
        }
    }

    private byte[] createEntity() {
        String content = createContent();
        logger.info("Sending data " + content);
        return content.getBytes();
    }

    private String createContent() {
        UUID uniqueId = UUID.randomUUID();
        String data = "ready to serve traffic";
        return String.format("{\"Status\": \"SUCCESS\", \"Reason\": \"Web Server started\", \"UniqueId\": \"%s\", \"Data\": \"%s\"}",
                uniqueId,
                data);
    }
}
