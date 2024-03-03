package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.diagnostics.DiagnosticReasonDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsDTO;
import com.tramchester.domain.presentation.DTO.diagnostics.StationDiagnosticsLinkDTO;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.DualTest;
import com.tramchester.testSupport.testTags.VictoriaCrackedRailTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.Durations.greaterOrEquals;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.TestEnv.avoidChristmasDate;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("JUnitTestMethodWithNoAssertions")
@ExtendWith(ConfigParameterResolver.class)
@DualTest
@DataUpdateTest
public class RouteCalculatorTest {

    // Note this needs to be > time for whole test fixture, see note below in @After
    private static final int TXN_TIMEOUT = 5*60;

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static TramchesterConfig config;

    private final int maxChanges = 4;

    private static EnumSet<TransportMode> requestedModes;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();
    private MutableGraphTransaction txn;
    private Duration maxJourneyDuration;
    private int maxNumResults;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
        requestedModes = TramsOnly;
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        txn = database.beginTxMutable(TXN_TIMEOUT, TimeUnit.SECONDS);
        calculator = new RouteCalculatorTestFacade(componentContainer, txn);
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        maxNumResults = config.getMaxNumResults();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldReproIssueWithChangesVeloToTraffordBar() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, VeloPark, TraffordBar);
    }

    @Test
    void shouldPlanSimpleJourneyFromAltyToAshtonCheckInterchangesAndHaveExpectedIndexes() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(17,45), false,
                3, maxJourneyDuration, 5, requestedModes);

        // todo added market street and shudehill
        Set<String> expected = Stream.of(Cornbrook, StPetersSquare, Deansgate, Piccadilly, MarketStreet, Shudehill).
                map(TramStations::getName).collect(Collectors.toSet());

        List<Journey> journeys = calculator.calculateRouteAsList(Altrincham, Ashton, journeyRequest);
        assertFalse(journeys.isEmpty(), journeyRequest.toString());

        Set<Integer> indexes = new HashSet<>();

        journeys.forEach(journey -> {
            TransportStage<?, ?> firstStage = journey.getStages().get(0);
            String interchange = firstStage.getLastStation().getName();
            indexes.add(journey.getJourneyIndex());
            assertTrue(expected.contains(interchange), interchange + " not in " + expected);
        });

        assertEquals(journeys.size(), indexes.size());
        // todo due to filtering of duplicates this is not possible
