package com.tramchester.graph.filters;


/***
 * Use this when just want to check if any filtering is in place
 */
public class GraphFilterActive {
    private final boolean isActive;

    public GraphFilterActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    @Deprecated
    public boolean isFiltered() {
        return isActive;
    }
}
