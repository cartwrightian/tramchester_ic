package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeJsonSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {

        gen.writeString(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
