package com.tramchester.domain.id.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static com.tramchester.domain.id.serialization.IdSetSerializer.CONTAINS_FIELD_NAME;
import static com.tramchester.domain.id.serialization.IdSetSerializer.ITEMS_FIELD_NAME;

public class IdSetDeserializer extends JsonDeserializer<IdSet<?>> {

    private static final Reflections reflections = new Reflections(CoreDomain.class.getPackageName());

    @Override
    public IdSet<?> deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        final ObjectCodec objectCodec = jsonParser.getCodec();
        final JsonNode node = objectCodec.readTree(jsonParser);

        if (!node.has(CONTAINS_FIELD_NAME)) {
            throw new JsonParseException(jsonParser, "IdSetDeserializer Missing field " + CONTAINS_FIELD_NAME);
        }
        if (!node.has(ITEMS_FIELD_NAME)) {
            throw new JsonParseException(jsonParser, "IdSetDeserializer Missing field " + ITEMS_FIELD_NAME);
        }

        final String contentsName = node.get(CONTAINS_FIELD_NAME).asText();

        final Class<? extends CoreDomain> contentsType = getContentsTypeFor(contentsName, jsonParser);

        final JsonNode itemsNode = node.get(ITEMS_FIELD_NAME);
        if (itemsNode instanceof ArrayNode arrayNode) {
            final IdSet<? extends CoreDomain> theSet = arrayNode.valueStream().
                    filter(JsonNode::isTextual).
                    map(JsonNode::asText).
                    map(txt -> StringIdFor.createId(txt, contentsType)).
                    collect(IdSet.idCollector());
            if (theSet.size() != arrayNode.size()) {
                throw new JsonParseException(jsonParser, "mismatch on items size, got " + theSet.size() + " but needed " + arrayNode.size());
            }
            return theSet;
        }
        throw new JsonParseException(jsonParser, "field " + ITEMS_FIELD_NAME + " was not an ArrayNode");
    }

    private Class<? extends CoreDomain> getContentsTypeFor(String name, JsonParser forDiag) throws JsonParseException {
        Set<Class<? extends CoreDomain>> candidates = reflections.getSubTypesOf(CoreDomain.class);

        Optional<Class<? extends CoreDomain>> matched = candidates.stream().
                filter(candidate -> candidate.getSimpleName().equals(name)).
                findFirst();

        if (matched.isPresent()) {
            return matched.get();
        }

        throw new JsonParseException(forDiag, "Cannot find CoreDomain type matching " + name);
    }
}
