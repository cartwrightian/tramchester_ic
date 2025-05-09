package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.livedata.domain.DTO.TramPositionDTO;
import com.tramchester.livedata.domain.DTO.TramsPositionsDTO;
import com.tramchester.resources.TramPositionsResource;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import com.tramchester.testSupport.testTags.LiveDataDueTramsTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@RequiresNetwork
@ExtendWith(DropwizardExtensionsSupport.class)
class TramPositionsResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(TramPositionsResource.class, IntegrationTramTestConfig.LiveData.Enabled));
    private static APIClientFactory factory;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @LiveDataDueTramsTest
    @Test
    void shouldGetSomePositionsFilteredByDefault() {
        String endPoint = "positions";
        Response response = factory.clientFor(endPoint).getApiResponse();
        assertEquals(200, response.getStatus());

        TramsPositionsDTO filtered = response.readEntity(TramsPositionsDTO.class);

        //assertFalse(filtered.getBuses());

        // should have some positions
        List<TramPositionDTO> positions = filtered.getPositionsList();
        assertFalse(positions.isEmpty(), "no tram positions available");

        // ALL of those positions should have trams
        long positionsWithTrams = positions.stream().filter(position -> !position.getTrams().isEmpty()).count();
        // MUST be same as total number of positions for filtered
        assertEquals(positions.size(), positionsWithTrams);

        Set<String> uniquePairs = positions.stream().
                map(position -> position.getFirst().getId().getActualId() + position.getSecond().getId().getActualId()).
                collect(Collectors.toSet());

        assertEquals(positions.size(), uniquePairs.size());

        long hasCost = positions.stream().filter(position -> position.getCost()>0).count();
        assertEquals(positions.size(), hasCost);

        long departingTrams = positions.stream().map(TramPositionDTO::getTrams).
                flatMap(Collection::stream).filter(dueTram -> "Departing".equals(dueTram.getStatus())).count();
        assertEquals(0, departingTrams);
    }

    @LiveDataDueTramsTest
    @Test
    void shouldGetSomePositionsUnfiltered() {
        String endPoint = "positions?unfiltered=true";
        Response response = factory.clientFor(endPoint).getApiResponse();
        assertEquals(200, response.getStatus());

        TramsPositionsDTO unfiltered = response.readEntity(TramsPositionsDTO.class);

        // should have some positions
        List<TramPositionDTO> positions = unfiltered.getPositionsList();
        assertFalse(positions.isEmpty());

        long positionsWithTrams = positions.stream().filter(position -> !position.getTrams().isEmpty()).count();
        assertTrue(positionsWithTrams>0, "no positions have trams available");

        // for unfiltered should have more positions than ones with trams
        assertTrue(positions.size() > positionsWithTrams, positions.size() + " not greater than " + positionsWithTrams);

        long departingTrams = positions.stream().map(TramPositionDTO::getTrams).
                flatMap(Collection::stream).filter(dueTram -> "Departing".equals(dueTram.getStatus())).count();
        assertEquals(0, departingTrams);
    }
}
