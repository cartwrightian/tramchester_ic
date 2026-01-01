package com.tramchester.unit.domain.time;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.time.TramDuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TramDurationTest {

    @Test
    void shouldHaveEquality() {
        TramDuration tramDurationA = TramDuration.ofSeconds(42);
        TramDuration tramDurationB = TramDuration.ofSeconds(42);

        assertEquals(42, tramDurationA.toSeconds());

        assertEquals(tramDurationA, tramDurationB);
        assertEquals(tramDurationB, tramDurationA);
        assertEquals(tramDurationA, tramDurationA);

    }

    @Test
    void shouldAdd() {
        TramDuration tramDurationA = TramDuration.ofSeconds(10);

        TramDuration resultA = tramDurationA.plusSeconds(5);
        assertEquals(15, resultA.toSeconds());

        TramDuration resultB = resultA.plusMinutes(2);

        assertEquals(15+(60*2), resultB.toSeconds());

        TramDuration resultC = resultB.plus(tramDurationA);

        assertEquals(25+(2*60), resultC.toSeconds());

    }

    @Test
    void shouldTruncateToMinutes() {
        TramDuration tramDurationA = TramDuration.ofSeconds(10);
        assertEquals(0, tramDurationA.truncateToMinutes().toSeconds());

        TramDuration tramDurationB= TramDuration.ofSeconds(65);
        assertEquals(60, tramDurationB.truncateToMinutes().toSeconds());

        TramDuration tramDurationC = TramDuration.ofSeconds(120);
        assertEquals(120, tramDurationC.truncateToMinutes().toSeconds());

        TramDuration tramDurationD = TramDuration.ofSeconds(121);
        assertEquals(120, tramDurationD.truncateToMinutes().toSeconds());
    }

    @Test
    void shouldSerializeRoundTrip() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        TramDuration duration= TramDuration.ofSeconds(65);

        String text = mapper.writeValueAsString(duration);

        TramDuration result = mapper.readValue(text, TramDuration.class);

        assertEquals(duration, result);
    }

}
