package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteMapperTest {
    private static ComponentContainer componentContainer;
    private TramRouteHelper tramRouteHelper;
    private TramDate date;
    private RoutesMapper mapper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        date = TestEnv.testDay();
        mapper = componentContainer.get(RoutesMapper.class);
        tramRouteHelper = new TramRouteHelper(routeRepository);
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldGetRouteStationsInCorrectOrder() {

        List<RouteDTO> dtosForAirportLine = mapper.getRouteDTOs(TestEnv.testDay()).stream().
                filter(dto -> dto.getRouteName().equals(VictoriaWythenshaweManchesterAirport.longName())).
                toList();

        List<RouteDTO> startingFromAirport = dtosForAirportLine.stream().
                filter(dto -> dto.getStartStation().equals(ManAirport.getIdForDTO())).toList();

        assertFalse(startingFromAirport.isEmpty(), dtosForAirportLine.toString());

        startingFromAirport.forEach(fromAirport -> {
            List<LocationRefWithPosition> stations = fromAirport.getStations();
            LocationRefWithPosition start = stations.get(0);

            assertEquals(ManAirport.getIdForDTO(), start.getId(), "for route " + fromAirport);
            TestEnv.assertLatLongEquals(ManAirport.getLatLong(), start.getLatLong(), 0.00001, "position");
            assertTrue(start.getTransportModes().contains(TransportMode.Tram));

            assertEquals(Victoria.getIdForDTO(), stations.get(stations.size()-1).getId());
        });


    }

    @Test
    void shouldHaveWorkaroundForAirportRouteIdsTransposedInData() {
        Route fromAirportRoute = tramRouteHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        List<Station> results = mapper.getStationsOn(fromAirportRoute, false, ManAirport.getId());

        assertEquals(ManAirport.getId(), results.get(0).getId());
        assertEquals(Victoria.getId(), results.get(results.size()-1).getId());

    }

    @Test
    void shouldHaveWorkaroundForTraffordCentreRouteIdsTransposedInData() {
        Route fromTraffordCenter = tramRouteHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        List<Station> results = mapper.getStationsOn(fromTraffordCenter, false, TraffordCentre.getId());

        assertEquals(TraffordCentre.getId(), results.get(0).getId(), HasId.asIds(results));
        Station seventhStopAfterTrafford = results.get(7);
        assertEquals(Cornbrook.getId(), seventhStopAfterTrafford.getId(), HasId.asIds(results));

    }
}
