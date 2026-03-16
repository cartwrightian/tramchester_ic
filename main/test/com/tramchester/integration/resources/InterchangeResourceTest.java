package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.resources.InterchangeResource;
import com.tramchester.testSupport.TramAppTestExtension;
import com.tramchester.testSupport.testTags.TramApp;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TramAppTestExtension.class)
public class InterchangeResourceTest  {

    @TramApp
    private static IntegrationAppExtension appExtension =
            new IntegrationAppExtension(new ResourceTramTestConfig<>(InterchangeResource.class));
    private static APIClientFactory factory;

    private InterchangeRepository interchangeRepository;

    @BeforeAll
    public static void onceBeforeAll() {
        factory =  appExtension.getApiClientFactory();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        interchangeRepository = app.getDependencies().get(InterchangeRepository.class);
    }

    @AfterAll
    public static void onceAfterAllTestsRun() {
        appExtension.after();
        appExtension = null;
    }

    @Test
    void shouldGetInterchangeIds() {
        Response result = APIClient.getApiResponse(factory, "interchanges/all");

        assertEquals(200, result.getStatus());

        List<IdForDTO> results = result.readEntity(new GenericType<>() {});

        List<IdForDTO> expected = interchangeRepository.getAllInterchanges().stream().
                map(interchangeStation -> IdForDTO.createFor(interchangeStation.getStation())).toList();

        assertEquals(expected.size(), results.size());

        assertTrue(expected.containsAll(results));
    }

}
