package com.tramchester.integration.resources;


import com.tramchester.domain.presentation.DTO.DataVersionDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.DataVersionResource;
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.TramApp;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataUpdateTest
@ExtendWith(TramAppTestExtension.class)
public class DataVersionResourceTest {

    public static final String version = "2026-07-21T10:54:05Z";

    @TramApp
    private static IntegrationAppExtension appExtension = new IntegrationAppExtension(
            new ResourceTramTestConfig<>(DataVersionResource.class));

    private static APIClientFactory factory;

    @BeforeAll
    public static void onceBeforeAll() {
        factory =  appExtension.getApiClientFactory();
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
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
