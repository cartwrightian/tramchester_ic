package com.tramchester.unit.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.config.StationClosuresConfig;
import com.tramchester.config.StationListConfig;
import com.tramchester.config.StationPairConfig;
import com.tramchester.config.StationsConfig;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StationClosuresConfigTest {

    private ObjectMapper mapper;

    // NOTE: does not use StandaloneConfigLoader so not entirely realistic

    @BeforeEach
    void onceBeforeEachTestRuns() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldParseYamlWithDiversionsAround() throws JsonProcessingException {

        String yaml = """
                stations:
                   ids: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
                dateRange:
                   begin: 2023-07-15
                   end: 2023-09-20
                fullyClosed: true
                diversionsAroundClosure: [ "9400ZZMAVIC" ]""";

        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        StationsConfig stationConfig = result.getStations();
        assertInstanceOf(StationListConfig.class, stationConfig);
        IdSet<Station> stations = ((StationListConfig)stationConfig).getStations();

        assertEquals(3, stations.size());
        assertTrue(stations.contains(TramStations.Eccles.getId()));
        assertTrue(stations.contains(TramStations.Ladywell.getId()));
        assertTrue(stations.contains(TramStations.Weaste.getId()));

        DateRange dateRange = result.getDateRange();
        assertEquals(TramDate.of(2023,7,15), dateRange.getStartDate());
        assertEquals(TramDate.of(2023,9,20), dateRange.getEndDate());

        assertTrue(result.hasDiversionsAroundClosure());
        assertEquals(IdSet.singleton(TramStations.Victoria.getId()), result.getDiversionsAroundClosure());

        assertFalse(result.hasDiversionsToFromClosure());

        assertTrue(result.isFullyClosed());

    }

    @Test
    void shouldParseYamlWithDiversionsAroundAndToFrom() throws JsonProcessingException {

        String yaml = """
                stations:
                    ids: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
                dateRange:
                   begin: 2023-07-15
                   end: 2023-09-20
                fullyClosed: true
                diversionsToFromClosure: [ "9400ZZMAMKT" ]
                diversionsAroundClosure: [ "9400ZZMAVIC" ]""";


        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        StationsConfig stationConfig = result.getStations();
        assertInstanceOf(StationListConfig.class, stationConfig);
        IdSet<Station> stations = ((StationListConfig)stationConfig).getStations();

        assertEquals(3, stations.size());
        assertTrue(stations.contains(TramStations.Eccles.getId()));
        assertTrue(stations.contains(TramStations.Ladywell.getId()));
        assertTrue(stations.contains(TramStations.Weaste.getId()));

        DateRange dateRange = result.getDateRange();
        assertEquals(TramDate.of(2023,7,15), dateRange.getStartDate());
        assertEquals(TramDate.of(2023,9,20), dateRange.getEndDate());

        assertTrue(result.hasDiversionsAroundClosure());
        assertEquals(IdSet.singleton(TramStations.Victoria.getId()), result.getDiversionsAroundClosure());

        assertTrue(result.hasDiversionsToFromClosure());
        assertEquals(IdSet.singleton(TramStations.MarketStreet.getId()), result.getDiversionsToFromClosure());

        assertTrue(result.isFullyClosed());

    }

    @Test
    void shouldParseYamlWithNoDiversionsGiven() throws JsonProcessingException {

        String yaml = """
                stations:
                   ids: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
                dateRange:
                   begin: 2023-07-15
                   end: 2023-09-20
                fullyClosed: true""";

        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        StationsConfig stationConfig = result.getStations();
        assertInstanceOf(StationListConfig.class, stationConfig);
        IdSet<Station> stations = ((StationListConfig)stationConfig).getStations();

        assertEquals(3, stations.size());
        assertTrue(stations.contains(TramStations.Eccles.getId()));
        assertTrue(stations.contains(TramStations.Ladywell.getId()));
        assertTrue(stations.contains(TramStations.Weaste.getId()));

        DateRange dateRange = result.getDateRange();
        assertEquals(TramDate.of(2023,7,15), dateRange.getStartDate());
        assertEquals(TramDate.of(2023,9,20), dateRange.getEndDate());

        assertFalse(result.hasTimeRange());

        assertFalse(result.hasDiversionsAroundClosure());
        assertFalse(result.hasDiversionsToFromClosure());

        assertTrue(result.isFullyClosed());

    }

    @Test
    void shouldParseYamlWithStationRangeNoDiversionsGiven() throws JsonProcessingException {

        String yaml = """
                stations:
                   first: "9400ZZMAALT"
                   second: "9400ZZMATIM"
                dateRange:
                   begin: 2023-07-15
                   end: 2023-09-20
                fullyClosed: true""";

        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        StationsConfig stationConfig = result.getStations();
        assertInstanceOf(StationPairConfig.class, stationConfig);
        StationIdPair stations = ((StationPairConfig) stationConfig).getStationPair();

        assertEquals(stations.getBeginId(), TramStations.Altrincham.getId());
        assertEquals(stations.getEndId(), TramStations.Timperley.getId());

        DateRange dateRange = result.getDateRange();
        assertEquals(TramDate.of(2023,7,15), dateRange.getStartDate());
        assertEquals(TramDate.of(2023,9,20), dateRange.getEndDate());

        assertFalse(result.hasTimeRange());

        assertFalse(result.hasDiversionsAroundClosure());
        assertFalse(result.hasDiversionsToFromClosure());

        assertTrue(result.isFullyClosed());

    }

    @Test
    void shouldParseYamlWithOptionalTimeRangeForClosure() throws JsonProcessingException {

        String yaml = """
                stations:
                    ids: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
                dateRange:
                   begin: 2023-07-15
                   end: 2023-09-20
                timeRange:
                   begin: 00:01
                   end: 10:45
                fullyClosed: true""";

        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        StationsConfig stationConfig = result.getStations();
        assertInstanceOf(StationListConfig.class, stationConfig);
        IdSet<Station> stations = ((StationListConfig)stationConfig).getStations();

        assertEquals(3, stations.size());
        assertTrue(stations.contains(TramStations.Eccles.getId()));
        assertTrue(stations.contains(TramStations.Ladywell.getId()));
        assertTrue(stations.contains(TramStations.Weaste.getId()));

        DateRange dateRange = result.getDateRange();
        assertEquals(TramDate.of(2023,7,15), dateRange.getStartDate());
        assertEquals(TramDate.of(2023,9,20), dateRange.getEndDate());

        assertTrue(result.hasTimeRange());
        TimeRange timeRange = result.getTimeRange();
        assertEquals(TramTime.of(0,1), timeRange.getStart());
        assertEquals(TramTime.of(10,45), timeRange.getEnd());

        assertFalse(result.hasDiversionsAroundClosure());
        assertFalse(result.hasDiversionsToFromClosure());

        assertTrue(result.isFullyClosed());

    }

    @Test
    void shouldThrowIfMandatoryFieldIsMissing() {
        String yaml = "stations: [ \"9400ZZMAECC\", \"9400ZZMALDY\", \"9400ZZMAWST\" ]\n" +
                "begin: 2023-07-15\n" +
                "fullyClosed: true";

        assertThrows(JsonProcessingException.class, () -> mapper.readValue(yaml, StationClosuresConfig.class));
    }

    @Test
    void shouldThrowIfUnknownExtra() {

        String yaml = """
                stations:
                    type: List
                    ids: [ "9400ZZMAECC", "9400ZZMALDY", "9400ZZMAWST" ]
                begin: 2023-07-15
                end: 2023-09-20
                UNKNOWN: 2023-09-20
                fullyClosed: true""";


        assertThrows(JsonProcessingException.class, () -> mapper.readValue(yaml, StationClosuresConfig.class));
    }
}
