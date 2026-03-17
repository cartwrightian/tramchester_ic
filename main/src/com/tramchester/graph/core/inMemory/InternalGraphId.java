package com.tramchester.graph.core.inMemory;

import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class InternalGraphId {
    @JsonIgnore
    abstract int getInternalId();
}
