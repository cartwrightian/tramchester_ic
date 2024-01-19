package com.tramchester.livedata.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.openLdb.TrainDeparturesRepository;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class DeparturesRepository {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesRepository.class);
    public static final Duration TRAM_WINDOW = Duration.ofMinutes(20);
    public static final Duration TRAIN_WINDOW = Duration.ofMinutes(60);
    public static final Duration BUS_WINDOW = Duration.ofMinutes(30);
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(45);

    private final StationLocationsRepository stationLocationsRepository;
    private final TramDepartureRepository tramDepartureRepository;
    private final TrainDeparturesRepository trainDeparturesRepository;
    private final TramchesterConfig config;

    @Inject
    public DeparturesRepository(StationLocationsRepository stationLocationsRepository,
                                TramDepartureRepository tramDepartureRepository, TrainDeparturesRepository trainDeparturesRepository, TramchesterConfig config) {
        this.stationLocationsRepository = stationLocationsRepository;
        this.tramDepartureRepository = tramDepartureRepository;
        this.trainDeparturesRepository = trainDeparturesRepository;
        this.config = config;
    }

    public List<UpcomingDeparture> getDueForLocation(final Location<?> location, final LocalDate date, final TramTime time,
                                                     final EnumSet<TransportMode> modes) {
        logger.info(format("Get due %s services at %s for %s %s", modes, location.getId(), date, time));
        List<UpcomingDeparture> departures = switch (location.getLocationType()) {
            case Station -> getStationDepartures((Station) location, modes);
            case StationGroup -> getStationGroupDepartures((StationGroup) location, modes);
            case MyLocation, Postcode -> getDeparturesNearTo(location, modes);
            case Platform -> getPlatformDepartures((Platform) location, modes);
        };

        return departures.stream().
                filter(departure -> departure.getDate().equals(date)).
                filter(departure -> isTimely(time, departure)).
                collect(Collectors.toList());
    }

    private boolean isTimely(final TramTime time, final UpcomingDeparture departure) {
        final Duration windowSize = getLiveDeparturesWindowFor(departure.getMode());
        final TimeRange timeRange = TimeRange.of(time, windowSize, windowSize);
        return timeRange.contains(departure.getWhen());
    }

    private Duration getLiveDeparturesWindowFor(TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAM_WINDOW;
            case Train -> TRAIN_WINDOW;
            case Bus -> BUS_WINDOW;
            default -> DEFAULT_WINDOW;
        };
    }

    private List<UpcomingDeparture> getPlatformDepartures(final Platform platform, final EnumSet<TransportMode> modes) {
        if (!TransportMode.intersects(modes, platform.getTransportModes())) {
            logger.error(format("Platform %s does not match supplied modes %s", platform, modes));
            return Collections.emptyList();
        }
        return tramDepartureRepository.forStation(platform.getStation()).
                stream().
                filter(UpcomingDeparture::hasPlatform).
                filter(departure -> departure.getPlatform().equals(platform)).
                toList();
    }

    private List<UpcomingDeparture> getDeparturesNearTo(final Location<?> location, final EnumSet<TransportMode> modes) {
        final MarginInMeters margin = MarginInMeters.of(config.getNearestStopRangeKM());
        final int numOfNearestStopsToOffer = config.getNumOfNearestStopsToOffer();

        List<Station> nearbyStations = stationLocationsRepository.nearestStationsSorted(location, numOfNearestStopsToOffer,
                margin, modes);

        return nearbyStations.stream().
                flatMap(station -> getStationDepartures(station, modes).stream()).
                distinct().
                collect(Collectors.toList());
    }

    private List<UpcomingDeparture> getStationGroupDepartures(final StationGroup stationGroup,  final EnumSet<TransportMode> modes) {
        return stationGroup.getAllContained().stream().
                filter(station -> TransportMode.intersects(station.getTransportModes(), modes)).
                flatMap(station -> getStationDepartures(station, modes).stream()).
                distinct().collect(Collectors.toList());

    }

    private List<UpcomingDeparture> getStationDepartures(final Station station, final EnumSet<TransportMode> modes) {
        final SetUtils.SetView<TransportMode> toFetch = SetUtils.intersection(station.getTransportModes(), modes);

        if (toFetch.isEmpty()) {
            logger.error(format("Station modes %s and filter modes %s do not overlap", station, modes));
        }

        return toFetch.stream().
                flatMap(mode -> getDeparturesFor(mode, station)).
                collect(Collectors.toList());
    }

    private Stream<UpcomingDeparture> getDeparturesFor(TransportMode mode, Station station) {
        switch (mode) {
            case Tram -> {
                return tramDepartureRepository.forStation(station).stream();
            }
            case Train -> {
                return trainDeparturesRepository.forStation(station).stream();
            }
            default -> {
                final String msg = "TODO - live data for " + mode + " is not implemented yet";
                logger.error(msg);
                return Stream.empty(); }
        }
    }
}
