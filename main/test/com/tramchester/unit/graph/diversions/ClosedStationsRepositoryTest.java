package com.tramchester.unit.graph.diversions;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.closures.ClosedStationFactory;
import com.tramchester.domain.closures.Closure;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.filters.IncludeAllFilter;
import com.tramchester.integration.testSupport.config.StationClosuresConfigForTest;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClosedStationsRepositoryTest extends EasyMockSupport {


    private ClosedStationsRepository closeStationRepository;
    private TramDate when;
    private DateRange dateRange;
    private ClosedStationFactory closeStationFactory;
    private GraphFilter graphFilter;

    @BeforeEach
    void onceBeforeEachTestRuns() {

        closeStationFactory = createMock(ClosedStationFactory.class);

        TramStations shudehill = Shudehill;
        TramStations bury = Bury;

        TimeRange buryTimeRange = TimeRange.of(TramTime.of(15, 14), TramTime.of(19, 35));

        when = TestEnv.testDay();
        dateRange = DateRange.of(when, when.plusWeeks(1));

        boolean fullyClosed = true;

        StationClosures shudehillConfig = new StationClosuresConfigForTest(shudehill, dateRange, fullyClosed);
        StationClosures buryConfig = new StationClosuresConfigForTest(bury, dateRange, fullyClosed);

        EasyMock.expect(closeStationFactory.createFor(shudehillConfig)).
                andReturn(new Closure(new DateTimeRange(dateRange, TimeRange.AllDay()), Collections.singleton(Altrincham.fake()), fullyClosed));
        EasyMock.expect(closeStationFactory.createFor(buryConfig)).
                andReturn(new Closure(new DateTimeRange(dateRange, buryTimeRange), Collections.singleton(Altrincham.fake()), fullyClosed));

        ClosedStation shudehillClosed = new ClosedStation(shudehill.fake(), dateRange, TimeRange.AllDay(), Collections.emptySet(), Collections.emptySet());
        ClosedStation buryClosed = new ClosedStation(bury.fake(), dateRange, buryTimeRange, Collections.emptySet(),
                Collections.emptySet());

        EasyMock.expect(closeStationFactory.createClosedStation(EasyMock.eq(shudehillConfig), EasyMock.eq(shudehill.getId()), EasyMock.isA(ClosedStationFactory.ShouldIncludeStationInDiversions.class))).
                andReturn(shudehillClosed);
        EasyMock.expect(closeStationFactory.createClosedStation(EasyMock.eq(buryConfig), EasyMock.eq(bury.getId()), EasyMock.isA(ClosedStationFactory.ShouldIncludeStationInDiversions.class))).
                andReturn(buryClosed);

        TramchesterConfig config = new ConfigWithClosure(Arrays.asList(shudehillConfig, buryConfig));
        graphFilter = new IncludeAllFilter();
        closeStationRepository = new ClosedStationsRepository(config, closeStationFactory, graphFilter);

    }

    @Test
    void shouldGuardAgainstOverlappingClosures() {

        DateRange dateRangeA = DateRange.of(when, when.plusWeeks(1));
        DateRange dateRangeB = DateRange.of(when.plusDays(4), when.plusWeeks(1));

        StationClosures closedA = new StationClosuresConfigForTest(Altrincham, dateRangeA, true);
        StationClosures closedB = new StationClosuresConfigForTest(Altrincham, dateRangeB, true);
        TramchesterConfig config = new ConfigWithClosure(Arrays.asList(closedA, closedB));

        EasyMock.expect(closeStationFactory.createFor(closedA)).
                andReturn(new Closure(new DateTimeRange(dateRangeA, TimeRange.AllDay()), Collections.singleton(Altrincham.fake()), true));
        EasyMock.expect(closeStationFactory.createFor(closedB)).
                andReturn(new Closure(new DateTimeRange(dateRangeB, TimeRange.AllDay()), Collections.singleton(Altrincham.fake()), true));

        EasyMock.expect(closeStationFactory.createClosedStation(EasyMock.eq(closedA), EasyMock.eq(Altrincham.getId()), EasyMock.isA(ClosedStationFactory.ShouldIncludeStationInDiversions.class))).
                andReturn(new ClosedStation(Altrincham.fake(), dateRangeA, Collections.emptySet(), Collections.emptySet()));
        EasyMock.expect(closeStationFactory.createClosedStation(EasyMock.eq(closedB), EasyMock.eq(Altrincham.getId()), EasyMock.isA(ClosedStationFactory.ShouldIncludeStationInDiversions.class))).
                andReturn(new ClosedStation(Altrincham.fake(), dateRangeB, Collections.emptySet(), Collections.emptySet()));

        replayAll();
        assertThrows(RuntimeException.class, () -> {
            ClosedStationsRepository anotherRepository = new ClosedStationsRepository(config, closeStationFactory, graphFilter);
            anotherRepository.start();
        });
//        verifyAll();
    }

    @Test
    void shouldHaveExpectedClosures() {
        replayAll();
        closeStationRepository.start();
        assertTrue(closeStationRepository.isStationClosed(TramStations.Shudehill.getId(), when.plusDays(1)));
        assertFalse(closeStationRepository.isStationClosed(TramStations.Shudehill.getId(), when.plusWeeks(1).plusDays(1)));
        verifyAll();
    }

    @Test
    void shouldHaveClosedStations() {
        replayAll();
        closeStationRepository.start();
        assertTrue(closeStationRepository.hasClosuresOn(when));
        assertFalse(closeStationRepository.hasClosuresOn(when.minusDays(1)));
        verifyAll();
    }

    @Test
    void shouldHaveAnyOpenStations() {
        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Bury.fake());
        assertTrue(closeStationRepository.anyStationOpen(locations, when));
        verifyAll();
    }

    @Test
    void shouldNotHaveAnyOpenStations() {
        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Shudehill.fake());
        assertFalse(closeStationRepository.anyStationOpen(locations, when));
        verifyAll();
    }

    @Test
    void shouldHaveAnyOpenStationsAfterClosureDates() {
        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Shudehill.fake());
        assertTrue(closeStationRepository.anyStationOpen(locations, when.plusWeeks(4)));
        verifyAll();
    }

    @Test
    void shouldHaveAnyOpenStationsMultipleLocations() {
        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Shudehill.fake());
        locations.add(StPetersSquare.fake());
        assertTrue(closeStationRepository.anyStationOpen(locations, when));
        verifyAll();
    }

