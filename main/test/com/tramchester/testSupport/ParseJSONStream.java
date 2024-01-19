package com.tramchester.testSupport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.core.JsonToken.*;

public class ParseJSONStream<T> {

    private final ObjectMapper mapper;
    private final Class<T> valueType;

    public ParseJSONStream(Class<T> valueType) {
        this.valueType = valueType;
        mapper = JsonMapper.builder().
                addModule(new JavaTimeModule()).
                addModule(new AfterburnerModule()).
                build();

//        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @NotNull
    public List<T> receive(Response response, InputStream inputStream) throws IOException {
        List<T> received = parseStream(inputStream);
        response.close();
        return received;
    }

    @NotNull
    public List<T> parseStream(InputStream inputStream) throws IOException {
        List<T> received = new ArrayList<>();

        try (final JsonParser jsonParser = mapper.getFactory().createParser(inputStream)) {
            JsonToken nextToken = jsonParser.nextToken();

            while (START_ARRAY.equals(nextToken) || START_OBJECT.equals(nextToken)) {
                if (START_OBJECT.equals(nextToken)) {
                    readObject(valueType, received, jsonParser, nextToken);
                }
                nextToken = jsonParser.nextToken();
                while (VALUE_STRING.equals(nextToken)) {
                    // consume line breaks written by server
                    nextToken = jsonParser.nextToken();
                }
            }
        }

        inputStream.close();
        return received;
    }

    private void readObject(final Class<T> valueType, final List<T> received, final JsonParser jsonParser, JsonToken current) throws IOException {
        while (START_OBJECT.equals(current)) {
            final JsonToken next = jsonParser.nextToken();
            if (JsonToken.FIELD_NAME.equals(next)) {
                final T item = mapper.readValue(jsonParser, valueType); //jsonParser.readValueAs(valueType);
                received.add(item);
            }
            current = jsonParser.nextToken();
        }
    }
}
