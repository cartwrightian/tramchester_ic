package com.tramchester.unit.graph;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
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
import java.util.Set;

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

        TramDate when = TestEnv.testDay();
        DateRange dateRange = DateRange.of(when.plusWeeks(1), when.plusWeeks(2));
        TimeRange timeRange = TimeRange.AllDay();

        ClosedStation closedStation = new ClosedStation(TramStations.Shudehill.fake(), dateRange, timeRange, Collections.emptySet(), Collections.emptySet());
        Set<ClosedStation> closedStations = Collections.singleton(closedStation);

        lowestCostForDest = createMock(LowestCostsForDestRoutes.class);
        filterForDate = createMock(RunningRoutesAndServices.FilterForDate.class);

        //LocationSet<Station> endStations = LocationSet.singleton(TramStations.Bury.fake());

        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        journeyConstraints = new JourneyConstraints(config, filterForDate,
                closedStations, TestEnv.Modes.TramsOnly, lowestCostForDest, maxJourneyDuration,
                TimeRangePartial.of(TramTime.of(8,0), TramTime.of(23,0)));
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
        TimeRange timeRange = TimeRangePartial.of(TramTime.of(16, 0), TramTime.nextDay(1, 5));

        JourneyConstraints constraints = new JourneyConstraints(config, filterForDate,
                Collections.emptySet(), TestEnv.Modes.TramsOnly, lowestCostForDest, maxJourneyDuration,
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

        replayAll();
        assertFalse(journeyConstraints.isClosed(TramStations.Anchorage.getId()));
        assertTrue(journeyConstraints.isClosed(TramStations.Shudehill.getId()));
        verifyAll();
    }

//    @Test
//    void shouldGetEndStations() {
//
//        LocationCollection result = journeyConstraints.getDestinations();
//        assertEquals(1, result.size());
//        assertTrue(result.contains(TramStations.Bury.getLocationId()));
//    }

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
