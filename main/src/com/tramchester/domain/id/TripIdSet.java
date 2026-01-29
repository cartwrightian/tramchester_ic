package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.input.Trip;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class TripIdSet implements ImmutableIdSet<Trip> {

    private static final Factory factory = new Factory();

    @JsonIgnore
    private final Set<String> graphIds;

    @JsonIgnore
    private static final TripIdSet empty = new TripIdSet(Collections.emptySet());

    @JsonCreator
    public static TripIdSet create(final @JsonProperty("ids") List<String> graphIds) {
        return create(new HashSet<>(graphIds));
    }

    public static TripIdSet create(final Set<String> graphIds) {
        return factory.create(graphIds);
    }

    private TripIdSet(final Set<String> graphIds) {
        this.graphIds = graphIds;
    }

    public static TripIdSet empty() {
        return empty;
    }

    public static TripIdSet singleton(IdFor<Trip> tripId) {
        return new TripIdSet(Collections.singleton(tripId.getGraphId()));
    }

    @Override
    public int size() {
        return graphIds.size();
    }

    @Override
    public boolean contains(IdFor<Trip> id) {
        return graphIds.contains(id.getGraphId());
    }

    @Override
    public boolean isEmpty() {
        return graphIds.isEmpty();
    }

    @Override
    public Stream<IdFor<Trip>> stream() {
        return graphIds.stream().map(Trip::createId);
    }

    @Override
    public @NotNull Iterator<IdFor<Trip>> iterator() {
        return stream().iterator();
    }

    @JsonProperty("ids")
    List<String> getIds() {
        return new ArrayList<>(graphIds);
    }

    public TripIdSet copyThenAppend(final IdFor<Trip> tripId) {
        final HashSet<String> copy = new HashSet<>(this.graphIds);
        copy.add(tripId.getGraphId());
        return create(copy);
    }

    @Override
    public String toString() {
        return "TripIdSet{" +
                "graphIds=" + graphIds +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        TripIdSet tripIdSet = (TripIdSet) object;
        return Objects.equals(graphIds, tripIdSet.graphIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(graphIds);
    }

    private static class Factory {

        // causes OOM issue, need cleverer approach
        private final ConcurrentMap<Set<String>, TripIdSet> cache;

        private Factory() {
            cache = new ConcurrentHashMap<>();
        }

        public TripIdSet create(final Set<String> graphIds) {
            return new TripIdSet(graphIds);
            //return cache.computeIfAbsent(graphIds, TripIdSet::new);
        }
    }
}
