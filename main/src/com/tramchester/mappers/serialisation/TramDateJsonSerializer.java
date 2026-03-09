package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.tramchester.domain.dates.TramDate;

import java.io.IOException;

public class TramDateJsonSerializer extends JsonSerializer<TramDate> {
    @Override
    public void serialize(TramDate date, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(date.serialize());
    }

    @Override
    public void serializeWithType(TramDate tramDate, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

        WritableTypeId typeId = typeSer.typeId(tramDate, JsonToken.VALUE_EMBEDDED_OBJECT);

        typeSer.writeTypePrefix(gen, typeId);
        gen.writeString(tramDate.serialize());
        typeSer.writeTypeSuffix(gen, typeId);
    }
}