//        for (int i = 0; i < journeys.size(); i++) {
//            assertTrue(indexes.contains(i), "Missing index " + i);
//        }

    }

    @Test
    void shouldHaveExpectedPathsForSimpleJoruney() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(17,45), false,
                3, maxJourneyDuration, 5, requestedModes);

        calculator.calculateRouteAsList(TraffordBar, Deansgate, journeyRequest);
    }

    @Test
    void shouldHaveSimpleOneStopJourneyNextDays() {
        checkRouteNextNDays(TraffordBar, Altrincham, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleManyStopSameLineJourney() {
        checkRouteNextNDays(Altrincham, Cornbrook, when, TramTime.of(9,0), DAYS_AHEAD);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveSimpleManyStopJourneyViaInterchangeNDaysAhead() {
        checkRouteNextNDays(StPetersSquare, ManAirport, when, TramTime.of(12,0), DAYS_AHEAD);
    }

    @Test
    void shouldHaveSimpleJourney() {
        final TramTime originalQueryTime = TramTime.of(10, 15);
        JourneyRequest journeyRequest = new JourneyRequest(when, originalQueryTime, false, maxChanges,
                maxJourneyDuration, maxNumResults, requestedModes);
        List<Journey> journeys = calculator.calculateRouteAsList(Altrincham, Deansgate, journeyRequest);
        List<Journey> results = checkJourneys(Altrincham, Deansgate, originalQueryTime, journeyRequest.getDate(), journeys);

        results.forEach(journey -> {
            List<Location<?>> pathCallingPoints = journey.getPath();

            assertEquals(11, pathCallingPoints.size());
            assertEquals(Altrincham.getId(), pathCallingPoints.get(0).getId());
            assertEquals(LocationType.Station, pathCallingPoints.get(0).getLocationType());
            assertEquals(Deansgate.getId(), pathCallingPoints.get(10).getId());

            List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(1, stages.size(), "wrong number stages " + stages);
            TransportStage<?, ?> stage = stages.get(0);
            assertEquals(9, stage.getPassedStopsCount());
            List<StopCall> callingPoints = stage.getCallingPoints();
            assertEquals(9, callingPoints.size());
            assertEquals(NavigationRoad.getId(), callingPoints.get(0).getStationId());
            assertEquals(Cornbrook.getId(), callingPoints.get(8).getStationId());
        });
    }

    @Test
    void shouldHaveFirstResultWithinReasonableTimeOfQuery() {
        Duration cutoffInterval = Duration.ofMinutes(16);
        final TramTime queryTime = TramTime.of(17, 45);
        JourneyRequest journeyRequest = standardJourneyRequest(when, queryTime, maxNumResults);

        List<Journey> journeys = calculator.calculateRouteAsList(Altrincham, Ashton, journeyRequest);

        Optional<Journey> earliest = journeys.stream().min(TramTime.comparing(Journey::getDepartTime));
        assertTrue(earliest.isPresent());

        final TramTime firstDepartTime = earliest.get().getDepartTime();
        Duration elapsed = TramTime.difference(queryTime, firstDepartTime);
        assertFalse(greaterOrEquals(elapsed, cutoffInterval), "first result too far in future " + firstDepartTime
                + " cuttoff " + cutoffInterval + " earliest journey " + earliest) ;
    }

    @Test
    void shouldHaveSameResultWithinReasonableTime() {
        final TramTime queryTimeA = TramTime.of(8, 50);
        final TramTime queryTimeB = queryTimeA.plusMinutes(3);

        JourneyRequest journeyRequestA = standardJourneyRequest(when, queryTimeA, maxNumResults);
        JourneyRequest journeyRequestB = standardJourneyRequest(when, queryTimeB, maxNumResults);

        List<Journey> journeysA = calculator.calculateRouteAsList(ManAirport, EastDidsbury, journeyRequestA);
        List<Journey> journeysB = calculator.calculateRouteAsList(ManAirport, EastDidsbury, journeyRequestB);

        Optional<Journey> earliestA = journeysA.stream().min(TramTime.comparing(Journey::getDepartTime));
        assertTrue(earliestA.isPresent());

        Optional<Journey> earliestB = journeysB.stream().min(TramTime.comparing(Journey::getDepartTime));
        assertTrue(earliestB.isPresent());

        final TramTime firstDeparttimeA = earliestA.get().getDepartTime();
        final TramTime firstDeparttimeB = earliestB.get().getDepartTime();

        assertTrue(firstDeparttimeA.isAfter(queryTimeB) || firstDeparttimeA.equals(queryTimeB),
                firstDeparttimeA + " not after " + queryTimeB); // check assumption first
        assertEquals(firstDeparttimeA, firstDeparttimeB);
    }

    @Test
    void shouldHaveReasonableJourneyAltyToDeansgate() {
        JourneyRequest request = standardJourneyRequest(when, TramTime.of(10, 15), maxNumResults);

        List<Journey> results = calculator.calculateRouteAsList(Altrincham, Deansgate, request);

        assertFalse(results.isEmpty());
        results.forEach(journey -> {
            assertEquals(1, journey.getStages().size()); // should be one stage only
            journey.getStages().stream().
                    map(raw -> (VehicleStage) raw).
                    map(VehicleStage::getCost).
                    forEach(cost -> assertFalse(cost.isZero() || cost.isNegative()));
            Duration total = journey.getStages().stream().
                    map(raw -> (VehicleStage) raw).
                    map(VehicleStage::getCost).
                    reduce(Duration.ZERO, Duration::plus);
            assertTrue(total.compareTo(Duration.ofMinutes(20))>0);
        });
    }

    @Disabled("Failing due to temporarily less frequency service")
    @Test
    void shouldUseAllRoutesCorrectlWhenMultipleRoutesServDestination() {

        TramStations start = Altrincham;

        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(10, 21), maxNumResults);

        List<Journey> servedByBothRoutes = calculator.calculateRouteAsList(start, Deansgate, journeyRequest);
        List<Journey> altyToPiccGardens = calculator.calculateRouteAsList(start, PiccadillyGardens, journeyRequest);
        List<Journey> altyToMarketStreet = calculator.calculateRouteAsList(start, MarketStreet, journeyRequest);

        assertEquals(altyToPiccGardens.size()+altyToMarketStreet.size(), servedByBothRoutes.size());
    }


    // over max wait, catch failure to accumulate journey times correctly
    @Test
    void shouldHaveSimpleButLongJoruneySameRoute() {
        checkRouteNextNDays(ManAirport, TraffordBar, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldHaveLongJourneyAcross() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, Rochdale);
    }

    @Test
    void shouldHaveReasonableLongJourneyAcrossFromInterchange() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8, 0), maxNumResults);
        List<Journey> journeys = calculator.calculateRouteAsList(Monsall, RochdaleRail, journeyRequest);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            // direct, or change at shaw
            assertTrue(journey.getStages().size()<=2, Integer.toString(journey.getStages().size()));
        });
    }

    @Test
    void shouldHaveSimpleManyStopJourneyStartAtInterchange() {
        checkRouteNextNDays(Cornbrook, Bury, when, TramTime.of(9,0), 1);
    }

    @Test
    void shouldLimitNumberChangesResultsInNoJourneys() {
        TramDate today = TramDate.from(TestEnv.LocalNow());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, 0,
                maxJourneyDuration, 1, requestedModes);
        List<Journey> results = calculator.calculateRouteAsList(Altrincham, ManAirport, request);

        assertEquals(0, results.size());
    }

    @Test
    void shouldNotReturnBackToStartOnJourney() {
        TramDate today = TramDate.from(TestEnv.LocalNow());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(20, 9), false, 2,
                maxJourneyDuration, 6, requestedModes);

        //request.setCachingDisabled(true);

        List<Journey> results = calculator.calculateRouteAsList(Deansgate, ManAirport, request);

        assertFalse(results.isEmpty(),"no journeys found");

        results.forEach(result -> {
            long seenStart = result.getPath().stream().filter(location -> location.getId().equals(Deansgate.getId())).count();
            assertEquals(1, seenStart, "seen start location again");
        });
    }

    @Test
    void testJourneyFromAltyToAirport() {
        TramDate today = TramDate.from(TestEnv.LocalNow());

        JourneyRequest request = new JourneyRequest(today, TramTime.of(11, 43), false, maxChanges,
                maxJourneyDuration, maxNumResults, requestedModes);
        List<Journey> results =  calculator.calculateRouteAsList(Altrincham, ManAirport, request);

        assertFalse(results.isEmpty(), "no results");    // results is iterator
        for (Journey result : results) {
            List<TransportStage<?,?>> stages = result.getStages();
            assertEquals(2,stages.size());
            VehicleStage firstStage = (VehicleStage) stages.get(0);
            assertEquals(Altrincham.getId(), firstStage.getFirstStation().getId());
            assertEquals(TraffordBar.getId(), firstStage.getLastStation().getId(), stages.toString());
            assertEquals(Tram, firstStage.getMode());
            assertEquals(7, firstStage.getPassedStopsCount());

            VehicleStage finalStage = (VehicleStage) stages.get(stages.size()-1);
            //assertEquals(Stations.TraffordBar, secondStage.getFirstStation()); // THIS CAN CHANGE
            assertEquals(ManAirport.getId(), finalStage.getLastStation().getId());
            assertEquals(Tram, finalStage.getMode());
        }
    }

    @Test
    void shouldHandleCrossingMidnightWithChange() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(23,30), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, TraffordCentre, TraffordBar);
    }

    @Test
    void shouldHandleCrossingMidnightWithChangeCentral() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(22,15), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Monsall, Piccadilly);
    }

    @Test
    void shouldHandleCrossingMidnightDirectCornbrookStPeters() {
        JourneyRequest journeyRequestA = standardJourneyRequest(when, TramTime.of(23, 55), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestA, Cornbrook, StPetersSquare);
    }

    @Test
    void shouldHandleCrossingMidnightDirectAltrinchamToNavigationRoad() {

        JourneyRequest journeyRequestB = standardJourneyRequest(when, TramTime.of(23,55), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestB, Altrincham, OldTrafford);
    }

    @Test
    void shouldHandleAtMidnightDirectCornbrookStPeters() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.nextDay(0,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, StPetersSquare);
    }

    @Test
    void shouldHandlePastMidnightDirectCornbrookStPeters() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.nextDay(0,1), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, StPetersSquare);
    }

    @Test
    void shouldHandleAtMidnightDirectAltrinchamNavigation() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.nextDay(0,0), maxNumResults);
        //journeyRequest.setDiag(true);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, NavigationRoad);
    }

    @Test
    void shouldHandleJustBeforeMidnightDirectAltrinchamNavigation() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(23,59), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, NavigationRoad);
    }

    @Test
    void shouldHandlePastMidnightDirectAltrinchamNavigation() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.nextDay(0,1), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Altrincham, NavigationRoad);
    }

    @Test
    void shouldHandleAfterMidnightDirectCentral() {
        TramDate testDate = when;

        JourneyRequest journeyRequestA = standardJourneyRequest(testDate, TramTime.of(23,59), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestA, StPetersSquare, MarketStreet);

        JourneyRequest journeyRequestB = standardJourneyRequest(testDate, TramTime.nextDay(0,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestB, StPetersSquare, MarketStreet);

        JourneyRequest journeyRequestC = standardJourneyRequest(testDate, TramTime.nextDay(0,1), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestC, StPetersSquare, MarketStreet);

        // last tram is now earlier
//        JourneyRequest journeyRequestD = standardJourneyRequest(testDate, TramTime.of(0,0), maxNumResults);
//        assertGetAndCheckJourneys(journeyRequestD, StPetersSquare, MarketStreet);
    }

    @Test
    void shouldHaveHeatonParkToBurtonRoad() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(7, 30), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, HeatonPark, BurtonRoad);
    }

    @Test
    void shouldReproIssueRochInterchangeToBury() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(9, 0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Rochdale, Bury);
    }

    @VictoriaCrackedRailTest
    @Test
    void shouldReproIssueWithMediaCityTrams() {

        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(12, 0), maxNumResults);

        assertGetAndCheckJourneys(journeyRequest, StPetersSquare, MediaCityUK);
        assertGetAndCheckJourneys(journeyRequest, ExchangeSquare, MediaCityUK);
    }

    public static Duration costOfJourney(final Journey journey) {
        final TramTime departs = journey.getDepartTime();
        final TramTime arrive = journey.getArrivalTime();
        return TramTime.difference(departs, arrive);
    }

    @Test
    void shouldCheckCornbrookToStPetersSquareOnSundayMorning() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(11, 0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, StPetersSquare);
    }

    @Test
    void shouldNotGenerateDuplicateJourneysForSameReqNumChanges() {

        JourneyRequest request = new JourneyRequest(when, TramTime.of(11, 45), false,
                4, maxJourneyDuration, 3, requestedModes);
        List<Journey> journeys =  calculator.calculateRouteAsList(Bury, Altrincham, request);

        assertFalse(journeys.isEmpty());

        Set<Integer> reqNumChanges = journeys.stream().map(Journey::getRequestedNumberChanges).collect(Collectors.toSet());

        reqNumChanges.forEach(numChange -> {
            Set<List<TransportStage<?,?>>> uniqueStages = new HashSet<>();

            journeys.stream().filter(journey -> numChange.equals(journey.getRequestedNumberChanges())).forEach(journey -> {

                assertFalse(uniqueStages.contains(journey.getStages()),
                        journey.getStages() + " seen before in " + journeys);
                uniqueStages.add(journey.getStages());
            });
        });
    }

    @Test
    void ShouldReproIssueWithSomeMediaCityJourneys() {
        JourneyRequest request = new JourneyRequest(when, TramTime.of(8, 5), false,
                1, maxJourneyDuration, 2, requestedModes);

        assertFalse(calculator.calculateRouteAsList(MediaCityUK, Etihad, request).isEmpty());
        assertFalse(calculator.calculateRouteAsList(MediaCityUK, Ashton, request).isEmpty());
        assertFalse(calculator.calculateRouteAsList(MediaCityUK, VeloPark, request).isEmpty());
    }

    @Test
    void shouldHaveInAndAroundCornbrookToEccles8amTuesday() {
        // catches issue with services, only some of which go to media city, while others direct to broadway
        JourneyRequest journeyRequest8am = standardJourneyRequest(when, TramTime.of(8,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest8am, Cornbrook, Broadway);
        assertGetAndCheckJourneys(journeyRequest8am, Cornbrook, Eccles);

        JourneyRequest journeyRequest9am = standardJourneyRequest(when, TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest9am, Cornbrook, Broadway);
        assertGetAndCheckJourneys(journeyRequest9am, Cornbrook, Eccles);
    }

    @Test
    void shouldReproIssueWithJourneysToEccles() {
        TramDate testDate = this.when.plusWeeks(1);

        JourneyRequest journeyRequest = standardJourneyRequest(testDate, TramTime.of(9,0), maxNumResults);

        assertGetAndCheckJourneys(journeyRequest, Bury, Broadway);
        assertGetAndCheckJourneys(journeyRequest, Bury, Eccles);
    }

    @Test
    void reproduceIssueEdgePerTrip() {
        // see also RouteCalculatorSubGraphTest
        JourneyRequest journeyRequestA = standardJourneyRequest(when, TramTime.of(19,48), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestA, PiccadillyGardens, Pomona);

        JourneyRequest journeyRequestB = standardJourneyRequest(when, TramTime.of(19,51), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestB, StPetersSquare, Pomona);

        JourneyRequest journeyRequestC = standardJourneyRequest(when, TramTime.of(19,56), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestC, StPetersSquare, Pomona);

        JourneyRequest journeyRequestD = standardJourneyRequest(when, TramTime.of(6,40), maxNumResults);
        assertGetAndCheckJourneys(journeyRequestD, Cornbrook, Weaste);
    }

    @Test
    void shouldReproIssueWithStPetersToBeyondEcclesAt8AM() {
        List<TramTime> missingTimes = checkRangeOfTimes(Cornbrook, Eccles);
        assertTrue(missingTimes.isEmpty(), missingTimes.toString());
    }

    @Test
    void reproduceIssueWithImmediateDepartOffABoardedTram() {
        JourneyRequest journeyRequest = standardJourneyRequest(when, TramTime.of(8,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Deansgate, Ashton);
    }

    @Test
    void reproduceIssueWithTramsSundayStPetersToDeansgate() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, StPetersSquare, Deansgate);
    }

    @Test
    void reproduceIssueWithTramsSundayAshtonToEccles() {
        JourneyRequest journeyRequest = new JourneyRequest(TestEnv.nextSunday(), TramTime.of(9, 0), false,
                3, maxJourneyDuration, maxNumResults, requestedModes);
        assertGetAndCheckJourneys(journeyRequest, Ashton, Eccles);
    }

    @Test
    void reproduceIssueWithTramsSundayToFromEcclesAndCornbrook() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(9,0), maxNumResults);

        assertGetAndCheckJourneys(journeyRequest, Cornbrook, Eccles);
        assertGetAndCheckJourneys(journeyRequest, Eccles, Cornbrook);
    }

    @Test
    void shouldReproduceIssueCornbrookToAshtonSatursdays() {
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSaturday(), TramTime.of(9,0), maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Cornbrook, Ashton);
    }

    @Test
    void shouldFindRouteVeloToHoltTownAt8RangeOfTimes() {
        for(int i=0; i<60; i++) {
            TramTime time = TramTime.of(8,i);
            JourneyRequest journeyRequest = standardJourneyRequest(when, time, maxNumResults);
            assertGetAndCheckJourneys(journeyRequest, VeloPark, HoltTown);
        }
    }

    @Test
    void reproIssueRochdaleToEccles() {
        TramTime time = TramTime.of(9,0);
        JourneyRequest journeyRequest = standardJourneyRequest(when, time, maxNumResults);
        assertGetAndCheckJourneys(journeyRequest, Rochdale, Eccles);
    }

    @Disabled("diag testing only")
    @Test
    void shouldNotFindJourney() {
        // time needs to be when trams still running?
        JourneyRequest journeyRequest = standardJourneyRequest(TestEnv.nextSunday(), TramTime.of(1,0), maxNumResults);
        journeyRequest.setDiag(true);

        List<Journey> journeys = calculator.calculateRouteAsList(Bury, Altrincham, journeyRequest);

        // todo handle failed journeys, only returned when diagnostics enabled
        assertTrue(journeys.isEmpty());

        assertTrue(journeyRequest.hasReceivedDiagnostics());

        JourneyDiagnostics results = journeyRequest.getDiagnostics();
        assertNotNull(results);

        Optional<StationDiagnosticsDTO> findBury = results.getDtoList().stream().filter(item -> Bury.getIdForDTO().equals(item.getBegin().getId())).findFirst();

        assertTrue(findBury.isPresent());

        StationDiagnosticsDTO bury = findBury.get();

        List<StationDiagnosticsLinkDTO> links = bury.getLinks();
        assertEquals(1, links.size());

        StationDiagnosticsLinkDTO stationDiagnosticsLinkDTO = links.get(0);
        IdFor<Station> radcliffeId = Station.createId("9400ZZMARAD");
        assertEquals(new IdForDTO(radcliffeId), stationDiagnosticsLinkDTO.getTowards().getId());

        List<DiagnosticReasonDTO> notAvailables = stationDiagnosticsLinkDTO.getReasons().
                stream().filter(reason -> reason.getCode() == ReasonCode.DestinationUnavailableAtTime).toList();

        assertEquals(1, notAvailables.size());

        //String text = notAvailables.get(0).getText();
        //assertTrue(text.contains("01:01"), text);

    }

    @NotNull
    private JourneyRequest standardJourneyRequest(TramDate date, TramTime time, long maxNumberJourneys) {
        return new JourneyRequest(date, time, false, maxChanges, maxJourneyDuration, maxNumberJourneys, requestedModes);
    }

    private void checkRouteNextNDays(TramStations start, TramStations dest, TramDate date, TramTime time, int numDays) {
        if (!dest.equals(start)) {
            for(int day = 0; day< numDays; day++) {
                TramDate testDate = avoidChristmasDate(date.plusDays(day));
                JourneyRequest journeyRequest = standardJourneyRequest(testDate, time, maxNumResults);
                assertGetAndCheckJourneys(journeyRequest, start, dest);
            }
        }
    }

    private void assertGetAndCheckJourneys(JourneyRequest journeyRequest, TramStations start, TramStations dest) {
        List<Journey> journeys = calculator.calculateRouteAsList(start, dest, journeyRequest);
        checkJourneys(start, dest, journeyRequest.getOriginalTime(), journeyRequest.getDate(), journeys);
    }

    @NotNull
    private List<Journey> checkJourneys(TramStations start, TramStations dest, TramTime time, TramDate date, List<Journey> journeys) {
        String message = "from " + start.getId() + " to " + dest.getId() + " at " + time + " on " + date;
        assertFalse(journeys.isEmpty(), "Unable to find journey " + message);
        journeys.forEach(journey -> assertFalse(journey.getStages().isEmpty(), message + " missing stages for journey" + journey));
        journeys.forEach(journey -> {
            List<TransportStage<?,?>> stages = journey.getStages();
            TramTime earliestAtNextStage = null;
            for (TransportStage<?,?> stage : stages) {
                if (earliestAtNextStage!=null) {
                    assertFalse(
                            stage.getFirstDepartureTime().isBefore(earliestAtNextStage), stage + " arrived before " + earliestAtNextStage);
                }
                earliestAtNextStage = stage.getFirstDepartureTime().plusRounded(stage.getDuration());
            }
        });
        return journeys;
    }

    private List<TramTime> checkRangeOfTimes(TramStations start, TramStations dest) {

        // TODO lockdown services after 6.10
        int minsOffset = 10;
        List<TramTime> missing = new LinkedList<>();
        int latestHour = 23;
        for (int hour = 7; hour < latestHour; hour++) {
            for (int minutes = minsOffset; minutes < 59; minutes=minutes+ maxChanges) {
                TramTime time = TramTime.of(hour, minutes);
                JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                        maxJourneyDuration, 1, requestedModes);
                List<Journey> journeys = calculator.calculateRouteAsList(start, dest, journeyRequest);
                if (journeys.isEmpty()) {
                    missing.add(time);
                }
            }
            minsOffset = 0;
        }
        return missing;
    }

}