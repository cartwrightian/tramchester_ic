package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public interface GraphEntity {
    @JsonIgnore
    boolean isNode();

    @JsonIgnore
    boolean isRelationship();

    Map<String, Object> getAllProperties();

}
