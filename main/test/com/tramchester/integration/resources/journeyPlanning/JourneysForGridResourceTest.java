package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.BoxWithCostDTO;
import com.tramchester.domain.presentation.DTO.query.GridQueryDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneysForGridResource;
import com.tramchester.testSupport.ParseJSONStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneysForGridResourceTest {
    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneysForGridResource.class));

    private ParseJSONStream<BoxWithCostDTO> parseStream;
    private int maxChanges;
    private int gridSize;
    private int maxDuration;
    private StationLocations stationLocations;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationsRepository;
    private TramDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();

        parseStream = new ParseJSONStream<>(BoxWithCostDTO.class);

        maxChanges = 3;
        gridSize = 2000;
        maxDuration = 40;

        App app =  appExtension.getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();
        stationLocations = dependencies.get(StationLocations.class);
        closedStationsRepository = dependencies.get(ClosedStationsRepository.class);
        stationRepository = dependencies.get(StationRepository.class);
    }

    @Test
    void shouldHaveJourneysForWholeGridChunked() throws IOException {
        LatLong destPos = KnownLocations.nearStPetersSquare.latLong();
        Station destination = TramStations.StPetersSquare.from(stationRepository);

        final int outOfRangeForDuration = 3;

        IdForDTO destinationId = IdForDTO.createFor(destination);
        LocalDate departureDate = when.toLocalDate();
        LocalTime departureTime = LocalTime.of(9,15);
        GridQueryDTO gridQueryDTO = new GridQueryDTO(destination.getLocationType(), destinationId, departureDate,
                departureTime, maxDuration, maxChanges, gridSize);

        Response response = APIClient.postAPIRequest(appExtension, "grid/chunked", gridQueryDTO);

        assertEquals(200, response.getStatus());

        final InputStream inputStream = response.readEntity(InputStream.class);
        final List<BoxWithCostDTO> results = parseStream.receive(response, inputStream);
        assertFalse(results.isEmpty());

        // fe issue repro
        Set<BoxWithCostDTO> missingBL = results.stream().filter(box -> box.getBottomLeft() == null).collect(Collectors.toSet());
        assertTrue(missingBL.isEmpty(), missingBL.toString());

        // fe issue repro
        Set<BoxWithCostDTO> missingTR = results.stream().filter(box -> box.getTopRight() == null).collect(Collectors.toSet());
        assertTrue(missingTR.isEmpty(), missingBL.toString());

        final List<BoxWithCostDTO> containsDest = results.stream().filter(result -> result.getMinutes() == 0).toList();
        assertEquals(1, containsDest.size());
        BoxWithCostDTO boxWithDest = containsDest.get(0);
        assertTrue(boxWithDest.getBottomLeft().getLat() <= destPos.getLat());
        assertTrue(boxWithDest.getBottomLeft().getLon() <= destPos.getLon());
        assertTrue(boxWithDest.getTopRight().getLat() >= destPos.getLat());
        assertTrue(boxWithDest.getTopRight().getLon() >= destPos.getLon());

        List<BoxWithCostDTO> notDest = results.stream().filter(result -> result.getMinutes() > 0).toList();
        notDest.forEach(boundingBoxWithCost -> assertTrue(boundingBoxWithCost.getMinutes()<=maxDuration));

        List<BoxWithCostDTO> noResult = results.stream().filter(result -> result.getMinutes() < 0).toList();
        assertEquals(outOfRangeForDuration, noResult.size());

        Set<BoundingBoxWithStations> expectedBoxes = getExpectedBoxesInSearchGrid(destination);

        assertEquals(expectedBoxes.size() - outOfRangeForDuration, notDest.size(), "Expected " + expectedBoxes + " but got " + notDest);

        Set<BoxWithCostDTO> tookTooLong = results.stream().filter(boxWithCostDTO -> boxWithCostDTO.getMinutes() > maxDuration).collect(Collectors.toSet());
        assertTrue(tookTooLong.isEmpty(), tookTooLong.toString());
    }


    private Set<BoundingBoxWithStations> getExpectedBoxesInSearchGrid(Station destination) {
        return stationLocations.getStationsInGrids(gridSize).
                filter(boxWithStations -> !boxWithStations.getStations().contains(destination)).
                filter(boxWithStations -> anyOpen(boxWithStations.getStations())).collect(Collectors.toSet());
    }

    private boolean anyOpen(LocationSet locations) {
        return locations.stream().anyMatch(station -> !closedStationsRepository.isClosed(station, when));
    }


}
