package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationsWithDiversionRepository;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@LazySingleton
public class StationsWithDiversion implements StationsWithDiversionRepository {
    private final Map<Station, Set<DateTimeRange>> diversions;

    @Inject
    public StationsWithDiversion() {
        diversions = new HashMap<>();
    }

    @Override
    public boolean hasDiversions(final Station station) {
        return diversions.containsKey(station);
    }

    @Override
    public Set<DateTimeRange> getDateTimeRangesFor(Station station) {
        return diversions.get(station);
    }

    public void add(final Station station, final DateTimeRange dateTimeRange) {
        if (diversions.containsKey(station)) {
            final Set<DateTimeRange> currentRanges = diversions.get(station);
            long overlaps = currentRanges.stream().
                    filter(existing -> existing.overlaps(dateTimeRange)).
                    filter(existing -> !existing.equals(dateTimeRange)).
                    count();
            if (overlaps>0) {
                throw new RuntimeException("For Station " +station.getId()+ " found overlap between " + dateTimeRange +
                        " and existing " + currentRanges);
            }
        } else {
            diversions.put(station, new HashSet<>());
        }
        diversions.get(station).add(dateTimeRange);
    }

    public void close() {
        diversions.clear();
    }

    public void set(final Station station, final Set<DateTimeRange> ranges) {
        diversions.put(station, new HashSet<>(ranges));
    }
}
