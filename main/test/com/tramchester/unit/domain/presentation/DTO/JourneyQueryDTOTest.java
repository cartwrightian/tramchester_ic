package com.tramchester.unit.domain.presentation.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.DTO.query.JourneyQueryDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JourneyQueryDTOTest {

    private final TramchesterConfig config = new LocalConfig();

    @Test
    void shouldSerializedDeserialize() throws JsonProcessingException {
        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                LocalTime.of(13,56), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 2, true);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String txt = mapper.writeValueAsString(dto);

        JourneyQueryDTO result = mapper.readValue(txt, JourneyQueryDTO.class);

        assertEquals(dto.getDate(), result.getDate());
        assertEquals(dto.getTime(), result.getTime());
        assertEquals(dto.getDestType(), result.getDestType());
        assertEquals(dto.getDestId(), result.getDestId());
        assertEquals(dto.getStartType(), result.getStartType());
        assertEquals(dto.getStartId(), result.getStartId());
        assertEquals(dto.getMaxChanges(), result.getMaxChanges());
        assertEquals(dto.getModes(), result.getModes());
        assertEquals(dto.getDiagnostics(), result.getDiagnostics());
        assertNull(dto.getMaxNumResults());

    }

    @Test
    void shouldSerializedDeserializeWithMaxNumResults() throws JsonProcessingException {
        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                LocalTime.of(13,56), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 2, true);
        dto.setMaxNumResults(42);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String txt = mapper.writeValueAsString(dto);

        JourneyQueryDTO result = mapper.readValue(txt, JourneyQueryDTO.class);

        assertEquals(42, result.getMaxNumResults());
        assertEquals(LocalTime.of(13,56), result.getTime());

    }

    @Test
    void shouldConvertToJourneyRequest() {
        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                LocalTime.of(13,56), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 2, true);
        dto.setMaxNumResults(42);

        JourneyRequest result = JourneyQueryDTO.toJourneyRequest(config, dto);

        assertEquals(dto.getDate(), result.getDate().toLocalDate());
        assertEquals(dto.getTime(), result.getOriginalTime().asLocalTime());
        assertEquals(dto.getMaxChanges(), result.getMaxChanges().get());
        assertEquals(dto.isArriveBy(), result.getArriveBy());
        assertEquals(TransportMode.TramsOnly, result.getRequestedModes());
        assertEquals(config.getMaxJourneyDuration(), result.getMaxJourneyDuration().getMinutesSafe());
        assertEquals(dto.getDiagnostics(), result.getDiagnosticsEnabled());
        assertEquals(42, result.getMaxNumberOfJourneys());
    }

    @Test
    void shouldConvertToJourneyNoDiagnostics() {
        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                LocalTime.of(13,56), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 2, null);

        JourneyRequest result = JourneyQueryDTO.toJourneyRequest(config, dto);

        assertFalse(result.getDiagnosticsEnabled());
    }

    @Test
    void shouldConvertToJourneyRequestLateNightIntoNextDay() {
        TramTime afterMidnight = TramTime.nextDay(0,14);

        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                afterMidnight.asLocalTime(), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 2, true);

        JourneyRequest result = JourneyQueryDTO.toJourneyRequest(config, dto);

        assertEquals(dto.getDate(), result.getDate().toLocalDate());
        assertEquals(afterMidnight, result.getOriginalTime());
    }

    @Test
    void shouldConvertToJourneyRequestFirstTram() {
        TramTime firstTram = TestEnv.EarliestTramTime;

        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                firstTram.asLocalTime(), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 2, true);

        JourneyRequest result = JourneyQueryDTO.toJourneyRequest(config, dto);

        assertEquals(dto.getDate(), result.getDate().toLocalDate());
        assertEquals(firstTram, result.getOriginalTime());
    }

    private static class LocalConfig extends TestConfig {

        @Override
        public EnumSet<TransportMode> getTransportModes() {
            return EnumSet.of(TransportMode.Tram);
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }
    }
}
