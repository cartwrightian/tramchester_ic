package com.tramchester.integration.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.DTO.LocationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.JourneyLocationsResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyLocationsResourceTramTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new ResourceTramTestConfig<>(JourneyLocationsResource.class));
    private static APIClientFactory factory;

    private final ObjectMapper mapper = new ObjectMapper();
    private StationRepository stationRepository;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        App app =  appExtension.getApplication();
        stationRepository = app.getDependencies().get(StationRepository.class);
    }

    @Test
    void shouldGetTramStations() {
        Response result = APIClient.getApiResponse(factory, "locations/mode/Tram");

        assertEquals(200, result.getStatus());

        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        long typeCount = results.stream().filter(locationRefDTO -> locationRefDTO.getLocationType() == LocationType.Station).count();

        assertEquals(results.size(), typeCount);

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
        Response resultA = APIClient.getApiResponse(factory, "locations/mode/Tram");
        assertEquals(200, resultA.getStatus());

        Date lastMod = resultA.getLastModified();

        Response resultB = APIClient.getApiResponse(factory, "locations/mode/Tram", lastMod);
        assertEquals(304, resultB.getStatus());
    }

    @Test
    void shouldGetBusStations() {
        Response result = APIClient.getApiResponse(factory, "locations/mode/Bus");

        assertEquals(200, result.getStatus());

        // buses disabled, but should still get a list back, albeit empty
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
        Response result = APIClient.getApiResponse(factory, String.format("locations/near/Tram?lat=%s&lon=%s",
                place.getLat(), place.getLon()));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationList = result.readEntity(new GenericType<>() {});

        assertEquals(5,stationList.size());
        Set<IdForDTO> ids = stationList.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.PiccadillyGardens.getIdForDTO()), ids.toString());
//        assertTrue(ids.contains(TramStations.Piccadilly.getIdForDTO()), ids.toString());

        assertTrue(ids.contains(TramStations.StPetersSquare.getIdForDTO()));
        assertTrue(ids.contains(TramStations.ExchangeSquare.getIdForDTO()));

        assertTrue(ids.contains(TramStations.MarketStreet.getIdForDTO()));
        assertTrue(ids.contains(TramStations.Shudehill.getIdForDTO()));
    }

    @Test
    void shouldGetRecentStationsWithModes() throws JsonProcessingException {
        Cookie cookie = createRecentsCookieFor(TramStations.Altrincham, TramStations.Bury, TramStations.ManAirport);

        // same mode, but tests list parsing
        Response result = APIClient.getApiResponse(factory, "locations/recent?modes=Tram,Tram", List.of(cookie));
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
    void shouldGetRecentWithMixedLocationTypesCookie() {
        String exampleOfIssue = "{\"records\":[" +
                "{\"when\":1704982658951,\"id\":\"9400ZZMAANC\",\"locationType\":\"Station\"}," +
                "{\"when\":1705525137982,\"id\":\"E0057819\",\"locationType\":\"StationGroup\"}," +
                "{\"when\":1704982658951,\"id\":\"9400ZZMAMKT\",\"locationType\":\"Station\"}," +
                "{\"when\":1705525176399,\"id\":\"E0028261\",\"locationType\":\"StationGroup\"}," +
                "{\"when\":1705525176399,\"id\":\"N0075057\",\"locationType\":\"StationGroup\"}]}";

        String encoded = URLEncoder.encode(exampleOfIssue, StandardCharsets.UTF_8);
        Cookie cookie = new Cookie("tramchesterRecent", encoded);

        Response result = APIClient.getApiResponse(factory, "locations/recent?modes=Tram,Tram", List.of(cookie));
        assertEquals(200, result.getStatus());

        List<LocationRefDTO> stationDtos = result.readEntity(new GenericType<>() {});

        assertEquals(2, stationDtos.size());

        Set<IdForDTO> ids = stationDtos.stream().
                map(LocationRefDTO::getId).
                collect(Collectors.toSet());

        assertTrue(ids.contains(TramStations.MarketStreet.getIdForDTO()));
        assertTrue(ids.contains(TramStations.Anchorage.getIdForDTO()));

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

        String recentAsString = RecentJourneys.encodeCookie(mapper, recentJourneys);
        return new Cookie("tramchesterRecent", recentAsString);
    }

}
