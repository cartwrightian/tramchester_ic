package com.tramchester.integration;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.CentralZoneStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.routes.RoutePairToInterchangeRepository;
import com.tramchester.integration.testSupport.config.ConfigParameterResolver;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.MultiMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.CentralZoneStation.*;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@MultiMode
public class RoutePairToInterchangeRepositoryTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TramRouteHelper routeHelper;
    private final EnumSet<TransportMode> modes = TramsOnly;
    private TramDate date;
    private RoutePairToInterchangeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // Clear Cache - test creation here
        TestEnv.clearDataCache(componentContainer);

        config = tramchesterConfig;
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {

        // Clear Cache
        TestEnv.clearDataCache(componentContainer); // => this removes the index cache
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeHelper = new TramRouteHelper(componentContainer);

        date = TestEnv.testDay().plusWeeks(1);

        repository = componentContainer.get(RoutePairToInterchangeRepository.class);
    }

    @Test
    void shouldGetExpectedSingleInterchangesBetweenRoutes() {
        Route toTraffordCentre = routeHelper.getRed(date);
        Route toAirport = routeHelper.getNavy(date);

        RoutePair routeIndexPair = RoutePair.of(toTraffordCentre, toAirport);

        assertTrue(repository.hasAnyInterchangesFor(routeIndexPair));

        Set<InterchangeStation> interchanges = repository.getInterchanges(routeIndexPair, modes);

        IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStation).collect(IdSet.collector());

        if (config.hasRailConfig()) {
            assertEquals(3, stationIds.size(), stationIds.toString());
            assertTrue(stationIds.contains(Deansgate.getId()));
            assertTrue(stationIds.contains(RailStationIds.ManchesterDeansgate.getId()));
        } else {
            assertEquals(1, stationIds.size(), stationIds.toString());
        }

        assertTrue(stationIds.contains(Cornbrook.getId()), stationIds.toString());
        //assertTrue(stationIds.contains(StPetersSquare.getId()), stationIds.toString());
    }

    @Test
    void shouldGetExpectedMultipleInterchangesBetweenRoutes() {
        Route blueLine = routeHelper.getBlue(date);
        Route navyLine = routeHelper.getNavy(date);

        RoutePair routeIndexPair = RoutePair.of(blueLine, navyLine);

        assertTrue(repository.hasAnyInterchangesFor(routeIndexPair));

        Set<InterchangeStation> interchanges = repository.getInterchanges(routeIndexPair, modes);

        IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStation).collect(IdSet.collector());

        IdSet<Station> expected = Stream.of(
                StPetersSquare,
                MarketStreet,
                Victoria,
                //Deansgate,
                Cornbrook,
                TraffordBar
//                        Piccadilly,
//                        PiccadillyGardens,
                        //Shudehill
                        ).
                map(CentralZoneStation::getId).
                collect(IdSet.idCollector());

        if (config.hasRailConfig()) {
            expected.add(RailStationIds.ManchesterVictoria.getId());
            expected.add(RailStationIds.ManchesterDeansgate.getId());
            expected.add(TramStations.Deansgate.getId());
        }

        IdSet<Station> diff = IdSet.disjunction(expected, stationIds);

        assertEquals(IdSet.emptySet(), diff, "mismatch between expected " + expected + " and " + stationIds);

    }


}
