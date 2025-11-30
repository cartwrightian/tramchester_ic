package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.tramchester.domain.time.TramTime;

import java.io.IOException;

public class TramTimeJsonSerializer extends JsonSerializer<TramTime> {

    @Override
    public void serialize(TramTime time, JsonGenerator gen, SerializerProvider serializerProvider)
            throws IOException {

        gen.writeString(time.serialize());
    }

    @Override
    public void serializeWithType(TramTime tramTime, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {

        WritableTypeId typeId = typeSer.typeId(tramTime, JsonToken.VALUE_STRING);

        typeSer.writeTypePrefix(gen, typeId);
        gen.writeString(tramTime.serialize());
        typeSer.writeTypeSuffix(gen, typeId);
    }
}
