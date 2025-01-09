package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.rail.ResourceRailTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.StationResource;
import com.tramchester.testSupport.testTags.TrainTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceRailTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceRailTestConfig<>(StationResource.class, false));
    private static APIClientFactory factory;

    private StationRepository stationRepo;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        stationRepo = app.getDependencies().get(StationRepository.class);
    }

    @Test
    void shouldGetSingleStationWithPlatforms() {
        IdForDTO stationId = RailStationIds.ManchesterPiccadilly.getIdDTO();

        String endPoint = "stations/" + stationId.getActualId();
        Response response = APIClient.getApiResponse(factory, endPoint);
        
        assertEquals(200,response.getStatus());
        LocationDTO result = response.readEntity(LocationDTO.class);

        assertEquals(stationId, result.getId());
        assertEquals("Manchester Piccadilly Rail Station", result.getName());

        List<PlatformDTO> platforms = result.getPlatforms();
        assertEquals(16, platforms.size(), platforms.toString());

        List<String> platformIds = platforms.stream().
                map(PlatformDTO::getId).
                map(IdForDTO::getActualId).
                toList();

        assertTrue(platformIds.contains(stationId.getActualId()+"1"), platformIds.toString());
        assertTrue(platformIds.contains(stationId.getActualId()+"13B"), platformIds.toString());

        List<RouteRefDTO> routeRefDTOS = result.getRoutes();

        assertFalse(routeRefDTOS.isEmpty());

        Station station = stationRepo.getStationById(RailStationIds.ManchesterPiccadilly.getId());
        int stationRoutesNumber = station.getPickupRoutes().size() + station.getDropoffRoutes().size();

        assertEquals(routeRefDTOS.size(), stationRoutesNumber);

    }

    @Test
    void shouldGetTrainStations() {
        Response result = APIClient.getApiResponse(factory, "stations/mode/Train");

        assertEquals(200, result.getStatus());

        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});

        Set<IdForDTO> expectedIds = stationRepo.getStations().stream().
                filter(Location::isActive).
                map(IdForDTO::createFor).collect(Collectors.toSet());

        assertEquals(expectedIds.size(), results.size());

        List<IdForDTO> resultIds = results.stream().
                map(LocationRefDTO::getId).
                toList();

        assertTrue(expectedIds.containsAll(resultIds));

        ArrayList<LocationRefDTO> sortedResults = new ArrayList<>(results);
        sortedResults.sort(Comparator.comparing(item -> item.getName().toLowerCase()));

        for (int i = 0; i < sortedResults.size(); i++) {
            assertEquals(results.get(i), sortedResults.get(i), "not sorted");
        }
    }

    @Test
    void shouldGetNearestTrainStations() {

        LatLong place = nearPiccGardens.latLong();
        Response result = APIClient.getApiResponse(factory, String.format("stations/near?lat=%s&lon=%s",
                place.getLat(), place.getLon()));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5, stationList.size(), stationList.toString());
        Set<IdForDTO> ids = stationList.stream().map(LocationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(RailStationIds.ManchesterPiccadilly.getIdDTO()));
        assertTrue(ids.contains(RailStationIds.ManchesterVictoria.getIdDTO()));
        assertTrue(ids.contains(RailStationIds.ManchesterDeansgate.getIdDTO()));
        assertTrue(ids.contains(RailStationIds.SalfordCentral.getIdDTO()));
        assertTrue(ids.contains(RailStationIds.ManchesterOxfordRoad.getIdDTO()));
    }


}
