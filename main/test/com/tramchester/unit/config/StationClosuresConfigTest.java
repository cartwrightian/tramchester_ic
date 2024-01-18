package com.tramchester.unit.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.config.StationClosuresConfig;
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
    void shouldParseYaml() throws JsonProcessingException {

        String yaml = "stations: [ \"9400ZZMAECC\", \"9400ZZMALDY\", \"9400ZZMAWST\" ]\n" +
                "begin: 2023-07-15\n" +
                "end: 2023-09-20\n" +
                "fullyClosed: true";


        StationClosuresConfig result = mapper.readValue(yaml, StationClosuresConfig.class);

        IdSet<Station> stations = result.getStations();
        assertEquals(3, stations.size());
        assertTrue(stations.contains(TramStations.Eccles.getId()));
        assertTrue(stations.contains(TramStations.Ladywell.getId()));
        assertTrue(stations.contains(TramStations.Weaste.getId()));

        assertEquals(TramDate.of(2023,7,15), result.getBegin());
        assertEquals(TramDate.of(2023,9,20), result.getEnd());

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
