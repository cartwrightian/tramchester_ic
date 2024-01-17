package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.tram.ResourceTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.resources.RouteResource;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static com.tramchester.testSupport.reference.KnownTramRoute.VictoriaWythenshaweManchesterAirport;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class RouteResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new ResourceTramTestConfig<>(RouteResource.class));

    private RouteRepository routeRepository;

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

        Set<String> namesFromDTO = routeDTOS.stream().map(RouteRefDTO::getRouteName).collect(Collectors.toSet());
        Set<String> expectedNames = KnownTramRoute.getFor(today).stream().map(KnownTramRoute::longName).collect(Collectors.toSet());

        Set<String> namesMismatch = SetUtils.disjunction(namesFromDTO, expectedNames);
        assertTrue(namesMismatch.isEmpty());

    }

    @Test
    void shouldHaveExpectedFirstLastForAirportRoute() {
        IdForDTO manAirportIdForDTO = TramStations.ManAirport.getIdForDTO();
        IdForDTO victoriaIdForDTO = TramStations.Victoria.getIdForDTO();

        List<RouteDTO> routes = getRouteResponse();

        List<RouteDTO> airRoutes = routes.stream().
                filter(routeDTO -> routeDTO.getRouteName().equals(VictoriaWythenshaweManchesterAirport.longName())).
                toList();

        assertEquals(1, airRoutes.size());

        RouteDTO airRoute = airRoutes.get(0);
        List<LocationRefWithPosition> stations = airRoute.getStations();

        LocationRefWithPosition first = stations.get(0);

        LocationRefWithPosition airportDTO = null;
        if (first.getId().equals(manAirportIdForDTO)) {
            airportDTO = first;
            assertEquals(victoriaIdForDTO, stations.get(stations.size()-1).getId());
        } else if (first.getId().equals(victoriaIdForDTO)) {
            airportDTO = stations.get(stations.size()-1);
            assertEquals(manAirportIdForDTO, airportDTO.getId());
        } else {
            fail("first and/or last incorrect for airport route " + airRoute);
        }

        TestEnv.assertLatLongEquals(TramStations.ManAirport.getLatLong(), airportDTO.getLatLong(), 0.00001, "lat long");
        assertTrue(airportDTO.getTransportModes().contains(TransportMode.Tram));

    }

    @Test
    void shouldGetRoutesFilteredByDate() {
        TramDate date = TestEnv.testDay();

        Set<Route> expected = routeRepository.getRoutesRunningOn(date); // see also known tram route test that checks this number makes known number of routes

        String queryString = String.format("routes/filtered?date=%s", date.format(dateFormatDashes));

        Response result = APIClient.getApiResponse(appExtension, queryString);
        assertEquals(200, result.getStatus());

        List<RouteDTO> results = result.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        Set<IdForDTO> uniqueRouteIds = results.stream().map(RouteRefDTO::getRouteID).collect(Collectors.toSet());

        assertEquals(expected.size(), uniqueRouteIds.size());

        assertEquals(expected.size(), results.size());

    }

    private List<RouteDTO> getRouteResponse() {
        Response result = APIClient.getApiResponse(appExtension, "routes");
        assertEquals(200, result.getStatus());
        return result.readEntity(new GenericType<>() {
        });
    }
}
