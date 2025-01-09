package com.tramchester.integration.resources;


import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.DataVersionDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.DataVersionResource;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataUpdateTest
@ExtendWith(DropwizardExtensionsSupport.class)
public class DataVersionResourceTest {

    public static final String version = "2025-01-09T02:45:47Z";

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(DataVersionResource.class));
    private static APIClientFactory factory;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @Test
    void shouldGetDataVersionCorrectly() {
        String endPoint = "datainfo";

        Response response = APIClient.getApiResponse(factory, endPoint);
        assertEquals(200, response.getStatus());

        DataVersionDTO result = response.readEntity(DataVersionDTO.class);

        assertEquals(version, result.getVersion());
    }

}
