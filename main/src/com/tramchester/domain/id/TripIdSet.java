package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.tramchester.domain.input.Trip;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class TripIdSet implements ImmutableIdSet<Trip> {

    @JsonIgnore
    private final ImmutableSet<@NotNull String> graphIds;

    private TripIdSet(final ImmutableSet<@NotNull String> graphIds) {
        this.graphIds = graphIds;
    }

    @JsonCreator
    public static TripIdSet deserialize(final @JsonProperty("ids") List<String> graphIds) {
        return Factory.deserialize(new HashSet<>(graphIds));
    }

    ////////

    @Override
    public int size() {
        return graphIds.size();
    }

    @Override
    public boolean contains(final IdFor<Trip> id) {
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

    public static class Container {
        private final AtomicInteger usage;
        private final TripIdSet tripIdSet;

        public Container(AtomicInteger usage, TripIdSet tripIdSet) {
            this.usage = usage;
            this.tripIdSet = tripIdSet;
        }

        public Container(final TripIdSet tripIdSet) {
            this(new AtomicInteger(0), tripIdSet);
        }

        public int decrementUsage() {
            return usage.decrementAndGet();
        }

        public void incrementUsage() {
            usage.incrementAndGet();
        }
    }

    public static class Factory {

        public static final TripIdSet Empty = new TripIdSet(ImmutableSet.of());

        // naive initial implementation
        private static final ConcurrentMap<ImmutableSet<@NotNull String>, Container> cache = new ConcurrentHashMap<>();

        private Factory() {
        }

        public static TripIdSet empty() {
            return Empty;
        }

        private static TripIdSet create(final Set<String> graphIds) {
            final ImmutableSet<@NotNull String> theIds = ImmutableSet.copyOf(graphIds);
            final TripIdSet tripIdSet = new TripIdSet(theIds);
            final Container container = cache.computeIfAbsent(theIds, key -> new Container(tripIdSet));
            container.incrementUsage();
            return container.tripIdSet;
        }

        // helpers

        public static TripIdSet deserialize(final Set<String> ids) {
            return create(ids);
        }

        public static TripIdSet copyThenAppend(final TripIdSet existing, final IdFor<Trip> tripId) {
            return copyThenAppend(existing.graphIds, tripId.getGraphId());
        }

        private static TripIdSet copyThenAppend(ImmutableSet<@NotNull String> graphIds, String graphId) {
            final Set<String> copy = new HashSet<>(graphIds);
            copy.add(graphId);
            if (cache.containsKey(graphIds)) {
                Container container = cache.get(graphIds);
                int currentCount = container.decrementUsage();
                if (currentCount==0) {
                    cache.remove(graphIds);
                }
            }
            return create(copy);
        }

        public static TripIdSet singleton(IdFor<Trip> tripId) {
            return create(Collections.singleton(tripId.getGraphId()));
        }
    }
}
