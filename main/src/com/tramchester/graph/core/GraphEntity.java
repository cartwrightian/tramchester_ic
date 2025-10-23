package com.tramchester.graph.core;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public interface GraphEntity {
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean isNode();
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean isRelationship();
    Map<String, Object> getAllProperties();

}
