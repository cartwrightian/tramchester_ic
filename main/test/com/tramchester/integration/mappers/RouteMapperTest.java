package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void shouldHaveWorkaroundForAirportRouteIdsTransposedInData() {
        Route fromAirportRoute = tramRouteHelper.getOneRoute(getNavy(date), date);

        List<Station> results = mapper.getStationsOn(fromAirportRoute, false, ManAirport.getId());

        assertEquals(ManAirport.getId(), results.getFirst().getId());
        assertEquals(Victoria.getId(), results.getLast().getId());

    }

    @Test
    void shouldHaveWorkaroundForTraffordCentreRouteIdsTransposedInData() {
        Route fromTraffordCenter = tramRouteHelper.getOneRoute(getRed(date), date);

        List<Station> results = mapper.getStationsOn(fromTraffordCenter, false, TraffordCentre.getId());

        assertEquals(TraffordCentre.getId(), results.get(0).getId(), HasId.asIds(results));
        Station seventhStopAfterTrafford = results.get(7);
        assertEquals(Cornbrook.getId(), seventhStopAfterTrafford.getId(), HasId.asIds(results));

    }
}
