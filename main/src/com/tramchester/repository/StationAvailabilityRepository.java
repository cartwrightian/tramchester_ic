package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;


@LazySingleton
public class StationAvailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationAvailabilityRepository.class);

    // TODO Need to tidy up handling of different location types here, it is messy and inconsistent

    // NOTE: use routes here since they tend to have specific times ranges, whereas services end up 24x7 some stations
    private final Map<LocationId<?>, ServedRoute> pickupsForLocations;
    private final Map<LocationId<?>, ServedRoute> dropoffsForLocations;

    private final Map<LocationId<?>, Set<Service>> servicesForLocations;

    private final StationRepository stationRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final GraphFilterActive graphFilterActive;
    private final TripRepository tripRepository;
    private final InterchangeRepository interchangeRepository;


    @Inject
    public StationAvailabilityRepository(StationRepository stationRepository, ClosedStationsRepository closedStationsRepository,
                                         GraphFilterActive graphFilterActive, TripRepository tripRepository,
                                         InterchangeRepository interchangeRepository) {
        this.stationRepository = stationRepository;
        this.closedStationsRepository = closedStationsRepository;
        this.graphFilterActive = graphFilterActive;
        this.tripRepository = tripRepository;
        this.interchangeRepository = interchangeRepository;

        pickupsForLocations = new HashMap<>();
        dropoffsForLocations = new HashMap<>();
        servicesForLocations = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");

        final Set<Station> stations = stationRepository.getStations();
        logger.info("Add pickup and dropoff for stations");
        stations.forEach(this::addForStation);

        addServicesForStations();

        logger.info(format("started, from %s stations add entries %s for pickups and %s for dropoff",
                stations.size(), pickupsForLocations.size(), dropoffsForLocations.size()));
    }

    private void addServicesForStations() {
        logger.info("Add services for stations");
        stationRepository.getStations().forEach(station -> servicesForLocations.put(station.getLocationId(), new HashSet<>()));

        // TODO is this incorrect in the case of interchange locations ??
        tripRepository.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                filter(StopCall::callsAtStation).
                forEach(stopCall -> servicesForLocations.get(stopCall.getLocationId()).add(stopCall.getService()));
    }

    private void addForStation(final Station station) {
        final boolean diagnostic = !graphFilterActive.isActive() && logger.isDebugEnabled();

        if (interchangeRepository.isInterchange(station)) {
            final InterchangeStation interchangeStation = interchangeRepository.getInterchange(station);
            if (interchangeStation.isMultiMode()) {
                addInterchangeStation(interchangeStation, diagnostic);
                return;
            }
        }

        addStation(station, diagnostic);
    }

    private void addInterchangeStation(final InterchangeStation interchangeStation, final boolean diagnostic) {
        final LocationId<?> locationId = interchangeStation.getLocationId();
        final ServedRoute dropOffsAtLocation = getDropOffsFor(locationId);
        if (!dropOffsAtLocation.addFor(interchangeStation, interchangeStation.getDropoffRoutes(), StopCall::getArrivalTime)) {
            if (diagnostic) {
                logger.debug("No dropoffs for interchange " + interchangeStation.getId());
            }
        }

        final ServedRoute pickupsAtLocation = getPickupsFor(locationId);
        if (!pickupsAtLocation.addFor(interchangeStation, interchangeStation.getPickupRoutes(), StopCall::getDepartureTime)) {
            if (diagnostic) {
                logger.debug("No pickups for interchange " + interchangeStation.getId());
            }
        }

    }

    private void addStation(final Station station, final boolean diagnostic) {
        final LocationId<Station> locationId = station.getLocationId();
        final ServedRoute dropOffsAtLocation = getDropOffsFor(locationId);
        if (!dropOffsAtLocation.addFor(station.getId(), station.getDropoffRoutes(), StopCall::getArrivalTime)) {
            if (diagnostic) {
                logger.debug("No dropoffs for station " + station.getId());
            }
        }

        final ServedRoute pickupsAtLocation = getPickupsFor(locationId);
        if (!pickupsAtLocation.addFor(station.getId(), station.getPickupRoutes(), StopCall::getDepartureTime)) {
            if (diagnostic) {
                logger.debug("No pikcups for station " + station.getId());
            }
        }

    }

    private ServedRoute getPickupsFor(final LocationId<?> locationId) {
        final ServedRoute pickupsAtLocation;
        if (pickupsForLocations.containsKey(locationId)) {
            pickupsAtLocation = dropoffsForLocations.get(locationId);
        } else {
            pickupsAtLocation = new ServedRoute(locationId);
            pickupsForLocations.put(locationId, pickupsAtLocation);
        }
        return pickupsAtLocation;
    }

    private ServedRoute getDropOffsFor(final LocationId<?> locationId) {
        final ServedRoute dropOffsAtLocation;
        if (dropoffsForLocations.containsKey(locationId)) {
            dropOffsAtLocation = dropoffsForLocations.get(locationId);
        } else {
            dropOffsAtLocation = new ServedRoute(locationId);
            dropoffsForLocations.put(locationId, dropOffsAtLocation);
        }
        return dropOffsAtLocation;
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        pickupsForLocations.clear();
        dropoffsForLocations.clear();
        servicesForLocations.clear();
        logger.info("Stopped");
    }

    public boolean isAvailable(final Location<?> location, final TramDate date, final TimeRange timeRange,
                               final ImmutableEnumSet<TransportMode> requestedModes) {
        return isAvailable(location, date, timeRange, requestedModes, true);
    }

    public boolean isAvailablePickups(final Location<?> location, final TramDate date, final TimeRange timeRange,
                               final ImmutableEnumSet<TransportMode> requestedModes) {
        return isAvailable(location, date, timeRange, requestedModes, false);
    }

    public boolean isAvailable(final Location<?> location, final TramDate date, final TimeRange timeRange,
                               final ImmutableEnumSet<TransportMode> requestedModes, boolean requireDropoff) {

        if (location.getLocationType()==LocationType.StationGroup) {
            final StationLocalityGroup stationGroup = (StationLocalityGroup) location;
            return isGroupAvailable(stationGroup, date, timeRange, requestedModes);
        }

        final LocationId<?> locationId = location.getLocationId();

        if (!location.anyOverlapWith(requestedModes)) {
            if (logger.isDebugEnabled()) {
                logger.debug(locationId + " no overlap between requested " + requestedModes + " and " + location.getTransportModes());
            }
            return false;
        }

        if (!pickupsForLocations.containsKey(locationId)) {
            throw new RuntimeException("Missing pickups for " + locationId);
        }
        if (!dropoffsForLocations.containsKey(locationId)) {
            throw new RuntimeException("Missing dropoffs for " + locationId);
        }

        final Set<Service> services = servicesForLocations.get(locationId);

        boolean modesMatch = services.stream().
                anyMatch(service -> service.anyOverlapWith(requestedModes));

        if (!modesMatch) {
            logger.warn("No services modes overlap for " + locationId + " and " + requestedModes);
        }

        boolean datesMatch = services.stream().
                anyMatch(service -> service.getCalendar().operatesOn(date));

        if (!datesMatch) {
            logger.warn("No services date overlap for " + locationId);
        }

        final ServedRoute pickupsAtLocation = pickupsForLocations.get(locationId);

        if (requireDropoff) {
            final ServedRoute dropoffsAtLocation = dropoffsForLocations.get(locationId);
            return pickupsAtLocation.anyAvailable(date, timeRange, requestedModes) &&
                    dropoffsAtLocation.anyAvailable(date, timeRange, requestedModes);
        } else {
            return pickupsAtLocation.anyAvailable(date, timeRange, requestedModes);
        }

    }

    private boolean isGroupAvailable(final StationLocalityGroup stationGroup, final TramDate date, final TimeRange timeRange,
                                     final ImmutableEnumSet<TransportMode> requestedModes) {
        return stationGroup.getAllContained().stream().anyMatch(station -> isAvailable(station, date, timeRange, requestedModes));
    }

    public Set<Route> getPickupRoutesFor(final Location<?> location, final TramDate date, final TimeRange timeRange,
                                         final ImmutableEnumSet<TransportMode> modes) {
        final LocationId<?> locationId = location.getLocationId();

        if (location.getLocationType()==LocationType.StationGroup) {
            final StationLocalityGroup stationGroup = (StationLocalityGroup) location;
            return getPickupRoutesForGroup(stationGroup, date, timeRange, modes);
        }

        if (closedStationsRepository.isClosed(location, date)) {
            // include diversions around a close station
            final ClosedStation closedStation = closedStationsRepository.getClosedStation(location, date, timeRange);
            return getPickupRoutesFor(closedStation, date, timeRange, modes);
        }

        if (!pickupsForLocations.containsKey(locationId)) {
            // todo maybe into the none interchance case below?
            throw new RuntimeException("No pickups for " + locationId);
        }

        final ServedRoute servedRoute = pickupsForLocations.get(locationId);
        return servedRoute.getRoutes(date, timeRange, modes);

    }

    private Set<Route> getPickupRoutesForGroup(final StationLocalityGroup stationGroup, final TramDate date, final TimeRange timeRange,
                                               final ImmutableEnumSet<TransportMode> modes) {
        return stationGroup.getAllContained().stream().
                flatMap(station -> getPickupRoutesFor(station, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getDropoffRoutesFor(final Location<?> location, final TramDate date, final TimeRange timeRange,
                                          final ImmutableEnumSet<TransportMode> modes) {

        // TODO find way to share logic on station groups, interchanges etc between pickup and dropoff

        if (location.getLocationType()==LocationType.StationGroup) {
            final StationLocalityGroup stationGroup = (StationLocalityGroup) location;
            return getDropoffRoutesForGroup(stationGroup, date, timeRange, modes);
        }
        if (closedStationsRepository.isClosed(location, date, timeRange)) {
            final ClosedStation closedStation = closedStationsRepository.getClosedStation(location, date, timeRange);
            // include diversions around closed station
            return getDropoffRoutesFor(closedStation, date,timeRange, modes);
        }

        final LocationId<?> locationId = location.getLocationId();

        if (!dropoffsForLocations.containsKey(locationId)) {
            throw new RuntimeException("No dropoffs for " + locationId);
        }

        return dropoffsForLocations.get(locationId).getRoutes(date, timeRange, modes);
    }

    private Set<Route> getDropoffRoutesForGroup(StationLocalityGroup stationGroup, TramDate date, TimeRange timeRange, ImmutableEnumSet<TransportMode> modes) {
        return stationGroup.getAllContained().stream().
                flatMap(station -> getDropoffRoutesFor(station, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    private Set<Route> getDropoffRoutesFor(ClosedStation closedStation, TramDate date, TimeRange timeRange, ImmutableEnumSet<TransportMode> modes) {
        logger.warn(closedStation.getStationId() + " is closed, using linked stations for dropoffs");
        return closedStation.getDiversionAroundClosure().stream().
                flatMap(linked -> dropoffsForLocations.get(linked.getLocationId()).getRoutes(date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getPickupRoutesFor(final LocationSet<Station> locations, final TramDate date, final TimeRange timeRange,
                                         final ImmutableEnumSet<TransportMode> modes) {
        return locations.stream().
                flatMap(location -> getPickupRoutesFor(location, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    private Set<Route> getPickupRoutesFor(ClosedStation closedStation, TramDate date, TimeRange timeRange, ImmutableEnumSet<TransportMode> modes) {
        logger.warn(closedStation.getStationId() + " is closed, using linked stations for pickups");
        return closedStation.getDiversionAroundClosure().stream().
                flatMap(linked -> pickupsForLocations.get(linked.getLocationId()).getRoutes(date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getDropoffRoutesFor(LocationSet<Station> locations, TramDate date, TimeRange timeRange, ImmutableEnumSet<TransportMode> modes) {
        return locations.stream().
                flatMap(location -> getDropoffRoutesFor(location, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public long size() {
        return pickupsForLocations.size() + dropoffsForLocations.size();
    }

    public ServedRoute getServedPickUpRouteFor(final LocationId<?> location) {
        return dropoffsForLocations.get(location);
    }

    public TimeRange getAvailableTimesFor(final LocationCollection destinations, final TramDate tramDate) {

        final ImmutableEnumSet<TransportMode> modes = destinations.getModes();

        Stream<ServedRoute> servedRouteStream = destinations.locationStream().
                flatMap(locations -> expand(locations).locationStream()).
                map(Location::getLocationId).
                filter(dropoffsForLocations::containsKey).
                map(dropoffsForLocations::get);

        final Set<TimeRange> ranges = servedRouteStream.
                flatMap(servedRoute -> servedRoute.getTimeRanges(tramDate, modes)).
                collect(Collectors.toSet());

        if (ranges.isEmpty()) {
            // likely down to closed station(s)
            logger.warn("Found no time range available for " + destinations + " (Closed stations?) Will use whole day");
            return TimeRange.of(TramTime.of(0,1), TramTime.of(23,59));
        } else {
            // now create timerange
            return TimeRange.coveringAllOf(ranges);
        }

    }

    private LocationCollection expand(final Location<?> location) {
        if (location.getLocationType()==LocationType.Station) {
            return LocationCollectionSingleton.of(location);
        }
        if (location.getLocationType()==LocationType.StationGroup) {
            final StationLocalityGroup group = (StationLocalityGroup) location;
            return group.getAllContained();
        }
        throw new RuntimeException("Unsupported location type " + location.getId() + " " + location.getLocationType());
    }
}
