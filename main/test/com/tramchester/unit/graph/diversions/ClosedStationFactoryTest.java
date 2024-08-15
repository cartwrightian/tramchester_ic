package com.tramchester.unit.graph.diversions;

import com.tramchester.config.AppConfiguration;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.closures.ClosedStationFactory;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.config.StationClosuresConfigForTest;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClosedStationFactoryTest extends EasyMockSupport {

    private ClosedStationFactory closedStationFactory;
    private StationRepository stationRepository;
    private StationLocations stationLocations;
    private AppConfiguration config;

    @BeforeEach
    void onceBeforeEachTestRuns() {

        config = TestEnv.GET();
        stationRepository = createMock(StationRepository.class);
        stationLocations = createMock(StationLocations.class);

        closedStationFactory = new ClosedStationFactory(config, stationRepository, stationLocations);

    }

    @Test
    void shouldCreateClosedStationFromNearby() {
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusWeeks(1));
        StationClosures closures = new StationClosuresConfigForTest(Bury, dateRange, true);

        ClosedStationFactory.ShouldIncludeStationInDiversions include = diversionStation -> true;

        EasyMock.expect(stationRepository.getStationById(Bury.getId())).andReturn(Bury.fake());

        Set<Station> nearby = new HashSet<>(Arrays.asList(Altrincham.fake(), StPetersSquare.fake()));

        EasyMock.expect(stationLocations.nearestStationsUnsorted(Bury.fake(), config.getWalkingDistanceRange())).andReturn(nearby.stream());

        replayAll();
        ClosedStation created = closedStationFactory.createClosedStation(closures, Bury.getId(), include);
        verifyAll();

        DateTimeRange expected = DateTimeRange.of(dateRange, TimeRange.AllDay());


        assertEquals(Bury.getId(), created.getStationId());
        assertEquals(expected, created.getDateTimeRange());
        assertTrue(created.closedWholeDay());

        assertEquals(nearby, created.getDiversionAroundClosure());
        assertEquals(nearby, created.getDiversionToFromClosure());
    }

    @Test
    void shouldCreateClosedWithTimeRange() {

        TimeRange timeRange = TimeRangePartial.of(TramTime.of(9,13), TramTime.of(10,55));

        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusWeeks(1));
        StationClosuresConfigForTest closures = new StationClosuresConfigForTest(Bury, dateRange, true);
        closures.setTimeRange(timeRange);

        ClosedStationFactory.ShouldIncludeStationInDiversions include = diversionStation -> true;

        EasyMock.expect(stationRepository.getStationById(Bury.getId())).andReturn(Bury.fake());

        Set<Station> nearby = new HashSet<>(Arrays.asList(Altrincham.fake(), StPetersSquare.fake()));

        EasyMock.expect(stationLocations.nearestStationsUnsorted(Bury.fake(), config.getWalkingDistanceRange())).andReturn(nearby.stream());

        replayAll();
        ClosedStation created = closedStationFactory.createClosedStation(closures, Bury.getId(), include);
        verifyAll();

        DateTimeRange expected = DateTimeRange.of(dateRange, timeRange);
        assertEquals(expected, created.getDateTimeRange());

    }

    @Test
    void shouldCreateClosedStationFromNearbySomeExluded() {
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusWeeks(1));
        StationClosures closures = new StationClosuresConfigForTest(Bury, dateRange, true);

        ClosedStationFactory.ShouldIncludeStationInDiversions include = diversionStation -> diversionStation.getId().equals(StPetersSquare.getId());

        EasyMock.expect(stationRepository.getStationById(Bury.getId())).andReturn(Bury.fake());

        Set<Station> nearby = new HashSet<>(Arrays.asList(Altrincham.fake(), StPetersSquare.fake()));

        EasyMock.expect(stationLocations.nearestStationsUnsorted(Bury.fake(), config.getWalkingDistanceRange())).andReturn(nearby.stream());

        replayAll();
        ClosedStation created = closedStationFactory.createClosedStation(closures, Bury.getId(), include);
        verifyAll();

        DateTimeRange expected = DateTimeRange.of(dateRange, TimeRange.AllDay());

        assertEquals(Bury.getId(), created.getStationId());
        assertEquals(expected, created.getDateTimeRange());

        assertEquals(Collections.singleton(StPetersSquare.fake()), created.getDiversionAroundClosure());
        assertEquals(Collections.singleton(StPetersSquare.fake()), created.getDiversionToFromClosure());
    }

    @Test
    void shouldCreateClosedStationFromProvided() {
        DateRange dateRange = DateRange.of(TestEnv.testDay(), TestEnv.testDay().plusWeeks(1));
        Set<TramStations> diversionsAround = Collections.singleton(NavigationRoad);
        Set<TramStations> diversionsFrom = Collections.singleton(ManAirport);
        StationClosures closures = new StationClosuresConfigForTest(Bury, dateRange, true, diversionsAround, diversionsFrom);

        ClosedStationFactory.ShouldIncludeStationInDiversions include = diversionStation -> true;

        EasyMock.expect(stationRepository.getStationById(Bury.getId())).andReturn(Bury.fake());
        EasyMock.expect(stationRepository.getStationById(NavigationRoad.getId())).andReturn(NavigationRoad.fake());
        EasyMock.expect(stationRepository.getStationById(ManAirport.getId())).andReturn(ManAirport.fake());

        Set<Station> nearby = new HashSet<>(Arrays.asList(Altrincham.fake(), StPetersSquare.fake()));

        EasyMock.expect(stationLocations.nearestStationsUnsorted(Bury.fake(), config.getWalkingDistanceRange())).andReturn(nearby.stream());

        replayAll();
        ClosedStation created = closedStationFactory.createClosedStation(closures, Bury.getId(), include);
        verifyAll();

        assertEquals(Bury.getId(), created.getStationId());

        DateTimeRange expected = DateTimeRange.of(dateRange, TimeRange.AllDay());

        assertEquals(expected, created.getDateTimeRange());

        assertEquals(Collections.singleton(NavigationRoad.fake()), created.getDiversionAroundClosure());
        assertEquals(Collections.singleton(ManAirport.fake()), created.getDiversionToFromClosure());
    }
}
