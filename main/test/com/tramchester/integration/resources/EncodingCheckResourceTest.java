package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.Utf8Appendable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(DropwizardExtensionsSupport.class)
public class EncodingCheckResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(IntegrationTramTestConfig.LiveData.Disabled, true));
    private static APIClientFactory factory;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
    }

    @Test
    void shouldCheckResponse404GET() {
        String endPoint = new String("noSuch".getBytes(), StandardCharsets.UTF_8);
        Response result = APIClient.getApiResponse(factory, endPoint);
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldCheckResponse404POST() {
        final String endpoint = "noSuch";
        String payload = "";
        Response result = APIClient.postAPIRequest(factory, endpoint, payload, Collections.emptyList());
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldCheckResponseWhenQueryEncodingIssueGET() {
        String endPoint = new String("routes?search_string=%E8.0".getBytes(), StandardCharsets.UTF_8);
        Response result = APIClient.getApiResponse(factory, endPoint);
        assertEquals(400, result.getStatus());
    }

    @Test
    void shouldCheckResponseWhenQueryEncodingIssuePOST() {
        final String endPoint = new String("journey?search_string=%E8.".getBytes(), StandardCharsets.UTF_8);
        String payload = "";
        Response result = APIClient.postAPIRequest(factory, endPoint, payload, Collections.emptyList());
        assertEquals(400, result.getStatus());
    }

    @Test
    void shouldCheckResponseWhenPayloadEncodingIssuePOST() {
        final String payload = new String("{ =%E8.} ".getBytes(), StandardCharsets.UTF_8);
        Response result = APIClient.postAPIRequest(factory, "journey/", payload, Collections.emptyList());
        assertEquals(400, result.getStatus());
    }

    @Test
    void shouldCheckResponseWhenPayloadInvalidPOST() {
        String payload = "XXXXXXXXXXXXXXXXXXXXX";
        Response result = APIClient.postAPIRequest(factory, "journey/", payload, Collections.emptyList());
        assertEquals(400, result.getStatus());
    }

    @Test
    void shouldCheckResponseWhenBadQueryMissingPath() {
        String endPoint = new String("unknown?search_string=%E8.".getBytes(), StandardCharsets.UTF_8);
        Response result = APIClient.getApiResponse(factory, endPoint);
        assertEquals(400, result.getStatus());
    }

    @Test
    void checkBehaviourOfURI() {
        String endPoint = new String("/api/routes?search_string=%E8.0".getBytes(), StandardCharsets.UTF_8);
        URI uri = URI.create(endPoint);
        String query = uri.getRawQuery();
        assertEquals("search_string=%E8.0", query);
    }

    @Test
    void checkBehaviourOfUrlDecoder() {

        MultiMap<String> queryParameters = new MultiMap<>();

        assertThrows(Utf8Appendable.NotUtf8Exception.class,
                () -> UrlEncoded.decodeTo("routes?search_string=%E8.0", queryParameters, UrlEncoded.ENCODING));
    }

}
