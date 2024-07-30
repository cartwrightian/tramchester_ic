package com.tramchester.unit.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.config.StationClosuresConfig;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
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

        String yaml = "stations: [ \"9400ZZMAECC\", \"9400ZZMALDY\", \"9400ZZMAWST\" ]\n" +
                "dateRange:\n" +
                "   begin: 2023-07-15\n" +
                "   end: 2023-09-20\n" +
                "fullyClosed: true\n" +
                "diversionsAroundClosure: [ \"9400ZZMAVIC\" ]";

        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        IdSet<Station> stations = result.getStations();
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

        String yaml = "stations: [ \"9400ZZMAECC\", \"9400ZZMALDY\", \"9400ZZMAWST\" ]\n" +
                "dateRange:\n" +
                "   begin: 2023-07-15\n" +
                "   end: 2023-09-20\n" +
                "fullyClosed: true\n" +
                "diversionsToFromClosure: [ \"9400ZZMAMKT\" ]\n" +
                "diversionsAroundClosure: [ \"9400ZZMAVIC\" ]";


        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        IdSet<Station> stations = result.getStations();
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

        String yaml = "stations: [ \"9400ZZMAECC\", \"9400ZZMALDY\", \"9400ZZMAWST\" ]\n" +
                "dateRange:\n" +
                "   begin: 2023-07-15\n" +
                "   end: 2023-09-20\n" +
                "fullyClosed: true";


        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        IdSet<Station> stations = result.getStations();
        assertEquals(3, stations.size());
        assertTrue(stations.contains(TramStations.Eccles.getId()));
        assertTrue(stations.contains(TramStations.Ladywell.getId()));
        assertTrue(stations.contains(TramStations.Weaste.getId()));

        DateRange dateRange = result.getDateRange();
        assertEquals(TramDate.of(2023,7,15), dateRange.getStartDate());
        assertEquals(TramDate.of(2023,9,20), dateRange.getEndDate());

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

        String yaml = "stations: [ \"9400ZZMAECC\", \"9400ZZMALDY\", \"9400ZZMAWST\" ]\n" +
                "begin: 2023-07-15\n" +
                "end: 2023-09-20\n" +
                "UNKNOWN: 2023-09-20\n" +
                "fullyClosed: true";


        assertThrows(JsonProcessingException.class, () -> mapper.readValue(yaml, StationClosuresConfig.class));
    }
}
