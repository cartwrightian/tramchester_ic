package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RailRouteHelper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteToRouteCostsTest {
    private StationRepository stationRepository;
    private static ComponentContainer componentContainer;

    private TramDate date;
    private RouteToRouteCosts routeToRouteCosts;
    private EnumSet<TransportMode> allTransportModes;
    private RailRouteHelper railRouteHelper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void afterAllTestsRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        date = TestEnv.testDay();
        allTransportModes = EnumSet.allOf(TransportMode.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);
        railRouteHelper = new RailRouteHelper(componentContainer);
    }

    @Test
    void shouldValidHopsBetweenTramAndRailLongRange() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        EnumSet<TransportMode> all = allTransportModes;
        int result = routeToRouteCosts.getPossibleMinChanges(tram(Bury), rail(Stockport),
                all, date, timeRange);

        assertEquals(1, result);
        //assertEquals(2, result.getMax());
    }

    @Test
    void shouldValidHopsBetweenTramAndRailNeighbours() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        int result = routeToRouteCosts.getPossibleMinChanges(tram(Altrincham), rail(RailStationIds.Altrincham),
                allTransportModes, date, timeRange);

        assertEquals(0, result);
    }

    // TODO better to handle this case by passing in an extended list of modes ??

    // TODO is served route working with Interchange Stations correctly?
    // Rail station Man Picc is not returning tram routes, but is an interchange station

    @Test
    void shouldValidHopsBetweenTramAndInterchangceWhenConnectPossibleTramOnly() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        int result = routeToRouteCosts.getPossibleMinChanges(tram(Altrincham), rail(ManchesterPiccadilly),
                TramsOnly, date, timeRange);

        assertEquals(0, result);
    }

    @Test
    void shouldValidHopsBetweenInterchangceAndTramWhenConnectPossibleTramOnly() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        int result = routeToRouteCosts.getPossibleMinChanges(rail(RailStationIds.Altrincham), tram(Piccadilly),
                TramsOnly, date, timeRange);

        assertEquals(0, result);
    }

    @Test
    void shouldValidHopsBetweenTramAndRailNeighbourThenTrain() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        int result = routeToRouteCosts.getPossibleMinChanges(tram(Altrincham), rail(Stockport),
                allTransportModes, date, timeRange);

        // direct via altrincham rail
        assertEquals(0, result);
    }

    //@Disabled("Is this realistic? Trains only but start at a tram station")
    @Test
    void shouldValidHopsBetweenTramAndRailNeighbourThenTrainWhenOnlyTrainModeEnabled() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        // should find a result since Altrincham -> Altincham Rail -> Stockport iff neighbours enabled
        int result = routeToRouteCosts.getPossibleMinChanges(tram(Altrincham), rail(Stockport),
                EnumSet.of(Train), date, timeRange);

        assertEquals(0, result);
    }

    @Test
    void shouldValidHopsBetweenTramAndRailShortRange() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        int result = routeToRouteCosts.getPossibleMinChanges(tram(Cornbrook), rail(RailStationIds.Altrincham),
                allTransportModes, date, timeRange);

        assertEquals(1, result);
    }

    @Test
    void shouldNotHaveHopsBetweenTramAndRailWhenTramOnly() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        EnumSet<TransportMode> preferredModes = EnumSet.of(Tram);

        int result = routeToRouteCosts.getPossibleMinChanges(tram(TramStations.Bury), rail(Stockport),
                preferredModes, date, timeRange);

        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    void shouldHaveCorrectHopsBetweenRailStationsOnly() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        EnumSet<TransportMode> preferredModes = EnumSet.of(Train);
        int result = routeToRouteCosts.getPossibleMinChanges(rail(ManchesterPiccadilly), rail(Stockport),
                preferredModes, date, timeRange);

        assertEquals(0, result); // non stop
    }

    @Test
    void shouldHaveCorrectHopsBetweenTramStationsOnly() {
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 15), TramTime.of(22, 35));

        int result = routeToRouteCosts.getPossibleMinChanges(tram(Cornbrook), tram(StPetersSquare),
                allTransportModes, date, timeRange);

        assertEquals(0, result);
    }

    @Test
    void shouldHaveOneChangeRochdaleToEccles() {
        // Rochdale, Eccles
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,0), TramTime.of(10,0));
        Station rochdale = TramStations.Rochdale.from(stationRepository);
        Station eccles = TramStations.Eccles.from(stationRepository);
        int changes = routeToRouteCosts.getPossibleMinChanges(rochdale, eccles, TramsOnly, date, timeRange);

        // 1 -> 2 , eccles replacement bus
        assertEquals(2, changes);
    }

    @Test
    void shouldRHaveChangesBetweenLiverpoolAndCreweRoutes() {
        // repro issue in routecostmatric
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,0), TramTime.of(10,0));

        Route routeA = railRouteHelper.getRoute(TrainOperatingCompanies.NT, RailStationIds.ManchesterVictoria, LiverpoolLimeStreet, 1);
        Route routeB = railRouteHelper.getRoute(TrainOperatingCompanies.NT, Crewe, ManchesterPiccadilly, 2);

        int changes = routeToRouteCosts.getPossibleMinChanges(routeA, routeB, date, timeRange, allTransportModes);

        assertEquals(2, changes);

    }

    @Test
    void shouldReproduceErrorWithPinkAndYellowRoutesTramOnly() {
        // error was due to handling of linked interchange stations in StationAvailabilityRepository
        // specifically because Victoria is linked to rail routes and is only place to change between pink/yellow route
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8, 45), TramTime.of(16, 45));

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        TramRouteHelper tramRouteHelper = new TramRouteHelper(routeRepository);

        Route yellowInbound = tramRouteHelper.getOneRoute(KnownTramRoute.getPiccadillyVictoria(date), date);
        Route pinkOutbound = tramRouteHelper.getOneRoute(KnownTramRoute.getShawandCromptonManchesterEastDidisbury(date), date);

        int routeToRouteResult = routeToRouteCosts.getPossibleMinChanges(yellowInbound, pinkOutbound, date, timeRange, TramsOnly);

        assertFalse(routeToRouteResult==Integer.MAX_VALUE);

    }

    private Station tram(TramStations tramStation) {
        return tramStation.from(stationRepository);
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
    }
}
