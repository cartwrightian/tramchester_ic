package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.tramchester.domain.time.TramDuration;

import java.io.IOException;

public class TramDurationSerializer extends JsonSerializer<TramDuration> {

    public static final String FIELD_NAME_SECONDS = "seconds";

    @Override
    public void serialize(TramDuration tramDuration, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        serialize(tramDuration, generator);
    }


    @Override
    public void serializeWithType(TramDuration tramDuration, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        WritableTypeId typeId = typeSer.typeId(tramDuration, JsonToken.VALUE_EMBEDDED_OBJECT);

        typeSer.writeTypePrefix(gen, typeId);
        serialize(tramDuration, gen);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    private static void serialize(TramDuration tramDuration, JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName(FIELD_NAME_SECONDS);
        generator.writeNumber(tramDuration.toSeconds());
        generator.writeEndObject();
    }

}
