package com.tramchester.domain.id.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class PropertyDTOSerializer extends StdSerializer<PropertyDTO.PropertyDTOValue> {

    public static final String ENUM_SET = "EnumSet";
    public static final String DURATION_FIELD_NAME = "duration";
    public static final String TRANSPORT_MODE_FIELD_NAME = "transportMode";

    protected PropertyDTOSerializer(Class<PropertyDTO.PropertyDTOValue> t) {
        super(t);
    }

    protected PropertyDTOSerializer() {
        this(null);
    }

    @Override
    public void serializeWithType(PropertyDTO.PropertyDTOValue propertyDTOValue, JsonGenerator generator, SerializerProvider provider,
                                  TypeSerializer typeSerializer) throws IOException {

        Object dtoValue = propertyDTOValue.getValue();
        if (dtoValue instanceof EnumSet<? extends Enum<?>> enumSet) {
            serializeEnumSet(generator, enumSet);
        } else {
            provider.findTypedValueSerializer(dtoValue.getClass(), true, null).
                serializeWithType(dtoValue, generator, provider, typeSerializer);
        }

    }

    // special handling due to sealed implementations of EnumSet
    private static <T extends Enum<T>> void serializeEnumSet(JsonGenerator generator, EnumSet<T> enumSet) throws IOException {
        Optional<T> findExample = enumSet.stream().findFirst();

        if (findExample.isEmpty()) {
            throw new JsonParseException("Todo - empty enum set");
        }

        final Class<T> containedClass = findExample.get().getDeclaringClass();

        generator.writeStartObject();
        generator.writeFieldName(ENUM_SET);

            generator.writeStartObject();
            generator.writeFieldName(containedClass.getSimpleName());

            List<String> names = enumSet.stream().map(Enum::name).toList();
            // enum set handling, everything as a pojo
                generator.writeStartArray();
                for(String name : names) {
                    generator.writeString(name);
                }
                generator.writeEndArray();
            generator.writeEndObject();

        generator.writeEndObject();
    }

    @Override
    public void serialize(PropertyDTO.PropertyDTOValue dtoValue, JsonGenerator generator, SerializerProvider provider) throws IOException {
        throw new RuntimeException("Not defined");
    }
}
