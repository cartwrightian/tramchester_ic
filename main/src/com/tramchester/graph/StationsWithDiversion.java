package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationsWithDiversionRepository;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@LazySingleton
public class StationsWithDiversion implements StationsWithDiversionRepository {
    private final Map<Station, Set<DateRange>> diversions;

    @Inject
    public StationsWithDiversion() {
        diversions = new HashMap<>();
    }

    @Override
    public boolean hasDiversions(final Station station) {
        return diversions.containsKey(station);
    }

    @Override
    public Set<DateRange> getDateRangesFor(final Station station) {
        return diversions.get(station);
    }

    public void add(final Station station, final DateRange dateRange) {
        if (diversions.containsKey(station)) {
            final Set<DateRange> currentRanges = diversions.get(station);
            long overlaps = currentRanges.stream().filter(existing -> existing.overlapsWith(dateRange)).
                    filter(existing -> !existing.equals(dateRange)).
                    count();
            if (overlaps>0) {
                throw new RuntimeException("For Station " +station.getId()+ " found overlap between " + dateRange + " and existing " + currentRanges);
            }
        } else {
            diversions.put(station, new HashSet<>());
        }
        diversions.get(station).add(dateRange);
    }

    public void close() {
        diversions.clear();
    }

    public void add(final Station station, final Set<DateRange> ranges) {
        diversions.put(station, new HashSet<>(ranges));
    }
}
