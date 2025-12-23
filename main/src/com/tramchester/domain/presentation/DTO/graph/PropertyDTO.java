package com.tramchester.domain.presentation.DTO.graph;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.id.serialization.PropertyDTODeserializer;
import com.tramchester.domain.id.serialization.PropertyDTOSerializer;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static com.tramchester.domain.id.serialization.PropertyDTOSerializer.DURATION_FIELD_NAME;
import static com.tramchester.domain.id.serialization.PropertyDTOSerializer.TRANSPORT_MODE_FIELD_NAME;

/***
 * Supports saving of graphs
 */
public class PropertyDTO {

    private final String key;
    private final PropertyDTOValue value;

    @JsonIgnore
    public PropertyDTO(
            final String key,
            final Object value) {
        this.key = key;
        this.value = new PropertyDTOValue(value);
    }

    @JsonCreator
    public PropertyDTO(
            @JsonProperty("key")
            final String key,
            @JsonProperty("value")
            final PropertyDTOValue value) {
        this.key = key;
        this.value = value;
    }

    // called direct by getters on the Node and Relationship implementations
    public static PropertyDTO fromMapEntry(final Map.Entry<String, Object> entry) {
        return new PropertyDTO(entry.getKey(), entry.getValue());
    }

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("value")
    public PropertyDTOValue getValue() {
        return value;
    }

    @JsonIgnore
    public Object getContainedValue() {
        return value.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PropertyDTO that = (PropertyDTO) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "{" + key + '=' + value +
                '}';
    }

    @JsonSerialize(using = PropertyDTOSerializer.class)
    @JsonDeserialize(using = PropertyDTODeserializer.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TramTime.class, name = "TramTime"),
            @JsonSubTypes.Type(value = Duration.class, name = DURATION_FIELD_NAME),
            @JsonSubTypes.Type(value = TransportMode.class, name = TRANSPORT_MODE_FIELD_NAME)
    })
    public static class PropertyDTOValue {

        private final Object value;

        public PropertyDTOValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PropertyDTOValue that = (PropertyDTOValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return "{" +
                     value +
                    '}';
        }
    }


}
