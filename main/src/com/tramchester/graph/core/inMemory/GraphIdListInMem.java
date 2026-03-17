package com.tramchester.graph.core.inMemory;

import com.tramchester.graph.core.GraphId;
import com.tramchester.graph.core.GraphIdList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GraphIdListInMem implements GraphIdList {
    private final List<GraphId> ids;

    public GraphIdListInMem(final GraphIdListInMem other) {
        ids = new ArrayList<>(other.ids);
    }

    public GraphIdListInMem() {
        ids = new ArrayList<>();
    }

    @Override
    public void add(GraphId id) {
        ids.add(id);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        GraphIdListInMem that = (GraphIdListInMem) object;
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }
}
