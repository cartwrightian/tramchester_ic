package com.tramchester.unit.domain.presentation.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.DTO.query.GridQueryDTO;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GridQueryDTOTest {

    @Test
    void shouldRoundTripDTO() throws JsonProcessingException {
        JsonMapper mapper = JsonMapper.builder().
                addModule(new JavaTimeModule()).
                addModule(new AfterburnerModule()).
                build();

        GridQueryDTO gridQueryDTO = new com.tramchester.domain.presentation.DTO.query.GridQueryDTO(LocationType.Station, TramStations.StPetersSquare.getIdForDTO(),
                LocalDate.of(2021, 6,15), LocalTime.of(9,15), 30, 2, 1000);


        String json = mapper.writeValueAsString(gridQueryDTO);

        GridQueryDTO result = mapper.readValue(json, GridQueryDTO.class);

        assertEquals(gridQueryDTO, result);

    }
}
