package com.tramchester.graph.core;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.neo4j.RouteCalculatorNeo4J;
import com.tramchester.graph.search.neo4j.RouteCalculatorSupport;

import java.util.Objects;

public final class TimedPath {
    private final GraphPath path;
    private final TramTime queryTime;
    private final int numChanges;

    public TimedPath(final GraphPath path, final TramTime actualQueryTime, int numChanges) {
        this.path = path;
        this.queryTime = actualQueryTime;
        this.numChanges = numChanges;
    }

    public TimedPath(final GraphPath path, final RouteCalculatorSupport.PathRequest pathRequest) {
        this(path, pathRequest.getActualQueryTime(), pathRequest.getNumChanges());
    }

    @Override
    public String toString() {
        return "TimedPath{" +
                "path=" + path +
                ", queryTime=" + queryTime +
                ", numChanges=" + numChanges +
                '}';
    }

    public GraphPath path() {
        return path;
    }

    public TramTime queryTime() {
        return queryTime;
    }

    public int numChanges() {
        return numChanges;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TimedPath) obj;
        return Objects.equals(this.path, that.path) &&
                Objects.equals(this.queryTime, that.queryTime) &&
                this.numChanges == that.numChanges;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, queryTime, numChanges);
    }

}
