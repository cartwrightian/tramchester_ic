package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.ConfigDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.Version;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.VersionResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class VersionResourceTest {

    private static final IntegrationTramTestConfig configuration = new ResourceTramTestConfig<>(VersionResource.class);

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, configuration);
    private static APIClientFactory factory;

    private final String endPoint = "version";

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @Test
    void shouldGetVersion() {

        Response responce = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, responce.getStatus());

        Version version = responce.readEntity(Version.class);

        String build = System.getenv("BUILD");
        if (build==null) {
            build = "0";
        }
        Assertions.assertEquals(format("2.%s", build), version.getBuildNumber());
    }

    @Test
    void shouldGetTransportModes() {
        Set<TransportMode> expectedModes = configuration.getTransportModes();

        Response responce = APIClient.getApiResponse(factory, endPoint+"/config");
        assertEquals(200, responce.getStatus());

        ConfigDTO results = responce.readEntity(ConfigDTO.class);

        List<TransportMode> result = results.getModes();

        assertEquals(expectedModes.size(), result.size());
        assertTrue(expectedModes.containsAll(result));
        assertFalse(results.getPostcodesEnabled());
        assertEquals(configuration.getMaxNumResults(), results.getNumberJourneysToDisplay());
    }

    @Test
    void shouldGetTransportModesWithBetaFlag() {
        Set<TransportMode> expectedModes = configuration.getTransportModes();

        Response responce = APIClient.getApiResponse(factory, endPoint+"/config?beta=true");
        assertEquals(200, responce.getStatus());

        ConfigDTO results = responce.readEntity(ConfigDTO.class);

        List<TransportMode> result = results.getModes();

        assertEquals(expectedModes.size(), result.size());
        assertTrue(expectedModes.containsAll(result));
        assertFalse(results.getPostcodesEnabled());
        assertEquals(configuration.getMaxNumResults(), results.getNumberJourneysToDisplay());
    }

}
