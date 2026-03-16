package com.tramchester.integration.resources;

import com.tramchester.domain.presentation.DTO.ConfigDTO;
import com.tramchester.domain.presentation.Version;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.VersionResource;
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.testTags.TramApp;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TramAppTestExtension.class)
class VersionAndConfigResourceTest {

    private static final IntegrationTramTestConfig configuration = new ResourceTramTestConfig<>(VersionResource.class);

    @TramApp
    private static IntegrationAppExtension appExtension = new IntegrationAppExtension(configuration);
    private static APIClientFactory factory;

    private final String endPoint = "version";

    @BeforeAll
    public static void onceBeforeAll() {
        factory = appExtension.getApiClientFactory();
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
    }

    @Test
    void shouldGetVersion() {

        Response response = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, response.getStatus());

        Version version = response.readEntity(Version.class);

        String build = System.getenv("BUILD");
        if (build==null) {
            build = "0";
        }
        Assertions.assertEquals(format("2.%s", build), version.getBuildNumber());
    }

    @Test
    void shouldGetConfig() {
        Set<TransportMode> expectedModes = configuration.getTransportModes();

        Response response = APIClient.getApiResponse(factory, endPoint+"/config");
        assertEquals(200, response.getStatus());

        ConfigDTO results = response.readEntity(ConfigDTO.class);

        List<TransportMode> result = results.getModes();

        assertEquals(expectedModes.size(), result.size());
        assertTrue(expectedModes.containsAll(result));
        assertFalse(results.getPostcodesEnabled());
        assertEquals(configuration.getMaxNumberResults(), results.getNumberJourneysToDisplay());
        assertEquals(configuration.getMaxNumberChanges(), results.getMaxNumberChanges());
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
        assertEquals(configuration.getMaxNumberResults(), results.getNumberJourneysToDisplay());
    }

}
