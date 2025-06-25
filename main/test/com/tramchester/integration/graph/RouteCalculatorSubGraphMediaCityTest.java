package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationIdPair;
import com.tramchester.domain.Route;
import com.tramchester.domain.collections.LocationIdPairSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.ImmutableGraphTransaction;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.LocationIdsAndNames;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UpcomingDates;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.TramStations.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorSubGraphMediaCityTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();

    private static final List<TramStations> tramStations = Arrays.asList(
            ExchangeSquare,
            StPetersSquare,
            Deansgate,
            Cornbrook,
            Pomona,
            ExchangeQuay,
            SalfordQuay,
            Anchorage,
            HarbourCity,
            MediaCityUK,
            TraffordBar,
            Victoria,
            Shudehill,
            MarketStreet
    );

    private ImmutableGraphTransaction txn;

    private Duration maxJourneyDuration;
    private RouteCalculationCombinations<Station> combinations;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();

        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphMediaCityTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());

        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter toConfigure, RouteRepository routeRepository) {
        tramStations.forEach(station -> toConfigure.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        combinations = new RouteCalculationCombinations<>(componentContainer, RouteCalculationCombinations.checkStationOpen(componentContainer) );
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);

    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveMediaCityToExchangeSquareSaturday() {
        TramDate nextSaturday = UpcomingDates.nextSaturday();
        validateAtLeastOneJourney(MediaCityUK, Cornbrook, TramTime.of(9,0), nextSaturday);
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), nextSaturday);
    }

    @DisabledUntilDate(year = 2025, month = 6, day = 29)
    @Test
    void shouldHaveMediaCityToExchangeSquareSunday() {
        TramDate testSunday = UpcomingDates.nextSunday();
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(10,0), testSunday);
    }

    @DisabledUntilDate(year = 2025, month = 6, day = 29)
    @Test
    void shouldHaveJourneyFromEveryStationToEveryOtherNDaysAheadEarlyMorning() {

        TramTime queryTime = TramTime.of(9, 0);
        List<Pair<TramDate, LocationIdsAndNames<Station>>> failed = UpcomingDates.getUpcomingDates().
                map(date -> new JourneyRequest(date, queryTime, false,
                        2, maxJourneyDuration, 1, getRequestedModes())).
                map(journeyRequest -> Pair.of(journeyRequest.getDate(), getFailedPairedFor(journeyRequest))).
                filter(collection -> !collection.getRight().isEmpty()).
                toList();

        assertTrue(failed.isEmpty(), failed.toString());
    }

    @DisabledUntilDate(year = 2025, month = 6, day = 29)
    @Test
    void shouldHaveJourneyFromEveryStationToEveryOtherNDaysAhead() {

        TramTime queryTime = TramTime.of(10, 30);
        List<Pair<TramDate, LocationIdsAndNames<Station>>> failed = UpcomingDates.getUpcomingDates().
                map(date -> new JourneyRequest(date, queryTime, false,
                        1, maxJourneyDuration, 1, getRequestedModes())).
                map(journeyRequest -> Pair.of(journeyRequest.getDate(), getFailedPairedFor(journeyRequest))).
                filter(pair -> !pair.getRight().isEmpty()).
                toList();

        assertTrue(failed.isEmpty(), failed.toString());
    }

    @Test
    void shouldHaveExpectedInterchanges() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        IdSet<Station> interchangesIds = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertTrue(interchangesIds.contains(Cornbrook.getId()));
        assertTrue(interchangesIds.contains(Pomona.getId()));
        assertTrue(interchangesIds.contains(Deansgate.getId()));

    }

    @Test
    void shouldHaveSalfordQuayToStPeters() {
        final TramTime time = TramTime.of(8, 5);

        // 2 -> 4
        int maxChanges = 4;
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, getRequestedModes());

        List<Journey> results = calculator.calculateRouteAsList(SalfordQuay.getId(), StPetersSquare.getId(), journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveExpectedRouteOverlaps() {
        Station salfordQuay = SalfordQuay.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);

        Station cornbrook = Cornbrook.from(stationRepository);

        Set<Route> fromSalford = salfordQuay.getPickupRoutes();
        Set<Route> dropOffAtCornbrook = cornbrook.getDropoffRoutes();

        SetUtils.SetView<Route> salfordToCornbrook = SetUtils.union(fromSalford, dropOffAtCornbrook);

        assertFalse(salfordToCornbrook.isEmpty());

        Set<Route> salfordToCornbrookOnDate = salfordToCornbrook.stream().filter(route -> route.isAvailableOn(when)).collect(Collectors.toSet());

        assertFalse(salfordToCornbrookOnDate.isEmpty());

        Set<Route> fromCornbrook = cornbrook.getPickupRoutes();
        Set<Route> dropOffAtStPetersSquare = stPetersSquare.getDropoffRoutes();

        SetUtils.SetView<Route> cornbrookToStPetersSquare = SetUtils.union(fromCornbrook, dropOffAtStPetersSquare);

        assertFalse(cornbrookToStPetersSquare.isEmpty());

        Set<Route> cornbrookToStPetersSquareOnDate = cornbrookToStPetersSquare.stream().filter(route -> route.isAvailableOn(when)).collect(Collectors.toSet());

        assertFalse(cornbrookToStPetersSquareOnDate.isEmpty());

    }

    @Test
    void shouldHaveExpectedRouteConnections() {
        Station salfordQuay = SalfordQuay.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(8,5), TramTime.of(8,30));
        int result = getPossibleMinChanges(salfordQuay, stPetersSquare, getRequestedModes(), when, timeRange);

        assertEquals(result, 0);
    }

    private int getPossibleMinChanges(Location<?> being, Location<?> end, EnumSet<TransportMode> modes, TramDate date, TimeRange timeRange) {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        JourneyRequest journeyRequest = new JourneyRequest(date, timeRange.getStart(), false, JourneyRequest.MaxNumberOfChanges.of(1),
                Duration.ofMinutes(120), 1, modes);
        return routeToRouteCosts.getNumberOfChanges(being, end, journeyRequest, timeRange);
    }

    @Test
    void shouldHaveJourneyFromEveryStationToEveryOther() {

        final TramTime time = TramTime.of(8, 5);

        // 2 -> 4
        int maxChanges = 4;
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, getRequestedModes());

        // pairs of stations to check
        LocationIdsAndNames<Station> results = getFailedPairedFor(journeyRequest);
        assertTrue(results.isEmpty(), results + " failed for " + journeyRequest);
    }

    private LocationIdsAndNames<Station> getFailedPairedFor(final JourneyRequest journeyRequest) {
        final TramDate date = journeyRequest.getDate();
        Set<Station> stations = tramStations.stream().
                filter(station -> !UpcomingDates.hasClosure(station, date)).
                map(tramStations -> tramStations.from(stationRepository)).
                filter(station -> !closedStationRepository.isClosed(station, date)).
                collect(Collectors.toSet());

        assertFalse(stations.isEmpty(), "No stations for + " + date);

        LocationIdPairSet<Station> stationIdPairs = stations.stream().flatMap(start -> stations.stream().
                        filter(dest -> !dest.getId().equals(start.getId())).
                        filter(dest -> !combinations.betweenInterchanges(start, dest)).
                        map(dest -> LocationIdPair.of(start, dest))).
                        filter(pair -> !pair.same()).
                        collect(LocationIdPairSet.collector());

        assertFalse(stationIdPairs.isEmpty(), "No pairs for " + date + " from stations " + HasId.asIds(stations));

        RouteCalculationCombinations.CombinationResults<Station> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);

        return results.getMissing();

    }

    private EnumSet<TransportMode> getRequestedModes() {
        return TramsOnly;
    }

    @Test
    void reproduceMediaCityIssue() {
        validateAtLeastOneJourney(ExchangeSquare, MediaCityUK, TramTime.of(12,0), when);
    }

    @Test
    void reproduceMediaCityIssueSaturdays() {
        validateAtLeastOneJourney(ExchangeSquare, MediaCityUK, TramTime.of(9,0), UpcomingDates.nextSaturday());
    }

    @Test
    void shouldHaveSimpleJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(12, 0), false, 3,
                maxJourneyDuration, 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(TramStations.Pomona, MediaCityUK, journeyRequest);
        assertFalse(results.isEmpty());
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_mediacity_trams.dot"), MediaCityUK.fake(), 100, true);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super(IntegrationTramTestConfig.CurrentClosures);
        }

        @Override
        public boolean isGraphFiltered() {
            return true;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {

            IdSet<Station> additionalInterchanges = AdditionalTramInterchanges.stations();
            additionalInterchanges.add(Cornbrook.getId());
            additionalInterchanges.add(Pomona.getId());
            additionalInterchanges.add(Deansgate.getId());

            final Set<TransportMode> groupStationModes = Collections.emptySet(); //Collections.singleton(TransportMode.Bus);

            TFGMGTFSSourceTestConfig gtfsSourceConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                    Tram, additionalInterchanges, groupStationModes, IntegrationTramTestConfig.CurrentClosures,
                    Duration.ofMinutes(45), Collections.emptyList());

            return Collections.singletonList(gtfsSourceConfig);
        }

        @Override
        public Path getCacheFolder() {
            return TestEnv.CACHE_DIR.resolve("RouteCalculatorSubGraphMediaCityTest");
        }
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, TramDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(date, time, false, 5,
                maxJourneyDuration, 1, getRequestedModes());
        List<Journey> results = calculator.calculateRouteAsList(start, dest, journeyRequest);
        assertFalse(results.isEmpty(), format("no journey from %s to %s at %s %s", start, dest, date, time));
    }
}
