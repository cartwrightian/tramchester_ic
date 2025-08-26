package com.tramchester.graph.core;

import java.util.Map;

public interface GraphEntity {
    boolean isNode();
    boolean isRelationship();
    Map<String, Object> getAllProperties();

}
