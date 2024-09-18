package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.resources.InterchangeResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DropwizardExtensionsSupport.class)
public class InterchangeResourceTest  {
    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(InterchangeResource.class));
    private static APIClientFactory factory;

    private InterchangeRepository interchangeRepository;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        interchangeRepository = app.getDependencies().get(InterchangeRepository.class);
    }

    @Test
    void shouldGetInterchangeIds() {
        Response result = APIClient.getApiResponse(factory, "interchanges/all");

        assertEquals(200, result.getStatus());

        List<IdForDTO> results = result.readEntity(new GenericType<>() {});

        List<IdForDTO> expected = interchangeRepository.getAllInterchanges().stream().
                map(interchangeStation -> IdForDTO.createFor(interchangeStation.getStation())).collect(Collectors.toList());

        assertEquals(expected.size(), results.size());

        assertTrue(expected.containsAll(results));
    }

}
