package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.BitSet;

public class BitSetSerializer extends JsonSerializer<BitSet> {
    @Override
    public void serialize(BitSet bitSet, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        for(long word : bitSet.toLongArray()) {
            gen.writeNumber(word);
        }
        gen.writeEndArray();
    }
}
