package com.tramchester.domain.id.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.IdSet;

import java.io.IOException;
import java.util.Optional;

public class IdSetSerializer extends StdSerializer<IdSet<?>> {

    public static final String CONTAINS_FIELD_NAME = "contains";
    public static final String ITEMS_FIELD_NAME = "items";

    protected IdSetSerializer(Class<IdSet<?>> t) {
        super(t);
    }

    protected IdSetSerializer() {
        this(null);
    }

    @Override
    public void serializeWithType(IdSet<?> value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        Optional<? extends IdFor<?>> anyItem = value.stream().findAny();

        if (anyItem.isPresent()) {
            IdFor<?> sampleItem = anyItem.get();

            gen.writeStartObject();
                gen.writeFieldName("idSet");

                gen.writeStartObject();
                    gen.writeStringField(CONTAINS_FIELD_NAME,sampleItem.getDomainType().getSimpleName());

                    gen.writeFieldName(ITEMS_FIELD_NAME);
                        gen.writeStartArray();
                        if (!value.isEmpty()) {
                            for(IdFor<?> item : value) {
                                IdForDTO idForDTO = new IdForDTO(item);
                                gen.writeObject(idForDTO);
                            }
                        }
                        gen.writeEndArray();

                    gen.writeEndObject();
            gen.writeEndObject();

        } else {
            gen.writeStartArray();
            gen.writeEndArray();
        }

    }

    @Override
    public void serialize(IdSet<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        throw new RuntimeException("not used");
    }
}
