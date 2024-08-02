package com.tramchester.domain.closures;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class ClosedStationFactory {
    private static final Logger logger = LoggerFactory.getLogger(ClosedStationFactory.class);

    private final TramchesterConfig config;
    private final StationRepository stationRepository;
    private final StationLocations stationLocations;

    @Inject
    public ClosedStationFactory(TramchesterConfig config, StationRepository stationRepository, StationLocations stationLocations) {
        this.config = config;
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;
    }

    public ClosedStation createClosedStation(final StationClosures closure, final IdFor<Station> stationId,
                                             final ShouldIncludeStationInDiversions includeDiversion) {
        final Station station = stationRepository.getStationById(stationId);
        final DateRange dateRange = closure.getDateRange();

        final Set<Station> nearbyOpenStations = getNearbyOpenStations(station, includeDiversion);

        final boolean fullyClosed = closure.isFullyClosed();

        final Set<Station> diversionsAround;
        if (closure.hasDiversionsAroundClosure()) {
            final IdSet<Station> diversionsAroundClosure = closure.getDiversionsAroundClosure();
            logger.info(format("Using config provided stations %s for diversions around closed station %s", diversionsAroundClosure, stationId));
            diversionsAround = getStations(diversionsAroundClosure);
        } else {
            logger.info(format("Using discovered nearby stations %s for diversions around closed station %s", HasId.asIds(nearbyOpenStations), stationId));
            diversionsAround = nearbyOpenStations;
        }

        final Set<Station> diversionsToFrom;
        if (closure.hasDiversionsToFromClosure()) {
            final IdSet<Station> diversionsToFromClosure = closure.getDiversionsToFromClosure();
            logger.info(format("Using config provided stations %s for diversions to/from closed station %s", diversionsToFromClosure, stationId));
            diversionsToFrom = getStations(diversionsToFromClosure);
        } else {
            logger.info(format("Using discovered nearby stations %s for diversions to/from closed station %s", HasId.asIds(nearbyOpenStations), stationId));
            diversionsToFrom = nearbyOpenStations;
        }

        if (closure.hasTimeRange()) {
            return new ClosedStation(station, dateRange, closure.getTimeRange(), fullyClosed, diversionsAround, diversionsToFrom);
        } else {
            return new ClosedStation(station, dateRange, fullyClosed, diversionsAround, diversionsToFrom);
        }
    }

    private Set<Station> getStations(IdSet<Station> stationIds) {
        return stationIds.stream().map(stationRepository::getStationById).collect(Collectors.toSet());
    }

    private Set<Station> getNearbyOpenStations(final Station closedStation, ShouldIncludeStationInDiversions includeDiversion) {
        final MarginInMeters margin = config.getWalkingDistanceRange();

        final Set<Station> withinRange = stationLocations.nearestStationsUnsorted(closedStation, margin).collect(Collectors.toSet());

        final Set<Station> found = withinRange.stream().
                filter(nearby -> !nearby.equals(closedStation)).
                filter(includeDiversion::include).
                collect(Collectors.toSet());

        logger.debug(String.format("Found %s open stations (out of %s) within range of %s",
                found.size() ,  withinRange.size(), closedStation.getId()));

        return found;
    }

    public interface ShouldIncludeStationInDiversions {
        boolean include(Station diversionStation);
    }

}
