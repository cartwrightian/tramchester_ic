package com.tramchester.unit.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimestampedTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldRoundTrip() throws JsonProcessingException {

        LocalDateTime when = TestEnv.LocalNow().truncatedTo(ChronoUnit.MILLIS);

        Timestamped timestamped = new Timestamped(TramStations.Altrincham.getId(), when, LocationType.Station);

        String json = objectMapper.writeValueAsString(timestamped);

        Timestamped result = objectMapper.readValue(json, Timestamped.class);

        assertEquals(timestamped.getWhen(), result.getWhen());
        assertEquals(timestamped.getId(), result.getId());
        assertEquals(timestamped.getLocationType(), result.getLocationType());
    }

    @Test
    void shouldDefaultStationForPreviousFormat() throws JsonProcessingException {

        long millis = 1705508246161L;
        LocalDateTime expectedDateTime = Instant.ofEpochMilli(millis).atZone(TramchesterConfig.TimeZoneId).toLocalDateTime();

        String legacyValue = "{\"when\":1705508246161,\"id\":\"9400ZZMAALT\"}";

        Timestamped result = objectMapper.readValue(legacyValue, Timestamped.class);

        assertEquals(expectedDateTime, result.getWhen());
        assertEquals(TramStations.Altrincham.getIdForDTO(), result.getId());
        assertEquals(LocationType.Station, result.getLocationType());

    }
}
