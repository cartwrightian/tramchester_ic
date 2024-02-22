package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.JourneyConstraints;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static org.junit.jupiter.api.Assertions.*;

public class JourneyConstraintsTest extends EasyMockSupport {

    private JourneyConstraints journeyConstraints;
    private RunningRoutesAndServices.FilterForDate filterForDate;
    private TestConfigWithTramMode config;
    private LowestCostsForDestRoutes lowestCostForDest;
    private Duration maxJourneyDuration;

    @BeforeEach
    void beforeEachTestRuns() {
        config = new TestConfigWithTramMode();

        IdSet<Station> closedStations = IdSet.singleton(TramStations.Cornbrook.getId());

        lowestCostForDest = createMock(LowestCostsForDestRoutes.class);
        filterForDate = createMock(RunningRoutesAndServices.FilterForDate.class);

        LocationSet<Station> endStations = LocationSet.singleton(TramStations.Bury.fake());

        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        journeyConstraints = new JourneyConstraints(config, filterForDate,
                closedStations, endStations, lowestCostForDest, maxJourneyDuration,
                TimeRange.of(TramTime.of(8,0), TramTime.of(23,0)));
    }

    @Test
    void shouldCarryBasicParams() {
        assertEquals(config.getMaxWalkingConnections(), journeyConstraints.getMaxWalkingConnections());
        assertMinutesEquals(config.getMaxJourneyDuration(), journeyConstraints.getMaxJourneyDuration());
    }

    @Test
    void shouldHaveProvidedLowestCostCalc() {
        assertSame(lowestCostForDest, journeyConstraints.getFewestChangesCalculator());
    }

    @Test
    void shouldHaveTimesDestinationsAvailable() {
        // outside of range but not after range
        assertTrue(journeyConstraints.destinationsAvailable(TramTime.of(7,15)));
        // within range
        assertTrue(journeyConstraints.destinationsAvailable(TramTime.of(8,15)));

        // after range
        assertFalse(journeyConstraints.destinationsAvailable(TramTime.of(23,50)));
        assertFalse(journeyConstraints.destinationsAvailable(TramTime.nextDay(1,5)));
    }

    @Test
    void shouldHaveTimesAvailableEarlyMorningFromPreviousDay() {
        TimeRange timeRange = TimeRange.of(TramTime.of(16, 0), TramTime.nextDay(1, 5));

        JourneyConstraints constraints = new JourneyConstraints(config, filterForDate,
                new IdSet<>(), LocationSet.singleton(TramStations.Bury.fake()), lowestCostForDest, maxJourneyDuration,
                timeRange);

        assertTrue(constraints.destinationsAvailable(TramTime.of(16,15)));
        assertTrue(constraints.destinationsAvailable(TramTime.nextDay(0,45)));
        assertTrue(constraints.destinationsAvailable(TramTime.of(0,45))); // not next day

        assertFalse(constraints.destinationsAvailable(TramTime.of(1,10)));
        assertFalse(constraints.destinationsAvailable(TramTime.nextDay(1,10)));
    }

    @Test
    void shouldCheckIfRouteRunning() {
        Route route = TestEnv.getTramTestRoute();
        TramTime time = TramTime.of(10,11);

        EasyMock.expect(filterForDate.isRouteRunning(route.getId(), false)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isUnavailable(route, time);
        verifyAll();

        assertFalse(result);
    }

    @Test
    void shouldCheckIfServiceRunningOnDate() {
        IdFor<Service> serviceId = Service.createId("serviceA");

        TramTime visitTime = TramTime.of(13,56);

        EasyMock.expect(filterForDate.isServiceRunningByDate(serviceId, false)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunningOnDate(serviceId, visitTime);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfServiceRunningOnDateNextDay() {
        IdFor<Service> serviceId = Service.createId("serviceA");

        TramTime visitTime = TramTime.nextDay(13,56);

        EasyMock.expect(filterForDate.isServiceRunningByDate(serviceId, true)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunningOnDate(serviceId, visitTime);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfServiceRunningAtTime() {
        IdFor<Service> serviceId = Service.createId("serviceA");

        TramTime visitTime = TramTime.of(13,56);

        int maxWait = config.getMaxWait();

        EasyMock.expect(filterForDate.isServiceRunningByTime(serviceId, visitTime, maxWait)).andReturn(true);

        replayAll();
        boolean result = journeyConstraints.isRunningAtTime(serviceId, visitTime, maxWait);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldCheckIfClosedStation() {
        Station stationA = TramStations.Anchorage.fake();
        Station stationB = TramStations.Cornbrook.fake();

        replayAll();
        assertFalse(journeyConstraints.isClosed(stationA));
        assertTrue(journeyConstraints.isClosed(stationB));
        verifyAll();
    }

    @Test
    void shouldGetEndStations() {

        LocationCollection result = journeyConstraints.getDestinations();
        assertEquals(1, result.size());
        assertTrue(result.contains(TramStations.Bury.getLocationId()));
    }

    @Test
    void shouldCheckLongestPath() {
        assertEquals(400, journeyConstraints.getMaxPathLength());
    }


    @Test
    void shouldCheckDestinationAvailableAtTime() {

    }

    private static class TestConfigWithTramMode extends TestConfig {
        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }

        @Override
        public EnumSet<TransportMode> getTransportModes() {
            return EnumSet.of(Tram);
        }
    }

}
