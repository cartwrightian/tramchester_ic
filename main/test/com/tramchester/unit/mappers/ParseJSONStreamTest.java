package com.tramchester.unit.mappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;
import com.tramchester.resources.JsonStreamingOutput;
import com.tramchester.testSupport.ParseJSONStream;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ParseJSONStreamTest {

    @Test
    void shouldParseAStreamContainingDatesWithDateFormatSet() throws IOException {
        final LocalDateTime baseTime = TestEnv.LocalNow().truncatedTo(ChronoUnit.SECONDS);
        final int size = 2000;

        Stream<TestDTO> source = IntStream.range(0,size).mapToObj(baseTime::plusMinutes).map(TestDTO::new);

        JsonStreamingOutput<TestDTO> jsonStream = new JsonStreamingOutput<>(source);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        jsonStream.write(outputStream);

        // not very memory efficient....
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        ParseJSONStream<TestDTO> parseStream = new ParseJSONStream<>(TestDTO.class);
        List<TestDTO> received = parseStream.parseStream(inputStream);

        assertEquals(size, received.size());

        IntStream.range(0,size).mapToObj(baseTime::plusMinutes).map(TestDTO::new).forEach(expected -> {
            assertTrue(received.contains(expected), "missing " + expected );
        });

    }

    public static class TestDTO {
        @JsonDeserialize(using = TramTimeJsonDeserializer.class)
        @JsonSerialize(using = TramTimeJsonSerializer.class)
        public TramTime tramTime;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateFormatForJson)
        public LocalDate localDate;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TramchesterConfig.DateTimeFormatForJson)
        public LocalDateTime localDateTime;

        @JsonProperty(value = "text")
        public String text;

        public TestDTO() {
            // deserialization
        }

        public TestDTO(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
            this.localDate = localDateTime.toLocalDate().minusDays(1);
            this.tramTime = TramTime.ofHourMins(localDateTime.toLocalTime());
            this.text = "sometext"+tramTime.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestDTO testDTO = (TestDTO) o;
            return Objects.equals(tramTime, testDTO.tramTime) && Objects.equals(localDate, testDTO.localDate) && Objects.equals(localDateTime, testDTO.localDateTime) && Objects.equals(text, testDTO.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tramTime, localDate, localDateTime, text);
        }

        @Override
        public String toString() {
            return "TestDTO{" +
                    "tramTime=" + tramTime +
                    ", localDate=" + localDate +
                    ", localDateTime=" + localDateTime +
                    ", text='" + text + '\'' +
                    '}';
        }
    }
}
