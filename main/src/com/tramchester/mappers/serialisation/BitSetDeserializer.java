package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class BitSetDeserializer extends JsonDeserializer<BitSet> {
    @Override
    public BitSet deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        List<Long> longs = new ArrayList<>();

        JsonToken token;
        while(!JsonToken.END_ARRAY.equals(token = parser.nextToken())) {
            if (!token.isNumeric()) {
                throw new JsonParseException(parser, "Unexpected non numeric value " + token);
            }
            longs.add(parser.getLongValue());
        }
        return BitSet.valueOf(toPrimitiveArray(longs));
    }

    private long[] toPrimitiveArray(final List<Long> list) {
        final int size = list.size();
        final long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