//    @Test
//    void shouldHaveClosedStationOnDateAndTime() {
//        replayAll();
//        closeStationRepository.start();
//        ClosedStation closed = closeStationRepository.getClosedStation(Shudehill.fake(), when, TramTime.of(17,45));
//        assertEquals(Shudehill.fake(), closed.getStation());
//        dateRange.stream().forEach(date -> {
//            assertTrue(closed.getDateTimeRange().contains(date));
//        });
//        verifyAll();
//    }

    @Test
    void shouldHaveClosedStationOnDatAndTimeRangeMatch() {
        Station bury = Bury.fake(); // closed part of day
        TimeRange timeRange = TimeRange.of(TramTime.of(17,15), TramTime.of(17,45));

        replayAll();
        closeStationRepository.start();
        boolean isClosed = closeStationRepository.isClosed(bury, when, timeRange);
        assertTrue(isClosed);

        ClosedStation closed = closeStationRepository.getClosedStation(bury, when, timeRange);
        assertNotNull(closed);
        assertEquals(bury.getId(), closed.getStationId());
        dateRange.stream().forEach(date -> {
            assertTrue(closed.getDateTimeRange().contains(date));
        });
        verifyAll();
    }

    @Test
    void shouldHaveClosedStationOnDatAndTimeRangeMatchAllDay() {

        Station station = Shudehill.fake(); // closed all day
        TimeRange timeRange = TimeRange.of(TramTime.of(17,15), TramTime.of(17,45));

        replayAll();
        closeStationRepository.start();
        boolean isClosed = closeStationRepository.isClosed(station, when, timeRange);
        assertTrue(isClosed);

        ClosedStation closed = closeStationRepository.getClosedStation(station, when, timeRange);
        assertNotNull(closed);
        assertEquals(station.getId(), closed.getStationId());
        dateRange.stream().forEach(date -> {
            assertTrue(closed.getDateTimeRange().contains(date));
        });
        verifyAll();
    }

    @Test
    void shouldHaveClosedStationOnDateAndTimeRangeNoMatch() {

        Station bury = Bury.fake(); // closed part of day
        TimeRange timeRange = TimeRange.of(TramTime.of(8,15), TramTime.of(17,45));

        replayAll();
        closeStationRepository.start();
        boolean isClosed = closeStationRepository.isClosed(bury, when, timeRange);
        assertFalse(isClosed);

        verifyAll();
    }

//    @Test
//    void shouldHaveClosedStationsForDates() {
//        replayAll();
//        closeStationRepository.start();
//        Set<ClosedStation> closed = closeStationRepository.getFullyClosedStationsFor(when, TramTime.of(13,11));
//        assertEquals(1, closed.size());
//        assertTrue(closed.stream().anyMatch(closedStation -> closedStation.getStationId().equals(Shudehill.getId())));
//        verifyAll();
//    }
//
//    @Test
//    void shouldHaveNoClosedStations() {
//        replayAll();
//        closeStationRepository.start();
//        Set<ClosedStation> closed = closeStationRepository.getFullyClosedStationsFor(when.plusWeeks(8), TramTime.of(14,56));
//        assertTrue(closed.isEmpty());
//        verifyAll();
//    }

    @Test
    void shouldHaveClosedByDataSourceId() {
        replayAll();
        closeStationRepository.start();
        Set<ClosedStation> closedStations = closeStationRepository.getClosedStationsFor(DataSourceID.tfgm);
        assertEquals(2, closedStations.size());
        verifyAll();
    }

    private static class ConfigWithClosure extends TestConfig {

        public static final Duration MAX_INITIAL_WAIT = Duration.ofMinutes(13);

        private final TFGMGTFSSourceTestConfig gtfsSourceConfig;

        public ConfigWithClosure(StationClosures closed) {
            final List<StationClosures> closedStations = List.of(closed);
            gtfsSourceConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                    TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations,
                    MAX_INITIAL_WAIT, Collections.emptyList());
        }

        public ConfigWithClosure(final List<StationClosures> closedStations) {
            gtfsSourceConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
                    TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations,
                    MAX_INITIAL_WAIT, Collections.emptyList());
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return List.of(gtfsSourceConfig);
        }
    }


}
