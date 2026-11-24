package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphId;
import com.tramchester.graph.core.GraphIdList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GraphIdListInMem implements GraphIdList {
    private final List<Integer> ids;
    private int hash;

    public GraphIdListInMem(final GraphIdListInMem other) {
        ids = new ArrayList<>(other.ids);
        hash = other.hash;
    }

    public GraphIdListInMem() {
        ids = new ArrayList<>();
        hash = 1;
    }

    @Override
    public void add(final GraphId id) {
        final InternalGraphId internalGraphId = (InternalGraphId) id;
        final int actualId = internalGraphId.getInternalId();
        synchronized (ids) {
            ids.add(actualId);
            hash = 31 * hash + actualId;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        GraphIdListInMem that = (GraphIdListInMem) object;
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        return hash;
        //return Objects.hashCode(ids);
    }
}
