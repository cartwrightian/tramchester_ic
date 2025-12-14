package com.tramchester.domain.presentation.DTO.graph;

import com.fasterxml.jackson.annotation.*;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.util.*;

/***
 * Supports saving of graphs
 */
public class PropertyDTO {

    @JsonProperty
    private final String key;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TramTime.class, name = "tramTime"),
            @JsonSubTypes.Type(value = Duration.class, name = "duration"),
            @JsonSubTypes.Type(value = IdSet.class, name = "idSet"),
            @JsonSubTypes.Type(value = TransportMode.class, name = "transportMode"),
            @JsonSubTypes.Type(value = EnumSet.class, name = "enumSet")
            //@JsonSubTypes.Type(value = EnumSetDTO.class, name = "enumSet")
    })
    private final Object value;

    @JsonCreator
    public PropertyDTO(
            @JsonProperty("key") final String key,
            @JsonProperty("value") final Object value) {
        this.key = key;
        this.value = value;
    }

    // called direct by getters on the Node and Relationship implementations
    public static PropertyDTO fromMapEntry(final Map.Entry<String, Object> entry) {
        Object entryValue = entry.getValue();
        if (entry.getKey().equals("transport_modes")) {
            if (entryValue instanceof EnumSet<?> enumSet) {
                EnumSet<TransportMode> transportModes = (EnumSet<TransportMode>) enumSet;
                return new PropertyDTO(entry.getKey(), transportModes.stream().toList());
            } else {
                throw new RuntimeException("unexpected contents for transport_modes:" + entryValue.toString());
            }
        }
        return new PropertyDTO(entry.getKey(), entryValue);
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
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
        return "PropertyDTO{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
    }

//    public static class EnumSetDTO {
//
//        @JsonIgnore
//        private final Set<TransportMode> theSet;
//
//        @JsonProperty("contents")
//        public Set<TransportMode> getContents() {
//            return theSet;
//        }
//
//        @JsonCreator
//        public EnumSetDTO(
//                @JsonProperty(value = "contents", required = true) final Set<TransportMode> theSet) {
//            super();
//        }
//    }
}
