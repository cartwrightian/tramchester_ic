package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.tramchester.domain.dates.TramDate;

import java.io.IOException;

public class TramDateJsonDeserializer extends JsonDeserializer<TramDate> {
    @Override
    public TramDate deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        return TramDate.parse(node.asText());
    }

    @Override
    public TramDate deserializeWithType(JsonParser jsonParser, DeserializationContext context, TypeDeserializer typeDeserializer) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        if (node.isObject()) {
            JsonNode contained = node.get("TramDate");
            String txt = contained.asText();
            return TramDate.parse(txt);
        } else {
            throw new JsonParseException(jsonParser, "Expected an object but got " + node);
        }

    }
}
