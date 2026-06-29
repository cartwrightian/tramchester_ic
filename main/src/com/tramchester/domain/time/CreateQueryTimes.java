package com.tramchester.domain.time;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.StationWalk;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.StationAvailabilityRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@LazySingleton
public class CreateQueryTimes {
    private static final Logger logger = LoggerFactory.getLogger(CreateQueryTimes.class);

    private final StationAvailabilityRepository availabilityRepository;
    private final int interval;
    private final int numberQueries;
    private final int maxWaitMins;

    @Inject
    public CreateQueryTimes(final TramchesterConfig config, final StationAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;

        interval = config.getQueryInterval();
        numberQueries = config.getNumberQueries();
        maxWaitMins = config.getMaxWait();
    }

    public List<TramTime> generate(final TramTime initialQueryTime, final Location<?> location, final TramDate date,
                                   final ImmutableEnumSet<TransportMode> modes) {

        final List<TimeRange> times = IntStream.range(0, numberQueries).
                mapToObj(index -> initialQueryTime.plusMinutes(index * interval)).
                map(this::rangeFor).
                toList();

        final List<TramTime> results;
        if (location.anyOverlapWith(modes)) {
            results = times.stream().
                    filter(range -> availabilityRepository.isAvailablePickups(location, date, range, modes)).
                    map(TimeRange::getStart).
                    toList();
        } else {
            logger.warn("No modes overlap between " + location.getId() + " " + location.getTransportModes() +
                "requested  " + modes);
            results = times.stream().map(TimeRange::getStart).toList();
        }

        if (results.isEmpty()) {
            logger.error("No available times for " + initialQueryTime + " location:" + location.getId() + " " + date + " " + modes +
                    " " + times);
        } else {
            logger.info("Query times " + results);
        }
        return results;
    }

    private TimeRange rangeFor(final TramTime time) {
        return TimeRange.of(time, time.plusMinutes(maxWaitMins));
    }

    public List<TramTime> generate(final TramTime initialQueryTime, final Set<StationWalk> stationWalks, final TramDate date,
                                   final ImmutableEnumSet<TransportMode> modes) {

        final boolean hasOverlap = stationWalks.stream().anyMatch(stationWalk -> stationWalk.getStation().anyOverlapWith(modes));

        final List<TramTime> times = IntStream.range(0, numberQueries).
                mapToObj(index -> initialQueryTime.plusMinutes(index * interval)).
                toList();

        final List<TramTime> result;
        if (hasOverlap) {
            result = times.stream().
                    filter(time -> available(stationWalks, date, time, modes)).
                    toList();
        } else {
            logger.warn("No modes overlap between " + stationWalks + " " + "requested  " + modes);
            result = times;
        }

        if (result.isEmpty()) {
            logger.error("No available times for " + initialQueryTime + " walks: " + stationWalks + " " + date + " " + modes +
                    " " + times);
        } else {
            logger.info("Query times " + result);
        }
        return result;
    }

    private boolean available(Set<StationWalk> stationWalks, TramDate date, TramTime time, ImmutableEnumSet<TransportMode> modes) {
        return stationWalks.stream().
                anyMatch(stationWalk -> availabilityRepository.isAvailablePickups(stationWalk.getStation(), date,
                        rangeFor(time.plus(stationWalk.getCost().truncateToMinutes())), modes));
    }

}
