package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.StationClosureDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.config.StationClosuresConfigForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(DropwizardExtensionsSupport.class)
public class StationResourceClosedStationTest {
    private final static TramDate when = TramDate.of(TestEnv.LocalNow().toLocalDate());

    private static final TramStations closedStation = TramStations.StPetersSquare;

    private final static List<StationClosures> closedStations = Collections.singletonList(
            new StationClosuresConfigForTest(closedStation, new DateRange(when, when.plusWeeks(1)), false));

    // NOTE: planning disabled here
    private static final AppConfiguration config = new IntegrationTramClosedStationsTestConfig(closedStations, false);

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class, config);

    @Test
    void shouldGetClosedStations() {
        APIClientFactory factory = new APIClientFactory(appExtension);

        Response result = APIClient.getApiResponse(factory, "stations/closures");
        assertEquals(200, result.getStatus());

        List<StationClosureDTO> results = result.readEntity(new GenericType<>() {});

        assertEquals(1, results.size());
        StationClosureDTO stationClosure = results.getFirst();
        List<LocationRefDTO> stations = stationClosure.getStations();
        assertEquals(1, stations.size());
        assertEquals(closedStation.getIdForDTO(), stations.getFirst().getId());
        assertEquals(when.toLocalDate(), stationClosure.getBegin());
        assertEquals(when.plusWeeks(1).toLocalDate(), stationClosure.getEnd());
        assertFalse(stationClosure.getFullyClosed());
    }

}
