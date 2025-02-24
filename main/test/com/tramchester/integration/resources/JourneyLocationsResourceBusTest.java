package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.CookieSupport;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.bus.ResourceBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.resources.JourneyLocationsResource;
import com.tramchester.testSupport.reference.KnownLocality;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyLocationsResourceBusTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceBusTestConfig<>(JourneyLocationsResource.class));
    private static APIClientFactory factory;

    private final ObjectMapper mapper = new ObjectMapper();
    private StationGroupsRepository stationGroupsRepository;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();
        stationGroupsRepository = dependencies.get(StationGroupsRepository.class);
    }

    @Test
    void shouldGetStationGroupsForBuses() {
        Response result = APIClient.getApiResponse(factory, "locations/mode/Bus");

        assertEquals(200, result.getStatus());

        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        long typeCount = results.stream().filter(locationRefDTO -> locationRefDTO.getLocationType() == LocationType.StationGroup).count();

        assertEquals(results.size(), typeCount);

        Set<IdForDTO> expectedIds = stationGroupsRepository.getAllGroups().stream().
                map(IdForDTO::createFor).
                collect(Collectors.toSet());

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
    void shouldGetTramStation304response() {
        Response resultA = APIClient.getApiResponse(factory, "locations/mode/Bus");
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = APIClient.getApiResponse(factory, "locations/mode/Bus", lastMod);
        assertEquals(304, resultB.getStatus());
    }

    @Test
    void shouldGetTramStations() {
        Response result = APIClient.getApiResponse(factory, "locations/mode/Tram");

        assertEquals(200, result.getStatus());

        // trams disabled, but should still get a list back, albeit empty
        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});
        assertEquals(0, results.size());
    }

    @Test
    void should404ForUnknownMode() {
        Response result = APIClient.getApiResponse(factory, "locations/mode/Jumping");
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldGetNearestStationsWithModeGiven() {

        LatLong place = nearPiccGardens.latLong();
        Response result = APIClient.getApiResponse(factory, String.format("locations/near/Bus?lat=%s&lon=%s",
                place.getLat(), place.getLon()));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> locationList = result.readEntity(new GenericType<>() {});

        assertEquals(2,locationList.size());
        IdSet<StationLocalityGroup> ids = locationList.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                map(StationLocalityGroup::createId).
                collect(IdSet.idCollector());

        assertTrue(ids.contains(KnownLocality.ManchesterCityCentre.getId()), locationList.toString());
        assertTrue(ids.contains(KnownLocality.PiccadillyGardens.getId()), locationList.toString());

    }

    @Test
    void shouldSurfaceCorrectRecentStationsWhenHaveMixInCookie() throws JsonProcessingException {
        Stream<StationLocalityGroup> groups = Stream.of(KnownLocality.Altrincham, KnownLocality.Shudehill,
                KnownLocality.Stockport).map(locality -> locality.from(stationGroupsRepository));

        // fake as tram stations not loaded into repository
        Stream<Station> stations = Stream.of(TramStations.Bury.fake(), TramStations.Victoria.fake());

        Cookie cookie = CookieSupport.createCookieFor(Stream.concat(groups, stations), mapper);

        Response result = APIClient.getApiResponse(factory, "locations/recent?modes=Bus", List.of(cookie));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> locationDTOs = result.readEntity(new GenericType<>() {});

        assertEquals(3, locationDTOs.size());

        IdSet<StationLocalityGroup> ids = locationDTOs.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                map(StationLocalityGroup::createId).
                collect(IdSet.idCollector());

        assertTrue(ids.contains(KnownLocality.Altrincham.getId()));
        assertTrue(ids.contains(KnownLocality.Shudehill.getId()));
        assertTrue(ids.contains(KnownLocality.Stockport.getId()));

    }

    @Test
    void shouldGetRecentStationsWithModes() throws JsonProcessingException {
        // TODO add mix of modes
        Stream<StationLocalityGroup> groupStream = Stream.of(KnownLocality.Altrincham, KnownLocality.Shudehill, KnownLocality.Stockport).map(locality -> locality.from(stationGroupsRepository));
        Cookie cookie = CookieSupport.createCookieFor(groupStream, mapper);

        // same mode, but tests list parsing
        Response result = APIClient.getApiResponse(factory, "locations/recent?modes=Bus", List.of(cookie));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> locationDTOs = result.readEntity(new GenericType<>() {});

        assertEquals(3, locationDTOs.size());

        IdSet<StationLocalityGroup> ids = locationDTOs.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                map(StationLocalityGroup::createId).
                collect(IdSet.idCollector());

        assertTrue(ids.contains(KnownLocality.Altrincham.getId()));
        assertTrue(ids.contains(KnownLocality.Shudehill.getId()));
        assertTrue(ids.contains(KnownLocality.Stockport.getId()));
    }

}
