package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.StationResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Cookie;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class StationResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(StationResource.class));

    private final ObjectMapper mapper = new ObjectMapper();
    private StationRepository stationRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        stationRepository = app.getDependencies().get(StationRepository.class);
    }

    @Test
    void shouldGetSingleStationWithPlatforms() {
        TramStations stPetersSquare = TramStations.StPetersSquare;

        String stationId = stPetersSquare.getRawId();
        String endPoint = "stations/" + stationId;
        Response response = APIClient.getApiResponse(appExtension, endPoint);
        Assertions.assertEquals(200,response.getStatus());
        LocationDTO result = response.readEntity(LocationDTO.class);

        Assertions.assertEquals(stPetersSquare.getIdForDTO(), result.getId());

        List<PlatformDTO> platforms = result.getPlatforms();

        Assertions.assertEquals(4, platforms.size());
        List<String> platformIds = platforms.stream().
                map(PlatformDTO::getId).
                map(IdForDTO::getActualId).
                toList();

        Assertions.assertTrue(platformIds.contains(stationId+"1"));
        Assertions.assertTrue(platformIds.contains(stationId+"2"));

        Assertions.assertTrue(platformIds.contains(stationId+"3"));

        Assertions.assertTrue(platformIds.contains(stationId+"4"));

        List<RouteRefDTO> routeRefDTOS = result.getRoutes();

        assertFalse(routeRefDTOS.isEmpty());

        Station station = stationRepository.getStationById(stPetersSquare.getId());
        int stationRoutesNumber = station.getPickupRoutes().size() + station.getDropoffRoutes().size();

        assertEquals(routeRefDTOS.size(),stationRoutesNumber);

    }

    @Test
    void shouldGetTramStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Tram");

        assertEquals(200, result.getStatus());

        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});

        Set<IdForDTO> expectedIds = stationRepository.getStationsServing(TransportMode.Tram).stream().
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
        Response resultA = APIClient.getApiResponse(appExtension, "stations/mode/Tram");
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = APIClient.getApiResponse(appExtension, "stations/mode/Tram", lastMod);
        assertEquals(304, resultB.getStatus());
    }

    @Test
    void shouldGetAllStationsWithDetails() {
        Response response = APIClient.getApiResponse(appExtension, "stations/all");
        assertEquals(200, response.getStatus());

        List<LocationDTO> results = response.readEntity(new GenericType<>() {});

        assertEquals(stationRepository.getStationsServing(TransportMode.Tram).size(), results.size());

        Station stPeters = TramStations.StPetersSquare.from(stationRepository);
        IdForDTO expected = new IdForDTO(stPeters.getId());

        Optional<LocationDTO> found = results.stream().
                filter(item -> item.getId().equals(expected)).findFirst();

        assertTrue(found.isPresent());

        LocationDTO result = found.get();

        assertEquals(LocationType.Station, result.getLocationType());
        assertEquals(stPeters.getPlatforms().size(), result.getPlatforms().size());

        // WIP
        //assertTrue(result.getIsInterchange());
    }

    @Test
    void shouldGetBusStations() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        // buses disabled, but should still get a list back, albeit empty
        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});
        assertEquals(0, results.size());
    }

    @Test
    void should404ForUnknownMode() {
        Response result = APIClient.getApiResponse(appExtension, "stations/mode/Jumping");
        assertEquals(404, result.getStatus());
    }

    @Test
    void shouldGetNearestStationsNoModeGiven() {

        LatLong place = nearPiccGardens.latLong();
        Response result = APIClient.getApiResponse(appExtension, String.format("stations/near?lat=%s&lon=%s",
                place.getLat(), place.getLon()));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5,stationList.size());
        Set<IdForDTO> ids = stationList.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.PiccadillyGardens.getIdForDTO()), ids.toString());
        assertTrue(ids.contains(TramStations.StPetersSquare.getIdForDTO()), ids.toString());
        assertTrue(ids.contains(TramStations.ExchangeSquare.getIdForDTO()), ids.toString());

        // summer 2024 closures
        assertTrue(ids.contains(TramStations.Shudehill.getIdForDTO()));
//        assertTrue(ids.contains(TramStations.MarketStreet.getIdForDTO()));
        assertTrue(ids.contains(TramStations.Piccadilly.getIdForDTO()), ids.toString());
//        assertTrue(ids.contains(TramStations.Victoria.getIdForDTO()), ids.toString());
    }

    @Test
    void shouldGetNearestStationsWithModeGiven() {

        LatLong place = nearPiccGardens.latLong();
        Response result = APIClient.getApiResponse(appExtension, String.format("stations/near/Tram?lat=%s&lon=%s",
                place.getLat(), place.getLon()));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5,stationList.size());
        Set<IdForDTO> ids = stationList.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.PiccadillyGardens.getIdForDTO()), ids.toString());
        assertTrue(ids.contains(TramStations.StPetersSquare.getIdForDTO()), ids.toString());
        assertTrue(ids.contains(TramStations.ExchangeSquare.getIdForDTO()), ids.toString());

        // summer 2024 closures
        assertTrue(ids.contains(TramStations.Shudehill.getIdForDTO()));
//        assertTrue(ids.contains(TramStations.MarketStreet.getIdForDTO()));
        assertTrue(ids.contains(TramStations.Piccadilly.getIdForDTO()), ids.toString());
//        assertTrue(ids.contains(TramStations.Victoria.getIdForDTO()), ids.toString());

    }

    @Test
    void shouldGetRecentStationsNoModes() throws JsonProcessingException {
        Cookie cookie = createRecentsCookieFor(TramStations.Altrincham, TramStations.Bury, TramStations.ManAirport);

        // All
        Response result = APIClient.getApiResponse(appExtension, "stations/recent", List.of(cookie));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(3, stationDtos.size());

        Set<IdForDTO> ids = stationDtos.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.Altrincham.getIdForDTO()));
        assertTrue(ids.contains(TramStations.Bury.getIdForDTO()));
        assertTrue(ids.contains(TramStations.ManAirport.getIdForDTO()));
    }

    @Test
    void shouldGetRecentStationsWithModes() throws JsonProcessingException {
        Cookie cookie = createRecentsCookieFor(TramStations.Altrincham, TramStations.Bury, TramStations.ManAirport);

        // same mode, but tests list parsing
        Response result = APIClient.getApiResponse(appExtension, "stations/recent?modes=Tram,Tram", List.of(cookie));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(3, stationDtos.size());

        Set<IdForDTO> ids = stationDtos.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.Altrincham.getIdForDTO()));
        assertTrue(ids.contains(TramStations.Bury.getIdForDTO()));
        assertTrue(ids.contains(TramStations.ManAirport.getIdForDTO()));
    }

    @NotNull
    private Cookie createRecentsCookieFor(TramStations... stations) throws JsonProcessingException {
        RecentJourneys recentJourneys = new RecentJourneys();

        Set<Timestamped> recents = new HashSet<>();
        for (TramStations station : stations) {
            Timestamped timestamped = new Timestamped(station.getId(), TestEnv.LocalNow(), LocationType.Station);
            recents.add(timestamped);
        }
        recentJourneys.setTimestamps(recents);

        String recentAsString = RecentJourneys.encodeCookie(mapper,recentJourneys);
        return new Cookie("tramchesterRecent", recentAsString);
    }

}
