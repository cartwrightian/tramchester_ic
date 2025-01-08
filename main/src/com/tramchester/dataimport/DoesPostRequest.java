package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@LazySingleton
public class DoesPostRequest {
    private static final Logger logger = LoggerFactory.getLogger(DoesPostRequest.class);

    public String post(URI uri, String body) {
        // careful of logging here, used for authentication requests
        logger.info("Making POST to " + uri + " with body size " + body.length());

        String receivedBody = "";
        try(final HttpClient httpClient = HttpClient.newBuilder().build()) {

            final HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().
                    uri(uri).
                    POST(HttpRequest.BodyPublishers.ofString(body));

            httpRequestBuilder.setHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpRequest httpRequest = httpRequestBuilder.build();

            final HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                logger.error("Unexpected status for post " + statusCode + " url " + uri);
                String diag = response.body();
                if (!diag.isEmpty()) {
                    logger.error("Additional " + diag);
                }
            } else {
                receivedBody = response.body();
                logger.info("POST made OK to " + uri + " received body of size " + receivedBody.length());
            }
        } catch (IOException | InterruptedException exception) {
            logger.error("Error sending cloud formation triggered to " + uri, exception);
        }
        return receivedBody;
    }
}
