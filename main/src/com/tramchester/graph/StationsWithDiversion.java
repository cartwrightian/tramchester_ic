package com.tramchester.graph;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.dates.DateTimeRange;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationsWithDiversionRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@LazySingleton
public class StationsWithDiversion implements StationsWithDiversionRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationsWithDiversion.class);

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
    public Set<DateTimeRange> getDateTimeRangesFor(final Station station) {
        if (!diversions.containsKey(station)) {
            logger.warn("Tried to get date ranges for " + station.getId() + " but none present");
            return Collections.emptySet();
        }
        return diversions.get(station);
    }

    public void add(final Station station, final DateTimeRange dateTimeRange) {
        if (diversions.containsKey(station)) {
            final Set<DateTimeRange> currentRanges = diversions.get(station);
            final long overlapButNotSame = currentRanges.stream().
                    filter(existing -> existing.overlaps(dateTimeRange)).
                    filter(existing -> !existing.equals(dateTimeRange)).
                    count();
            if (overlapButNotSame>0) {
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
