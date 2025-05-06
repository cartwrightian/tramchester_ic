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
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.filters.IncludeAllFilter;
import com.tramchester.integration.testSupport.config.closures.StationClosuresListForTest;
import com.tramchester.integration.testSupport.config.closures.StationClosuresPairForTest;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.lang3.tuple.Pair;
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

    private TramDate when;
    private DateRange dateRange;
    private ClosedStationFactory closeStationFactory;
    private StationRepository stationRepository;
    private StopCallRepository stopCallRepository;
    private GraphFilter graphFilter;

    @BeforeEach
    void onceBeforeEachTestRuns() {

        closeStationFactory = createMock(ClosedStationFactory.class);
        stationRepository = createMock(StationRepository.class);
        stopCallRepository = createMock(StopCallRepository.class);

        when = TestEnv.testDay();
        dateRange = DateRange.of(when, when.plusWeeks(1));

        graphFilter = new IncludeAllFilter();
    }

    private TramchesterConfig createSimpleConfig() {
        boolean fullyClosed = true;

        TimeRange buryTimeRange = TimeRange.of(TramTime.of(15, 14), TramTime.of(19, 35));

        StationClosures buryConfig = createConfigAndExpectations(Bury, fullyClosed, buryTimeRange);
        StationClosures shudehillConfig = createConfigAndExpectations(Shudehill, fullyClosed, TimeRange.AllDay());

        return new ConfigWithClosure(Arrays.asList(shudehillConfig, buryConfig));
    }

    private StationClosures createConfigAndExpectations(TramStations tramStation, boolean fullyClosed, TimeRange timerange) {
        Station fakeStation = tramStation.fake();
        IdFor<Station> stationId = tramStation.getId();

        StationClosures shudehillConfig = new StationClosuresListForTest(tramStation, dateRange, fullyClosed);

        ClosedStation closedStation = new ClosedStation(fakeStation, dateRange, timerange,
                Collections.emptySet(), Collections.emptySet());

        EasyMock.expect(stationRepository.getStationById(stationId)).andReturn(fakeStation);

        EasyMock.expect(closeStationFactory.createFor(shudehillConfig, IdSet.singleton(stationId))).
                andReturn(new Closure(new DateTimeRange(dateRange, timerange), Collections.singleton(fakeStation), fullyClosed));

        EasyMock.expect(closeStationFactory.createClosedStation(EasyMock.eq(shudehillConfig), EasyMock.eq(stationId),
                        EasyMock.isA(ClosedStationFactory.ShouldIncludeStationInDiversions.class))).
                andReturn(closedStation);

        return shudehillConfig;
    }

    @Test
    void shouldGuardAgainstOverlappingClosures() {

        DateRange dateRangeA = DateRange.of(when, when.plusWeeks(1));
        DateRange dateRangeB = DateRange.of(when.plusDays(4), when.plusWeeks(1));

        StationClosures closedA = new StationClosuresListForTest(Altrincham, dateRangeA, true);
        StationClosures closedB = new StationClosuresListForTest(Altrincham, dateRangeB, true);
        TramchesterConfig config = new ConfigWithClosure(Arrays.asList(closedA, closedB));

        EasyMock.expect(closeStationFactory.createFor(closedA, IdSet.singleton(Altrincham.getId()))).
                andReturn(new Closure(new DateTimeRange(dateRangeA, TimeRange.AllDay()), Collections.singleton(Altrincham.fake()), true));
        EasyMock.expect(closeStationFactory.createFor(closedB, IdSet.singleton(Altrincham.getId()))).
                andReturn(new Closure(new DateTimeRange(dateRangeB, TimeRange.AllDay()), Collections.singleton(Altrincham.fake()), true));

        replayAll();
        assertThrows(RuntimeException.class, () -> {
            ClosedStationsRepository closedStationsRepository = new ClosedStationsRepository(config, closeStationFactory, stationRepository,
                    stopCallRepository, graphFilter);
            closedStationsRepository.start();
        });
        verifyAll();
    }

    @Test
    void shouldHavePairBasedClosureConfig() {

        DateRange dateRangeA = DateRange.of(when, when.plusWeeks(1));

        Pair<TramStations, TramStations> pair = Pair.of(Cornbrook, StPetersSquare);

        StationClosuresPairForTest closedConfig = new StationClosuresPairForTest(pair, dateRangeA, true,
                Collections.emptySet(), Collections.emptySet());

        TramchesterConfig config = new ConfigWithClosure(List.of(closedConfig));

        List<IdFor<Station>> idsBetween = Arrays.asList(Cornbrook.getId(), Deansgate.getId(), StPetersSquare.getId());

        // should be one call, change the creation order?
        EasyMock.expect(stopCallRepository.getStopcallsBetween(Cornbrook.getId(), StPetersSquare.getId(), dateRangeA)).
                andReturn(idsBetween);
        EasyMock.expect(stopCallRepository.getStopcallsBetween(Cornbrook.getId(), StPetersSquare.getId(), dateRangeA)).
                andReturn(idsBetween);

        EasyMock.expect(stationRepository.getStationById(Deansgate.getId())).andReturn(Deansgate.fake());
        EasyMock.expect(stationRepository.getStationById(StPetersSquare.getId())).andReturn(StPetersSquare.fake());

        Set<Station> stations = Collections.singleton(Altrincham.fake());

        IdSet<Station> closedStationIds = IdSet.wrap(Set.of(Deansgate.getId(), Cornbrook.getId(), StPetersSquare.getId()));
        EasyMock.expect(closeStationFactory.createFor(closedConfig, closedStationIds)).
                andReturn(new Closure(new DateTimeRange(dateRangeA, TimeRange.AllDay()),
                        stations, true));

        createExpectionsFor(closedConfig, Cornbrook);
        createExpectionsFor(closedConfig, Deansgate);
        createExpectionsFor(closedConfig, StPetersSquare);

        replayAll();
        ClosedStationsRepository closedStationsRepository = new ClosedStationsRepository(config, closeStationFactory, stationRepository,
                stopCallRepository, graphFilter);
        closedStationsRepository.start();
        verifyAll();
    }

    private void createExpectionsFor(StationClosuresPairForTest closedConfig, TramStations tramStations) {
        EasyMock.expect(stationRepository.getStationById(tramStations.getId())).andStubReturn(tramStations.fake());

        ClosedStation closedStation = new ClosedStation(tramStations.fake(), dateRange, TimeRange.AllDay(),
                Collections.emptySet(), Collections.emptySet());

        EasyMock.expect(closeStationFactory.createClosedStation(EasyMock.eq(closedConfig), EasyMock.eq(tramStations.getId()),
                        EasyMock.isA(ClosedStationFactory.ShouldIncludeStationInDiversions.class))).
                andReturn(closedStation);
    }

    @Test
    void shouldHaveExpectedClosures() {
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

        replayAll();
        closeStationRepository.start();
        assertTrue(closeStationRepository.isStationClosed(TramStations.Shudehill.getId(), when.plusDays(1)));
        assertFalse(closeStationRepository.isStationClosed(TramStations.Shudehill.getId(), when.plusWeeks(1).plusDays(1)));
        verifyAll();
    }

    @Test
    void shouldHaveClosedStations() {
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

        replayAll();
        closeStationRepository.start();
        assertTrue(closeStationRepository.hasClosuresOn(when));
        assertFalse(closeStationRepository.hasClosuresOn(when.minusDays(1)));
        verifyAll();
    }

    @Test
    void shouldHaveAnyOpenStations() {
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Bury.fake());
        assertTrue(closeStationRepository.anyStationOpen(locations, when));
        verifyAll();
    }

    @Test
    void shouldNotHaveAnyOpenStations() {
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Shudehill.fake());
        assertFalse(closeStationRepository.anyStationOpen(locations, when));
        verifyAll();
    }

    @Test
    void shouldHaveAnyOpenStationsAfterClosureDates() {
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

        replayAll();
        closeStationRepository.start();
        LocationSet<Station> locations = new LocationSet<>();
        locations.add(Shudehill.fake());
        assertTrue(closeStationRepository.anyStationOpen(locations, when.plusWeeks(4)));
        verifyAll();
    }

    @Test
    void shouldHaveAnyOpenStationsMultipleLocations() {
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

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
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

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
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

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
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

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
        TramchesterConfig config = createSimpleConfig();

        ClosedStationsRepository closeStationRepository = new ClosedStationsRepository(config, closeStationFactory,
                stationRepository, stopCallRepository, graphFilter);

        replayAll();
        closeStationRepository.start();
        Set<ClosedStation> closedStations = closeStationRepository.getClosedStationsFor(DataSourceID.tfgm);
        assertEquals(2, closedStations.size());
        verifyAll();
    }

    private static class ConfigWithClosure extends TestConfig {

        public static final Duration MAX_INITIAL_WAIT = Duration.ofMinutes(13);

        private final TFGMGTFSSourceTestConfig gtfsSourceConfig;

//        public ConfigWithClosure(StationClosures closed) {
//            final List<StationClosures> closedStations = List.of(closed);
//            gtfsSourceConfig = new TFGMGTFSSourceTestConfig(GTFSTransportationType.tram,
//                    TransportMode.Tram, AdditionalTramInterchanges.stations(), Collections.emptySet(), closedStations,
//                    MAX_INITIAL_WAIT, Collections.emptyList());
//        }

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
