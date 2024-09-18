package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.BoxWithFrequencyDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.resources.FrequencyResource;
import com.tramchester.testSupport.ParseJSONStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.List;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
public class FrequencyResourceTest {

    private static APIClientFactory factory;
    private ParseJSONStream<BoxWithFrequencyDTO> parseStream;

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(FrequencyResource.class));

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    public void beforeEachTest() {
        parseStream = new ParseJSONStream<>(BoxWithFrequencyDTO.class);
    }

    @Test
    void shouldHaveTramFrequencies() throws IOException {

        int gridSizeMeters = 1000;
        TramDate date = TestEnv.testDay();

        LocalTime start= LocalTime.of(7,59);
        LocalTime end = LocalTime.of(9,15);
        String queryString = String.format("frequency?gridSize=%s&date=%s&startTime=%s&endTime=%s",
                gridSizeMeters, date.format(dateFormatDashes),
                start.format(TestEnv.timeFormatter), end.format(TestEnv.timeFormatter));

        Response response = APIClient.getApiResponse(factory, queryString);
        assertEquals(200, response.getStatus());

        InputStream inputStream = response.readEntity(InputStream.class);
        List<BoxWithFrequencyDTO> results = parseStream.receive(response, inputStream);

        assertFalse(results.isEmpty());

        TramStations airportStation = TramStations.ManAirport;
        GridPosition manAirportPosition = CoordinateTransforms.getGridPosition(airportStation.getLatLong());

        List<BoxWithFrequencyDTO> containsAirport = results.stream().
                filter(item -> containsStation(manAirportPosition, item)).toList();
        assertEquals(1, containsAirport.size());
        BoxWithFrequencyDTO airportBox = containsAirport.get(0);

        assertEquals(7, airportBox.getNumberOfStopcalls());
        List<LocationRefDTO> stops = airportBox.getStops();
        boolean airportStopPresent = stops.stream().anyMatch(stop -> stop.getId().equals(airportStation.getIdForDTO()));
        assertTrue(airportStopPresent);
    }

    private boolean containsStation(GridPosition position, BoxWithFrequencyDTO box) {

        BoundingBox boundingBox = new BoundingBox(CoordinateTransforms.getGridPosition(box.getBottomLeft()),
                CoordinateTransforms.getGridPosition(box.getTopRight()));
        return boundingBox.contained(position);
    }

}
