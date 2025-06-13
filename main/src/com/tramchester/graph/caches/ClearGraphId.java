package com.tramchester.graph.caches;

import com.tramchester.graph.facade.GraphId;

public interface ClearGraphId<T extends GraphId> {
    void remove(T id);
    void close();
}
