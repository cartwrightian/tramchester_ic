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
import java.util.stream.Stream;

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

    public List<TramTime> generate(final TramTime initialQueryTime, final Location<?> location, final TramDate date, final ImmutableEnumSet<TransportMode> modes) {

        Stream<TramTime> durations = IntStream.range(0, numberQueries).
                mapToObj(index -> initialQueryTime.plusMinutes(index * interval)).
                filter(time -> availabilityRepository.isAvailablePickups(location, date, rangeFor(time), modes));

        List<TramTime> result = durations.toList();

        if (result.isEmpty()) {
            logger.error("No query times for " + initialQueryTime + " " + location.getId() + " " + date + " " + modes);
        } else {
            logger.info("Query times " + result);
        }
        return result;
    }

    private TimeRange rangeFor(final TramTime time) {
        return TimeRange.of(time, time.plusMinutes(maxWaitMins));
    }

    public List<TramTime> generate(TramTime initialQueryTime, Set<StationWalk> stationWalks, TramDate date, ImmutableEnumSet<TransportMode> modes) {

        Stream<TramTime> durations = IntStream.range(0, numberQueries).
                mapToObj(index -> initialQueryTime.plusMinutes(index * interval)).
                filter( time -> available(stationWalks, date, time, modes));

        List<TramTime> result = durations.toList();

        if (result.isEmpty()) {
            logger.error("No query times for " + initialQueryTime + " " + stationWalks + " " + date + " " + modes);
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
