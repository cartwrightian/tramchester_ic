package com.tramchester.domain.id.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.google.common.collect.Streams;
import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.presentation.DTO.graph.PropertyDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.serialization.IdSetSerializer.ID_SET;
import static com.tramchester.domain.id.serialization.PropertyDTOSerializer.*;

//  TODO use handleUnexpectedToken etc instead of throwing JsonParseException

public class PropertyDTODeserializer extends JsonDeserializer<PropertyDTO.PropertyDTOValue>  {

    private static final Reflections reflections = new Reflections(CoreDomain.class.getPackageName());;

    @Override
    public PropertyDTO.PropertyDTOValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        throw new RuntimeException("Not defined, always need types");
    }

    @Override
    public PropertyDTO.PropertyDTOValue deserializeWithType(JsonParser jsonParser, DeserializationContext context, TypeDeserializer typeDeserializer) throws IOException, JacksonException {
        final ObjectCodec objectCodec = jsonParser.getCodec();
        final JsonNode valueNode = objectCodec.readTree(jsonParser);

        final Object value;

        if (valueNode.isObject()) {
            // TODO feels like should be way to look these up from attributes etc but jackson seems to enforce these
            // having to be subtypes of PropertyDTO.PropertyDTOValue which doesn't work for this use case

            if (valueNode.has(ENUM_SET)) {
                value = deserializeEnumSet(jsonParser, valueNode.get(ENUM_SET));
            }
            else if (valueNode.has(TramTime.class.getSimpleName())) {
                value = deserializeTramTime(context, typeDeserializer, valueNode, objectCodec);
            } else if (valueNode.has(ID_SET)) {
                value = deserializeIdSet(context, typeDeserializer, valueNode.get(ID_SET), objectCodec);
            }
            else if (valueNode.has(DURATION_FIELD_NAME)) {
                DurationDeserializer deserializer = new DurationDeserializer();
                JsonParser parser = valueNode.get(DURATION_FIELD_NAME).traverse(objectCodec);
                parser.nextToken();
                value = deserializer.deserialize(parser, context);
            } else if (valueNode.has(TRANSPORT_MODE_FIELD_NAME)) {
                JsonNode textNode = valueNode.get(TRANSPORT_MODE_FIELD_NAME);
                String txt = textNode.asText();
                value = TransportMode.valueOf(txt);
            }
            else {
                final JsonParser valueNodeParser = valueNode.traverse();
                valueNodeParser.setCodec(objectCodec);
                valueNodeParser.nextToken();
                value = typeDeserializer.deserializeTypedFromObject(valueNodeParser, context);
            }
        }
        else if (valueNode.isTextual()) {
            value = valueNode.textValue();
        } else if (valueNode.isDouble()) {
            value = valueNode.doubleValue();
        } else if (valueNode.isInt()) {
            value =  valueNode.intValue();
        }
        else {
            throw new JsonParseException(jsonParser, "PropertyDTODeserializer could not handle node '" + valueNode +
                    "' of type " + valueNode.getNodeType());
        }

        return new PropertyDTO.PropertyDTOValue(value);
    }

    private IdSet<?> deserializeIdSet(DeserializationContext context, TypeDeserializer typeDeserializer, JsonNode valueNode, ObjectCodec objectCodec) throws IOException {
        final IdSetDeserializer deserializer = new IdSetDeserializer();
        JsonParser parser = valueNode.traverse(objectCodec);
        parser.nextToken();
        return deserializer.deserialize(parser, context);
    }

    private static TramTime deserializeTramTime(DeserializationContext context, TypeDeserializer typeDeserializer, JsonNode valueNode, ObjectCodec objectCodec) throws IOException {
        final TramTimeJsonDeserializer deserializer = new TramTimeJsonDeserializer();
        JsonParser parser = valueNode.traverse(objectCodec);
        parser.nextToken();
        return deserializer.deserializeWithType(parser, context, typeDeserializer);
    }

    private EnumSet<?> deserializeEnumSet(JsonParser jsonParser, JsonNode enumSetNode) throws JsonParseException {
        if (!enumSetNode.isObject()) {
            throw new JsonParseException(jsonParser, "Expected object for EnumSet " + enumSetNode);
        }
        // find out the type, should be one field containing the type simple name
        List<String> fieldNames = Streams.stream(enumSetNode.fieldNames()).toList();
        if (fieldNames.size()!=1) {
            throw new JsonParseException(jsonParser, "Expected 1 field name for EnumSet node " + enumSetNode);
        }
        final String fieldName = fieldNames.getFirst();
        Class<? extends Enum> containedType = getContentsTypeFor(fieldName, jsonParser);

        // load in the text values for the enum
        final JsonNode contentsNode = enumSetNode.get(fieldName);
        if (!contentsNode.isArray()) {
            throw new JsonParseException(jsonParser, "Expected contents as an array for EnumSet node " + enumSetNode);
        }
        Set<Enum> theSet = contentsNode.valueStream().
                filter(JsonNode::isTextual).
                map(JsonNode::asText).
                map(txt -> Enum.valueOf(containedType, txt)).
                collect(Collectors.toSet());
        if (theSet.size() != contentsNode.size()) {
            throw new JsonParseException(jsonParser, "mismatch on items size, got " + theSet.size() + " but needed " + contentsNode.size());
        }

        return EnumSet.copyOf(theSet);
    }

    private Class<? extends Enum> getContentsTypeFor(String name, JsonParser forDiag) throws JsonParseException {
        Set<Class<? extends Enum>> candidates = reflections.getSubTypesOf(Enum.class);

        Optional<Class<? extends Enum>> matched = candidates.stream().
                filter(Class::isEnum).
                filter(candidate -> candidate.getSimpleName().equals(name)).
                findFirst();

        if (matched.isPresent()) {
            Class<? extends Enum> found = matched.get();
            return found;
        }

        throw new JsonParseException(forDiag, "Cannot find CoreDomain Enum type matching " + name);
    }


    private static Object getTransportModes(JsonParser jsonParser, String key, JsonNode valueNode) throws JsonParseException {
        final Object value;
        if (!"transport_modes".equals(key)) {
            throw new JsonParseException(jsonParser, "not expecting enumset for " + key);
        }
        // array means enumset
        final List<String> textItems = new ArrayList<>();
        for (Iterator<JsonNode> it = valueNode.elements(); it.hasNext(); ) {
            final JsonNode item = it.next();
            if (item.isTextual()) {
                textItems.add(item.textValue());
            } else {
                throw new JsonParseException(jsonParser, "PropertyDTODeserializer could not handle enum node " + item);
            }
        }
        Set<TransportMode> modes = textItems.stream().map(TransportMode::valueOf).collect(Collectors.toSet());
        value = EnumSet.copyOf(modes);
        return value;
    }

}
