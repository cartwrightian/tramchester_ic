package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.closures.ClosedStation;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;


@LazySingleton
public class StationAvailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationAvailabilityRepository.class);

    // TODO Need to tidy up handling of different locaiton types here, it is messy and inconsistent

    // NOTE: use routes here since they tend to have specific times ranges, whereas services end up 24x7 some stations
    private final Map<Location<?>, ServedRoute> pickupsForLocation;
    private final Map<Location<?>, ServedRoute> dropoffsForLocation;
    private final Map<Location<?>, Set<Service>> servicesForLocation;

    private final StationRepository stationRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final GraphFilterActive graphFilterActive;
    private final TripRepository tripRepository;


    @Inject
    public StationAvailabilityRepository(StationRepository stationRepository, ClosedStationsRepository closedStationsRepository,
                                         GraphFilterActive graphFilterActive, TripRepository tripRepository) {
        this.stationRepository = stationRepository;
        this.closedStationsRepository = closedStationsRepository;
        this.graphFilterActive = graphFilterActive;
        this.tripRepository = tripRepository;
        pickupsForLocation = new HashMap<>();
        dropoffsForLocation = new HashMap<>();

        servicesForLocation = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");

        Set<Station> stations = stationRepository.getStations();
        logger.info("Add pickup and dropoff for stations");
        stations.forEach(this::addForStation);

        addServicesForStations();

        logger.info(format("started, from %s stations add entries %s for pickups and %s for dropoff",
                stations.size(), pickupsForLocation.size(), dropoffsForLocation.size()));
    }

    private void addServicesForStations() {
        logger.info("Add services for stations");
        stationRepository.getStations().forEach(station -> servicesForLocation.put(station, new HashSet<>()));

        tripRepository.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                filter(StopCall::callsAtStation).
                forEach(stopCall -> servicesForLocation.get(stopCall.getStation()).add(stopCall.getService()));
    }

    private void addForStation(final Station station) {
        final boolean diagnostic = !graphFilterActive.isActive() && logger.isDebugEnabled();

        if (!addFor(dropoffsForLocation, station, station.getDropoffRoutes(), StopCall::getArrivalTime)) {
            if (diagnostic) {
                logger.debug("No dropoffs for " + station.getId());
            }
            dropoffsForLocation.put(station, new ServedRoute()); // empty
        }
        if (!addFor(pickupsForLocation, station, station.getPickupRoutes(), StopCall::getDepartureTime)) {
            if (diagnostic) {
                logger.debug("No pickups for " + station.getId());
            }
            pickupsForLocation.put(station, new ServedRoute()); // empty
        }
    }

    private boolean addFor(final Map<Location<?>, ServedRoute> forLocations, final Station station, final Set<Route> routes,
                           final Function<StopCall, TramTime> getTime) {

        final Stream<Trip> callingTrips = routes.stream().
                filter(route -> station.servesRoutePickup(route) || station.servesRouteDropOff(route)).
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> trip.callsAt(station.getId()));

        final Stream<StopCall> stationStopCalls = callingTrips.
                map(Trip::getStopCalls).
                map(stopCalls -> stopCalls.getStopFor(station.getId())).
                filter(StopCall::callsAtStation);

        return stationStopCalls.
                map(stopCall -> addFor(forLocations, station, stopCall, getTime)).reduce(false, (a,b) -> a || b);

    }

    private boolean addFor(final Map<Location<?>, ServedRoute> forLocations, final Station station, final StopCall stopCall,
                       final Function<StopCall, TramTime> getTime) {
        final TramTime time = getTime.apply(stopCall);
        if (!time.isValid()) {
            logger.warn(format("Invalid time %s for %s %s", time, station.getId(), stopCall));
            return false;
        }
        addFor(forLocations, station, stopCall.getTrip().getRoute(), stopCall.getService(), time);
        return true;
    }

    private void addFor(final Map<Location<?>, ServedRoute> forLocations, final Station station, final Route route,
                        final Service service, final TramTime time) {
        if (!forLocations.containsKey(station)) {
            forLocations.put(station, new ServedRoute());
        }
        forLocations.get(station).add(route, service, time);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        pickupsForLocation.clear();
        dropoffsForLocation.clear();
        servicesForLocation.clear();
        logger.info("Stopped");
    }

    public boolean isAvailable(final Location<?> location, final TramDate date, final TimeRange timeRange,
                               final EnumSet<TransportMode> requestedModes) {

        if (location.getLocationType()==LocationType.StationGroup) {
            final StationGroup stationGroup = (StationGroup) location;
            return isGroupAvailable(stationGroup, date, timeRange, requestedModes);
        }

        if (!pickupsForLocation.containsKey(location)) {
            throw new RuntimeException("Missing pickups for " + location.getId());
        }
        if (!dropoffsForLocation.containsKey(location)) {
            throw new RuntimeException("Missing dropoffs for " + location.getId());
        }

        final Set<Service> services = servicesForLocation.get(location).stream().
                filter(service -> TransportMode.intersects(requestedModes, service.getTransportModes())).
                collect(Collectors.toSet());

        if (services.isEmpty()) {
            logger.warn("Found no services for " + location.getId() + " and " + requestedModes);
            return false;
        }

        // TODO is this worth it?
        final boolean onDate = services.stream().anyMatch(service -> service.getCalendar().operatesOn(date));
        if (!onDate) {
            return false;
        }

        return pickupsForLocation.get(location).anyAvailable(date, timeRange, requestedModes) &&
                dropoffsForLocation.get(location).anyAvailable(date, timeRange, requestedModes);
    }

    private boolean isGroupAvailable(final StationGroup stationGroup, final TramDate date, final TimeRange timeRange, final EnumSet<TransportMode> requestedModes) {
        return stationGroup.getAllContained().stream().anyMatch(station -> isAvailable(station, date, timeRange, requestedModes));
    }

    public Set<Route> getPickupRoutesFor(final Location<?> location, final TramDate date, final TimeRange timeRange, final EnumSet<TransportMode> modes) {
        if (location.getLocationType()==LocationType.StationGroup) {
            final StationGroup stationGroup = (StationGroup) location;
            return getPickupRoutesForGroup(stationGroup, date, timeRange, modes);
        }
        if (closedStationsRepository.isClosed(location, date)) {
            // include diversions around a close station
            final ClosedStation closedStation = closedStationsRepository.getClosedStation(location, date, timeRange);
            return getPickupRoutesFor(closedStation, date, timeRange, modes);
        }
        if (!pickupsForLocation.containsKey(location)) {
            throw new RuntimeException("No pickups for " + location.getId());
        }
        return pickupsForLocation.get(location).getRoutes(date, timeRange, modes);
    }

    private Set<Route> getPickupRoutesForGroup(final StationGroup stationGroup, final TramDate date, final TimeRange timeRange, final EnumSet<TransportMode> modes) {
        return stationGroup.getAllContained().stream().
                flatMap(station -> getPickupRoutesFor(station, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getDropoffRoutesFor(final Location<?> location, final TramDate date, final TimeRange timeRange, final EnumSet<TransportMode> modes) {
        if (location.getLocationType()==LocationType.StationGroup) {
            final StationGroup stationGroup = (StationGroup) location;
            return getDropoffRoutesForGroup(stationGroup, date, timeRange, modes);
        }
        if (closedStationsRepository.isClosed(location, date, timeRange)) {
            final ClosedStation closedStation = closedStationsRepository.getClosedStation(location, date, timeRange);
            // include diversions around closed station
            return getDropoffRoutesFor(closedStation, date,timeRange, modes);
        }
        if (!dropoffsForLocation.containsKey(location)) {
            throw new RuntimeException("No dropoffs for " + location.getId());
        }
        return dropoffsForLocation.get(location).getRoutes(date, timeRange, modes);
    }

    private Set<Route> getDropoffRoutesForGroup(StationGroup stationGroup, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        return stationGroup.getAllContained().stream().
                flatMap(station -> getDropoffRoutesFor(station, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    private Set<Route> getDropoffRoutesFor(ClosedStation closedStation, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        logger.warn(closedStation.getStationId() + " is closed, using linked stations for dropoffs");
        return closedStation.getDiversionAroundClosure().stream().
                flatMap(linked -> dropoffsForLocation.get(linked).getRoutes(date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getPickupRoutesFor(final LocationSet<Station> locations, final TramDate date, final TimeRange timeRange, final EnumSet<TransportMode> modes) {
        return locations.stream().
                flatMap(location -> getPickupRoutesFor(location, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    private Set<Route> getPickupRoutesFor(ClosedStation closedStation, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        logger.warn(closedStation.getStationId() + " is closed, using linked stations for pickups");
        return closedStation.getDiversionAroundClosure().stream().
                flatMap(linked -> pickupsForLocation.get(linked).getRoutes(date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getDropoffRoutesFor(LocationSet<Station> locations, TramDate date, TimeRange timeRange, EnumSet<TransportMode> modes) {
        return locations.stream().
                flatMap(location -> getDropoffRoutesFor(location, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public long size() {
        return pickupsForLocation.size() + dropoffsForLocation.size();
    }

    public TimeRange getAvailableTimesFor(final LocationCollection destinations, final TramDate tramDate) {

        final EnumSet<TransportMode> modes = destinations.getModes();

        final Set<TimeRange> ranges = destinations.locationStream().
                flatMap(location -> expand(location).locationStream()).
                filter(dropoffsForLocation::containsKey).
                map(dropoffsForLocation::get).
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

    private LocationCollection expand(Location<?> location) {
        if (location.getLocationType()==LocationType.Station) {
            return MixedLocationSet.singleton(location);
        }
        if (location.getLocationType()==LocationType.StationGroup) {
            StationGroup group = (StationGroup) location;
            return group.getAllContained();
        }
        throw new RuntimeException("Unsupported location type " + location.getId() + " " + location.getLocationType());
    }
}
