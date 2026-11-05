package com.tramchester.unit.domain.time;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.time.TramDuration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class TramDurationTest {

    @Test
    void shouldHaveEquality() {
        TramDuration tramDurationA = TramDuration.ofSeconds(42L);
        TramDuration tramDurationB = TramDuration.ofSeconds(42L);

        assertEquals(42, tramDurationA.toSeconds());

        assertEquals(tramDurationA, tramDurationB);
        assertEquals(tramDurationB, tramDurationA);
        assertEquals(tramDurationA, tramDurationA);

    }

    @Disabled("perf test")
    @Test
    void shouldComparePerformanceBoxing() {
        int COUNT = 1000000000;

        final long unboxed = 42L;
        final Long boxed = unboxed;

        Instant start = Instant.now();
        for (long i = 0; i < COUNT; i++) {
            TramDuration duration = TramDuration.ofSeconds(unboxed);
            duration.isValid();
        }
        long firstTime = Duration.between(start, Instant.now()).toMillis();

        start = Instant.now();
        for (long i = 0; i < COUNT; i++) {
            TramDuration duration = TramDuration.ofSeconds(boxed);
            duration.isValid();
        }
        long secondTime = Duration.between(start, Instant.now()).toMillis();

        assertEquals(firstTime ,secondTime);
    }

    @Test
    void shouldAdd() {
        TramDuration tramDurationA = TramDuration.ofSeconds(10L);

        TramDuration resultA = tramDurationA.plusSeconds(5);
        assertEquals(15, resultA.toSeconds());

        TramDuration resultB = resultA.plusMinutes(2);

        assertEquals(15+(60*2), resultB.toSeconds());

        TramDuration resultC = resultB.plus(tramDurationA);

        assertEquals(25+(2*60), resultC.toSeconds());

    }

    @Test
    void shouldCreateFrom() {
        assertEquals(TramDuration.ofSeconds(2*60*60L), TramDuration.ofHours(2));
        assertEquals(TramDuration.ofSeconds(3*60L), TramDuration.ofMinutes(3));
    }

    @Test
    void shouldHaveSameObject() {
        assertSame(TramDuration.ZERO, TramDuration.ZERO);
        assertSame(TramDuration.ofMinutes(11), TramDuration.ofMinutes(11));
        assertSame(TramDuration.ofHours(42), TramDuration.ofHours(42));
        assertSame(TramDuration.ofSeconds(111L), TramDuration.ofSeconds(111L));
        assertSame(TramDuration.getInvalid(), TramDuration.getInvalid());
        assertSame(TramDuration.getMax(), TramDuration.getMax());
    }

    @Test
    void shouldTruncateToMinutes() {
        TramDuration tramDurationA = TramDuration.ofSeconds(10L);
        assertEquals(0, tramDurationA.truncateToMinutes().toSeconds());

        TramDuration tramDurationB= TramDuration.ofSeconds(65L);
        assertEquals(60, tramDurationB.truncateToMinutes().toSeconds());

        TramDuration tramDurationC = TramDuration.ofSeconds(120L);
        assertEquals(120, tramDurationC.truncateToMinutes().toSeconds());

        TramDuration tramDurationD = TramDuration.ofSeconds(121L);
        assertEquals(120, tramDurationD.truncateToMinutes().toSeconds());
    }

    @Test
    void shouldHaveMinus() {
        TramDuration tramDuration = TramDuration.ofSeconds(60L);

        TramDuration result = tramDuration.minus(TramDuration.ofSeconds(15L));

        assertEquals(TramDuration.ofSeconds(45L), result);

        assertEquals(TramDuration.ZERO, tramDuration.minus(tramDuration));
        assertSame(TramDuration.ZERO, tramDuration.minus(tramDuration));
    }

    @Test
    void shouldHaveToMinutes() {
        TramDuration tramDurationA = TramDuration.ofSeconds(10L);
        assertEquals(0, tramDurationA.toMinutes());

        TramDuration tramDurationB = TramDuration.ofSeconds(60L);
        assertEquals(1, tramDurationB.toMinutes());

        TramDuration tramDurationC = TramDuration.ofSeconds(61L);
        assertEquals(1, tramDurationC.toMinutes());

        TramDuration tramDurationD = TramDuration.ofSeconds(119L);
        assertEquals(1, tramDurationD.toMinutes());
    }

    @Test
    void shouldConvertToDuration() {
        TramDuration tramDuration = TramDuration.ofSeconds(42L);

        Duration result = tramDuration.toDuration();
        assertEquals(42, result.getSeconds());

        Duration truncated = result.truncatedTo(SECONDS);
        assertEquals(result, truncated);
    }

    @Test
    void shouldSerializeRoundTrip() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        TramDuration duration= TramDuration.ofSeconds(65L);

        String text = mapper.writeValueAsString(duration);

        TramDuration result = mapper.readValue(text, TramDuration.class);

        assertEquals(duration, result);
    }

    @Test
    void shouldCompareWithOtherDurations() {
        TramDuration durationA = TramDuration.ofMinutes(1);
        TramDuration durationB = TramDuration.ofMinutes(42);

        assertTrue(durationA.compareTo(durationB) < 0);
        assertTrue(durationB.compareTo(durationA) > 0);
        assertEquals(0, durationA.compareTo(durationA));
    }

    @Test
    void shouldHaveZeroInvalidAndMax() {
        assertEquals(TramDuration.ZERO, TramDuration.ofSeconds(0L));
        assertSame(TramDuration.ZERO, TramDuration.ofSeconds(0L));

        TramDuration max = TramDuration.getMax();
        assertTrue(max.isMax());

        TramDuration notMax = TramDuration.ofSeconds(Long.MAX_VALUE-1);
        assertFalse(notMax.isMax());

        TramDuration invalid = TramDuration.getInvalid();
        assertFalse(invalid.isValid());

    }

    @Test
    void shouldHaveComparisonHelperMethodsForCleanerCode() {
        TramDuration durationA = TramDuration.ofMinutes(1);
        TramDuration durationB = TramDuration.ofMinutes(42);

        assertTrue(durationA.lessThan(durationB));
        assertFalse(durationA.lessThan(durationA));
        assertTrue(durationB.moreThan(durationA));
        assertFalse(durationB.moreThan(durationB));
    }

}
