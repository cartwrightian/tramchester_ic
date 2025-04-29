package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.APIClientFactory;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.resources.RouteResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TestRoute;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RouteResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(RouteResource.class));
    private static APIClientFactory factory;

    private RouteRepository routeRepository;

    @BeforeAll
    public static void onceBeforeAll() {
        factory = new APIClientFactory(appExtension);
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        App app =  appExtension.getApplication();
        routeRepository = app.getDependencies().get(RouteRepository.class);
    }

    @Test
    void shouldGetAllRoutes() {

        TramDate today = TramDate.from(TestEnv.LocalNow());

        List<RouteDTO> routeDTOS = getRouteResponse(); // uses current date server side
        routeDTOS.forEach(route -> assertFalse(route.getStations().isEmpty(), "Route no stations "+route.getRouteName()));

        Set<IdForDTO> namesFromDTO = routeDTOS.stream().map(RouteRefDTO::getId).collect(Collectors.toSet());
        Set<IdForDTO> expectedNames = KnownTramRoute.getFor(today).stream().
                map(TestRoute::dtoId).
                collect(Collectors.toSet());

        Set<IdForDTO> mismatch = SetUtils.disjunction(namesFromDTO, expectedNames);
        assertTrue(mismatch.isEmpty(), mismatch.toString());

    }

    @Test
    void shouldHaveExpectedFirstLastForAirportRoute() {
        IdForDTO manAirportIdForDTO = ManAirport.getIdForDTO();
        IdForDTO finalStationDTOId =  Victoria.getIdForDTO();

        List<RouteDTO> routes = getRouteResponse();

        // has to be today since route ids change over time
        TramDate date = TramDate.of(LocalDate.now());

        IdForDTO expectedRouteId = KnownTramRoute.getNavy(date).dtoId();

        Optional<RouteDTO> airRoutes = routes.stream().
                filter(routeDTO -> routeDTO.getId().equals(expectedRouteId)).
                findFirst();

        assertTrue(airRoutes.isPresent(),"expected to match " + expectedRouteId + " in " + HasId.asIds(routes));

        RouteDTO airRoute = airRoutes.get();
        List<LocationRefWithPosition> stations = airRoute.getStations();

        LocationRefWithPosition first = stations.getFirst();

        LocationRefWithPosition airportDTO = null;
        if (first.getId().equals(manAirportIdForDTO)) {
            airportDTO = first;
            assertEquals(finalStationDTOId, stations.getLast().getId());
        } else if (first.getId().equals(finalStationDTOId)) {
            airportDTO = stations.getLast();
            assertEquals(manAirportIdForDTO, airportDTO.getId());
        } else {
            fail("first and/or last incorrect for airport route " + airRoute);
        }

        TestEnv.assertLatLongEquals(ManAirport.getLatLong(), airportDTO.getLatLong(), 0.00001, "lat long");
        assertTrue(airportDTO.getTransportModes().contains(TransportMode.Tram));

    }

    @Test
    void shouldGetRoutesFilteredByDate() {
        TramDate date = TestEnv.testDay();

        Set<Route> expected = routeRepository.getRoutesRunningOn(date, TramsOnly); // see also known tram route test that checks this number makes known number of routes

        String queryString = String.format("routes/filtered?date=%s", date.format(dateFormatDashes));

        Response result = APIClient.getApiResponse(factory, queryString);
        assertEquals(200, result.getStatus());

        List<RouteDTO> results = result.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        Set<IdForDTO> uniqueRouteIds = results.stream().map(RouteRefDTO::getId).collect(Collectors.toSet());

        assertEquals(expected.size(), uniqueRouteIds.size());

        assertEquals(expected.size(), results.size());

    }

    private List<RouteDTO> getRouteResponse() {
        Response result = APIClient.getApiResponse(factory, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
