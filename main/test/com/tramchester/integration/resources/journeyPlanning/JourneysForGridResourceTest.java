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
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneysForGridResource;
import com.tramchester.testSupport.ParseJSONStream;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownLocations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
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

import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneysForGridResourceTest {
    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneysForGridResource.class));
    private static APIClientFactory factory;

    private ParseJSONStream<BoxWithCostDTO> parseStream;
    private int maxChanges;
    private int gridSize;
    private int maxDuration;
    private StationLocations stationLocations;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationsRepository;
    private TramDate when;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();

        parseStream = new ParseJSONStream<>(BoxWithCostDTO.class);

        maxChanges = 3;
        gridSize = 2000;

        App app =  appExtension.getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();
        stationLocations = dependencies.get(StationLocations.class);
        closedStationsRepository = dependencies.get(ClosedStationsRepository.class);
        stationRepository = dependencies.get(StationRepository.class);

        maxDuration = appExtension.getConfiguration().getMaxJourneyDuration();
    }

    @Test
    void shouldHaveJourneysForWholeGridChunked() throws IOException {
        LatLong nearDestination = KnownLocations.nearStPetersSquare.latLong();
        Station destination = StPetersSquare.from(stationRepository);


        IdForDTO destinationId = IdForDTO.createFor(destination);
        LocalDate departureDate = when.toLocalDate();
        LocalTime departureTime = LocalTime.of(9,15);

        GridQueryDTO gridQueryDTO = new GridQueryDTO(destination.getLocationType(), destinationId, departureDate,
                departureTime, maxDuration, maxChanges, gridSize);

        Response response = APIClient.postAPIRequest(factory, "grid/chunked", gridQueryDTO);

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
        BoxWithCostDTO boxWithDest = containsDest.getFirst();
        assertTrue(boxWithDest.getBottomLeft().getLat() <= nearDestination.getLat());
        assertTrue(boxWithDest.getBottomLeft().getLon() <= nearDestination.getLon());
        assertTrue(boxWithDest.getTopRight().getLat() >= nearDestination.getLat());
        assertTrue(boxWithDest.getTopRight().getLon() >= nearDestination.getLon());

        // expects 0 cost/minutes for the box containing the destination
        final List<BoxWithCostDTO> notDest = results.stream().
                filter(result -> result.getMinutes() > 0).toList();

        notDest.forEach(boundingBoxWithCost ->
                assertTrue(boundingBoxWithCost.getMinutes() <= maxDuration,
                        boundingBoxWithCost.getMinutes() + " more than " + maxDuration + " failed for " + boundingBoxWithCost));

        final int outOfRangeForDuration = 0; // todo compute this?

        List<BoxWithCostDTO> noResult = results.stream().filter(result -> result.getMinutes() < 0).toList();
        assertEquals(outOfRangeForDuration, noResult.size());

        Set<BoundingBoxWithStations> withoutDestination = getBoxesNotContaining(destination);

        assertEquals(withoutDestination.size() - outOfRangeForDuration, notDest.size(),
                "expected: " + withoutDestination.size() + " out of range " + outOfRangeForDuration + " not dest " + notDest.size() +
                " on " +when);

        Set<BoxWithCostDTO> tookTooLong = results.stream().filter(boxWithCostDTO -> boxWithCostDTO.getMinutes() > maxDuration).collect(Collectors.toSet());
        assertTrue(tookTooLong.isEmpty(), tookTooLong.toString());
    }

    private Set<BoundingBoxWithStations> getBoxesNotContaining(final Station destination) {
        return stationLocations.getStationsInGrids(gridSize).
                filter(box -> !box.getStations().contains(destination)).
                filter(box -> anyOpen(box.getStations())).collect(Collectors.toSet());
    }

    private boolean anyOpen(LocationSet<Station> locations) {
        return locations.stream().anyMatch(station -> !closedStationsRepository.isClosed(station, when));
    }


}
