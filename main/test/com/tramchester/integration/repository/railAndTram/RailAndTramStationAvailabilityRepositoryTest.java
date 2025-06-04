package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.Modes.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramStationAvailabilityRepositoryTest {
    private static ComponentContainer componentContainer;

    private StationAvailabilityRepository availabilityRepository;
    private StationRepository stationRepository;
    private TramDate when;
    private TimeRange timeRange;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        RailAndTramGreaterManchesterConfig config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);

        when = TestEnv.testDay();
        timeRange = TimeRangePartial.of(TramTime.of(6, 0), TramTime.of(23,0));

    }

    @Test
    void shouldHaveExpectedPickupsForTheLinkedRailStation() {
        Station altrinchamTrain = RailStationIds.Altrincham.from(stationRepository);

        //IdSet<Route> trainPickupsIds = trainPickups.stream().collect(IdSet.collector());

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrinchamTrain, when, timeRange, TrainAndTram);

        long tramRoutes = results.stream().filter(route -> route.getTransportMode()==Tram).count();
        assertEquals(2, tramRoutes, HasId.asIds(results));

        IdSet<Route> trainRouteIds = results.stream().filter(route -> route.getTransportMode() == Train).collect(IdSet.collector());
        assertFalse(trainRouteIds.isEmpty());

        IdSet<Route> trainPickups = altrinchamTrain.getPickupRoutes().stream().collect(IdSet.collector());

        // won't be a 1:1 correspondence due to date and time range filtering...
        assertTrue(trainPickups.containsAll(trainRouteIds));
    }

    @Test
    void shouldHaveExpectedPickupsForTheLinkedTramStationWhenAllModesEnabled() {
        Station altrinchamTram = TramStations.Altrincham.from(stationRepository);

        //IdSet<Route> trainPickupsIds = trainPickups.stream().collect(IdSet.collector());

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrinchamTram, when, timeRange, TrainAndTram);

        long trainRoutes = results.stream().filter(route -> route.getTransportMode()==Train).count();
        assertEquals(4, trainRoutes, HasId.asIds(results));

        IdSet<Route> tramRouteIds = results.stream().filter(route -> route.getTransportMode()==Tram).collect(IdSet.collector());
        assertFalse(tramRouteIds.isEmpty());

        IdSet<Route> tramPickups = altrinchamTram.getPickupRoutes().stream().collect(IdSet.collector());

        // won't be a 1:1 correspondence due to date and time range filtering...
        assertTrue(tramPickups.containsAll(tramRouteIds));
    }

    @Disabled("better to handle this by passing in an extended list of modes in some scenarios")
    @Test
    void shouldHaveExpectedPickupsForTheLinkedTramStationWhenOnlyTrain() {
        Station manPicc = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        Set<Route> results = availabilityRepository.getPickupRoutesFor(manPicc, when, timeRange, TramsOnly);

        long tramRoutes = results.stream().filter(route -> route.getTransportMode()==Tram).count();
        assertNotEquals(0, tramRoutes);

        long trainRoutes = results.stream().filter(route -> route.getTransportMode()==Train).count();
        assertNotEquals(0, trainRoutes);
    }

    @Test
    void shouldRespectPickupAndDropoffForInterchangeStationsWithLinks() {

        Station altrinchamRail = RailStationIds.Altrincham.from(stationRepository);

        // true because a train station
        boolean availableForTrains = availabilityRepository.isAvailable(altrinchamRail, when, timeRange, RailOnly);
        assertTrue(availableForTrains);

        // true because a multi modal station linked to a tram station
        boolean availableForTrams = availabilityRepository.isAvailable(altrinchamRail, when, timeRange, TrainAndTram);
        assertTrue(availableForTrams);

        // should get the linked Tram routes from the Train Station since Alty Tram station is linked

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrinchamRail, when, timeRange, TramsOnly);

        assertEquals(2, results.size(),
                timeRange + " missing routes from " + altrinchamRail.getId() + " got " + HasId.asIds(results));
    }

    @Test
    void shouldHaveConnectedStationPickupRoutesForInterchangeStations() {
        Station manPicc = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        // Direction is rail -> rail OR tram

        Set<Route> results = availabilityRepository.getPickupRoutesFor(manPicc, when, timeRange, TrainAndTram);

        long tramRoutes = results.stream().filter(route -> route.getTransportMode().equals(Tram)).count();
        assertNotEquals(0, tramRoutes, "no tram in " + HasId.asIds(results));

        long trainRoutes = results.stream().filter(route -> route.getTransportMode().equals(Train)).count();
        assertNotEquals(0, trainRoutes, "no train in " + HasId.asIds(results));
    }

    @Test
    void shouldHaveConnectedStationDropoffRoutesForInterchangeStations() {

        // Direction is rail -> rail OR tram

        Station manPicc = RailStationIds.ManchesterPiccadilly.from(stationRepository);

        Set<Route> dropOffs = availabilityRepository.getDropoffRoutesFor(manPicc, when, timeRange, TrainAndTram);

        long tramDropoffs = dropOffs.stream().filter(route -> route.getTransportMode().equals(Tram)).count();
        assertEquals(3, tramDropoffs, "wrong number tram in " + HasId.asIds(dropOffs));

        long trainDropoffs = dropOffs.stream().filter(route -> route.getTransportMode().equals(Train)).count();
        assertNotEquals(0, trainDropoffs, "no train in " + HasId.asIds(dropOffs));
    }


}
