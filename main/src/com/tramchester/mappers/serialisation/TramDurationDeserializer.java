package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.tramchester.domain.time.TramDuration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class TramDurationDeserializer extends JsonDeserializer<TramDuration> {
    @Override
    public TramDuration deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        return deserialize(jsonParser, node);
    }

    @Override
    public TramDuration deserializeWithType(JsonParser jsonParser, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        if (node.isObject()) {
            JsonNode contained = node.get("duration");
            if (contained==null) {
                throw new JsonParseException(jsonParser, "Expected an node with name duration but got " + node);
            }
            return deserialize(jsonParser, contained);
        } else {
            throw new JsonParseException(jsonParser, "Expected an object but got " + node);
        }
    }

    private static @NotNull TramDuration deserialize(JsonParser jsonParser, JsonNode node) throws JsonParseException {
        JsonNode secondsNode = node.get(TramDurationSerializer.FIELD_NAME_SECONDS);
        if (secondsNode.isInt()) {
            final int seconds = secondsNode.asInt();
            return TramDuration.ofSeconds(seconds);
        } else {
            throw new JsonParseException(jsonParser, "Could not extract duraction from " + node);
        }
    }
}
