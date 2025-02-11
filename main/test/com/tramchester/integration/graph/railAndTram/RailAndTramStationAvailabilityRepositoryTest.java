package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.config.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();
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
    void shouldHaveExpectedInterchangeForTramStation() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        Station altrinchamTram = TramStations.Altrincham.from(stationRepository);
        Station altrinchamTrain = RailStationIds.Altrincham.from(stationRepository);

        InterchangeStation interchangeTram = interchangeRepository.getInterchange(altrinchamTram);

        assertEquals(interchangeTram.getDropoffRoutes(), altrinchamTram.getDropoffRoutes());

        Set<Route> trainPickups = altrinchamTrain.getPickupRoutes();
        Set<Route> tramPickups = altrinchamTram.getPickupRoutes();

        int expectedNumber = trainPickups.size() + tramPickups.size();

        Set<Route> interchangePickups = interchangeTram.getPickupRoutes();
        assertEquals(expectedNumber, interchangePickups.size());

        assertTrue(interchangePickups.containsAll(tramPickups));
        assertTrue(interchangePickups.containsAll(trainPickups));
    }

    @Test
    void shouldHaveExpectedInterchangeForTrainStation() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        Station altrinchamTram = TramStations.Altrincham.from(stationRepository);
        Station altrinchamTrain = RailStationIds.Altrincham.from(stationRepository);

        InterchangeStation interchangeTrain = interchangeRepository.getInterchange(altrinchamTrain);

        assertEquals(interchangeTrain.getDropoffRoutes(), altrinchamTrain.getDropoffRoutes());

        Set<Route> trainPickups = altrinchamTrain.getPickupRoutes();
        Set<Route> tramPickups = altrinchamTram.getPickupRoutes();

        int expectedNumber = trainPickups.size() + tramPickups.size();

        Set<Route> interchangePickups = interchangeTrain.getPickupRoutes();
        assertEquals(expectedNumber, interchangePickups.size());

        assertTrue(interchangePickups.containsAll(tramPickups));
        assertTrue(interchangePickups.containsAll(trainPickups));
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
    void shouldHaveExpectedPickupsForTheLinkedTramStation() {
        Station altrinchamTram = TramStations.Altrincham.from(stationRepository);

        //IdSet<Route> trainPickupsIds = trainPickups.stream().collect(IdSet.collector());

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrinchamTram, when, timeRange, TrainAndTram);

        long trainRoutes = results.stream().filter(route -> route.getTransportMode()==Train).count();
        assertEquals(5, trainRoutes, HasId.asIds(results));

        IdSet<Route> tramRouteIds = results.stream().filter(route -> route.getTransportMode()==Tram).collect(IdSet.collector());
        assertFalse(tramRouteIds.isEmpty());

        IdSet<Route> tramPickups = altrinchamTram.getPickupRoutes().stream().collect(IdSet.collector());

        // won't be a 1:1 correspondence due to date and time range filtering...
        assertTrue(tramPickups.containsAll(tramRouteIds));
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




}
