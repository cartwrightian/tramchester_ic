package com.tramchester.domain;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Trips implements Iterable<Trip> {
    private final Set<Trip> trips;
    private Boolean intoNextDay;

    public Trips() {
        trips = new HashSet<>();
        intoNextDay = null;
    }

    public void add(final Trip trip) {
        trips.add(trip);
    }

    public int size() {
        return trips.size();
    }

    public IdSet<Station> getStartStations() {
        return trips.stream().map(Trip::firstStation).collect(IdSet.idCollector());
    }

    public boolean intoNextDay() {
        if (intoNextDay==null) {
            intoNextDay = trips.stream().anyMatch(Trip::intoNextDay);
        }
        return intoNextDay;
    }

    @Override
    public String toString() {
        return "Trips{" +
                "trips=" + HasId.asIds(trips) +
                ", intoNextDay=" + intoNextDay +
                '}';
    }

    public boolean anyOn(TramDate date) {
        // Testing showed little performance impact from caching these results for trams
        return trips.stream().anyMatch(trip -> trip.operatesOn(date));
    }

    @Override
    public void forEach(Consumer<? super Trip> action) {
        trips.forEach(action);
    }

    public Stream<Trip> stream() {
        return trips.stream();
    }

    @Override
    public @NotNull Iterator<Trip> iterator() {
        return trips.iterator();
    }

    public boolean isEmpty() {
        return trips.isEmpty();
    }
}
