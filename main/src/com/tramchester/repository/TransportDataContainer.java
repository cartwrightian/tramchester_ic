package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataContainer implements TransportData, WriteableTransportData {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataContainer.class);

    private final ProvidesNow providesNow;

    private final CompositeIdMap<Trip, MutableTrip> trips; // trip id -> trip
    private final CompositeIdMap<Station, MutableStation> stationsById;  // station id -> station
    private final CompositeIdMap<Service, MutableService> services;  // service id -> service
    private final CompositeIdMap<Route, MutableRoute> routes;  // route id -> route
    private final CompositeIdMap<Platform, MutablePlatform> platforms; // platformId -> platform
    private final IdMap<RouteStation> routeStations; // routeStationId - > RouteStation
    private final CompositeIdMap<Agency, MutableAgency> agencies; // agencyId -> agencies
    private final DataSourceInfoRepository dataSourceInfos;

    // data source name -> feedinfo (if present)
    private final Map<DataSourceID, DateRangeAndVersion> dateRangeAndVersionMap;
    private final String sourceName;

    /**
     * Not container managed due to test life cycle
     */
    public TransportDataContainer(ProvidesNow providesNow, String sourceName) {
        logger.info("Created for sourcename: " + sourceName);
        this.providesNow = providesNow;
        this.sourceName = sourceName;

        trips = new CompositeIdMap<>();
        stationsById = new CompositeIdMap<>();
        services = new CompositeIdMap<>();
        routes = new CompositeIdMap<>();
        platforms = new CompositeIdMap<>();
        routeStations = new IdMap<>();
        agencies = new CompositeIdMap<>();
        dataSourceInfos = new DataSourceInfoRepository(providesNow);
        dateRangeAndVersionMap = new HashMap<>();

    }

    // facilitates testing
    public static TransportDataContainer createUnmanagedCopy(TransportDataContainer dataContainer) {
        return new TransportDataContainer(dataContainer.providesNow,
                copyOf(dataContainer.trips),
                copyOf(dataContainer.stationsById),
                copyOf(dataContainer.services),
                copyOf(dataContainer.routes),
                copyOf(dataContainer.platforms),
                copyOf(dataContainer.routeStations),
                copyOf(dataContainer.agencies),
                new DataSourceInfoRepository(dataContainer.dataSourceInfos),
                copyOf(dataContainer.dateRangeAndVersionMap),
                dataContainer.sourceName);
    }

    private static Map<DataSourceID, DateRangeAndVersion> copyOf(Map<DataSourceID, DateRangeAndVersion> feedInfoMap) {
        return new HashMap<>(feedInfoMap);
    }

    private static IdMap<RouteStation> copyOf(IdMap<RouteStation> map) {
        return new IdMap<>(map.getValues());
    }

    private static <S extends HasId<S> & CoreDomain, T extends S> CompositeIdMap<S, T> copyOf(CompositeIdMap<S, T> map) {
        return new CompositeIdMap<>(map.getValues());
    }

    private TransportDataContainer(ProvidesNow providesNow, CompositeIdMap<Trip, MutableTrip> trips, CompositeIdMap<Station, MutableStation> stationsById,
                                   CompositeIdMap<Service, MutableService> services, CompositeIdMap<Route, MutableRoute> routes,
                                   CompositeIdMap<Platform, MutablePlatform> platforms, IdMap<RouteStation> routeStations,
                                   CompositeIdMap<Agency, MutableAgency> agencies, DataSourceInfoRepository dataSourceInfos,
                                   Map<DataSourceID, DateRangeAndVersion> dateRangeAndVersionMap, String sourceName) {
        this.providesNow = providesNow;
        this.trips = trips;
        this.stationsById = stationsById;
        this.services = services;
        this.routes = routes;
        this.platforms = platforms;
        this.routeStations = routeStations;
        this.agencies = agencies;
        this.dataSourceInfos = dataSourceInfos;
        this.dateRangeAndVersionMap = dateRangeAndVersionMap;
        this.sourceName = sourceName;
    }

    @Override
    public void dispose() {
        logger.info("stopping for " + sourceName);
        // clear's are here due to memory usage during testing
        trips.forEach(MutableTrip::dispose);
        trips.clear();
        stationsById.clear();
        services.clear();
        routes.clear();
        platforms.clear();
        routeStations.clear();
        agencies.clear();
        dateRangeAndVersionMap.clear();
        logger.info("stopped");
    }

    @Override
    public void reportNumbers() {
        logger.info("From " + dataSourceInfos + " name:" + sourceName);
        logger.info(format("%s agencies", agencies.size()));
        logger.info(format("%s routes", routes.size()));
        logger.info(stationsById.size() + " stations " + platforms.size() + " platforms ");
        logger.info(format("%s route stations", routeStations.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", trips.size()));
        logger.info(format("%s calling points", countStopCalls(trips)));
        logger.info(format("%s feedinfos", dateRangeAndVersionMap.size()));
    }

    private long countStopCalls(CompositeIdMap<Trip,MutableTrip> trips) {
        Optional<Long> count = trips.getValues().stream().
                map(trip -> trip.getStopCalls().numberOfCallingPoints()).
                reduce(Long::sum);
        return count.orElse(0L);
    }

    @Override
    public boolean hasStationId(IdFor<Station> stationId) {
        return stationsById.hasId(stationId);
    }

    @Override
    public Station getStationById(final IdFor<Station> stationId) {
        if (!stationsById.hasId(stationId)) {
            String msg = "Unable to find station from ID " + stationId;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationsById.get(stationId);
    }

    @Override
    public Set<Station> getStations() {
        return Collections.unmodifiableSet(stationsById.getValues());
    }

    @Override
    public Set<Station> getStations(EnumSet<TransportMode> modes) {
        return stationsById.getValuesStream().
                filter(station -> TransportMode.intersects(station.getTransportModes(), modes)).
                collect(Collectors.toSet());
    }

    @Override
    public Stream<Station> getActiveStationStream() {
        return stationsById.getValuesStream().filter(Location::isActive);
    }

    @Override
    public StationPair getStationPair(StationIdPair idPair) {
        return StationPair.of(getStationById(idPair.getBeginId()), getStationById(idPair.getEndId()));
    }

    @Override
    public Stream<Station> getAllStationStream() {
        return stationsById.getValuesStream();
    }

    @Override
    public Set<Station> getStationsServing(TransportMode mode) {
        return getStationsServingModeStream(mode).collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Station> getStationsServingModeStream(TransportMode mode) {
        return stationsById.filterStream(item -> item.servesMode(mode));
    }

    @Override
    public long getNumberOfStations(DataSourceID dataSourceID, TransportMode mode) {
        return stationsById.getValues().stream().
                filter(station -> station.getDataSourceID().equals(dataSourceID)).
                filter(station -> station.getTransportModes().contains(mode)).count();
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return Collections.unmodifiableSet(routeStations.getValues());
    }

    @Override
    public Set<RouteStation> getRouteStationsFor(IdFor<Station> stationId) {
        final Set<RouteStation> result = routeStations.getValuesStream().
                filter(routeStation -> routeStation.getStation().getId().equals(stationId)).
                collect(Collectors.toUnmodifiableSet());
        if (result.isEmpty()) {
            logger.warn("Found no route stations for " + stationId);
        }
        return result;
    }

    @Override
    public Stream<Station> getStationsFromSource(DataSourceID dataSourceID) {
        return this.stationsById.filterStream(station -> station.getDataSourceID()==dataSourceID);
    }

    @Override
    public RouteStation getRouteStationById(final IdFor<RouteStation> routeStationId) {
        final RouteStation routeStation = routeStations.get(routeStationId);
        if (routeStation==null) {
            logger.warn("Missing route station " + routeStationId);
        }
        return routeStation;
    }

    @Override
    public RouteStation getRouteStation(Station station, Route route) {
        return getRouteStationById(RouteStation.createId(station.getId(), route.getId()));
    }

    @Override
    public Set<Service> getServices() {
        return Collections.unmodifiableSet(services.getValues());
    }

    @Override
    public Set<Service> getServices(EnumSet<TransportMode> modes) {
        return services.getValuesStream().
                filter(service -> TransportMode.intersects(service.getTransportModes(), modes)).collect(Collectors.toSet());
    }

    @Override
    public Service getServiceById(IdFor<Service> serviceId) {
        return services.get(serviceId);
    }

    @Override
    public Set<Trip> getTrips() {
        return trips.getSuperValues();
    }

    @Override
    public Set<Trip> getTripsCallingAt(final Station station, final TramDate date) {
        return trips.filterStream(trip -> trip.callsAt(station.getId()) && trip.operatesOn(date)).collect(Collectors.toSet());
    }

    @Override
    public boolean hasRouteStationId(IdFor<RouteStation> routeStationId) {
        return routeStations.hasId(routeStationId);
    }

    @Override
    public void addRouteStation(final RouteStation routeStation) {
       routeStations.add(routeStation);
    }

    public MutablePlatform getMutablePlatform(IdFor<Platform> platformId) {
        return platforms.get(platformId);
    }

    @Override
    public Set<Platform> getPlatforms(final EnumSet<TransportMode> modes) {
        return platforms.getValuesStream().
                filter(platform -> TransportMode.intersects(modes, platform.getTransportModes())).collect(Collectors.toSet());
    }

    @Override
    public boolean hasPlatformId(final IdFor<Platform> id) {
        return platforms.hasId(id);
    }

    @Override
    public Route getRouteById(IdFor<Route> routeId) {
        return routes.get(routeId);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public Set<Agency> getAgencies() {
        return Collections.unmodifiableSet(agencies.getValues());
    }

    @Override
    public Agency get(IdFor<Agency> id) {
        return agencies.get(id);
    }

    @Override
    public IdFor<Agency> findByName(String name) {
        Optional<Agency> found = agencies.filterStream(item -> name.equals(item.getName())).findFirst();

        if (found.isPresent()) {
            return found.get().getId();
        } else {
            return IdFor.invalid(Agency.class);
        }
    }

    @Override
    public MutableService getMutableService(IdFor<Service> serviceId) {
        // logging into callers
        if (!services.hasId(serviceId)) {
            logger.debug("No such service " + serviceId);
        }
        return services.get(serviceId);
    }

    @Override
    public MutableRoute getMutableRoute(IdFor<Route> id) {
        return routes.get(id);
    }

    @Override
    public MutableAgency getMutableAgency(IdFor<Agency> agencyId) {
        return agencies.get(agencyId);
    }

    @Override
    public MutableTrip getMutableTrip(IdFor<Trip> tripId) {
        return trips.get(tripId);
    }

    @Override
    public Set<Service> getServicesWithoutCalendar() {
        return services.getValues().stream().filter(service -> !service.hasCalendar()).collect(Collectors.toSet());
    }

    @Override
    public IdSet<Service> getServicesWithZeroDays() {
        IdSet<Service> noDayServices = new IdSet<>();
        services.getValues().stream().filter(MutableService::hasCalendar).forEach(service -> {
                    ServiceCalendar calendar = service.getCalendar();
                    if (calendar.operatesNoDays()) {
                        // feedvalidator flags these as warnings also
                        noDayServices.add(service.getId());
                    }
                }
        );
        return noDayServices;
    }

    @Deprecated
    @Override
    public Set<DataSourceInfo> getDataSourceInfo() {
        return dataSourceInfos.getAll();
    }

    @Override
    public ZonedDateTime getNewestModTimeFor(final TransportMode mode) {
        return dataSourceInfos.getNewestModTimeFor(mode);
    }

    @Override
    public boolean hasDataSourceInfo() {
        return !dataSourceInfos.isEmpty();
    }

    @Override
    public String summariseDataSourceInfo() {
        return dataSourceInfos.toString();
    }

    @Override
    public DataSourceInfo getDataSourceInfo(DataSourceID dataSourceID) {
        return dataSourceInfos.get(dataSourceID);
    }

    @Override
    public boolean hasServiceId(IdFor<Service> serviceId) {
        return services.hasId(serviceId);
    }

    @Override
    public void addAgency(MutableAgency agency) {
        agencies.add(agency);
    }

    @Override
    public boolean hasAgencyId(IdFor<Agency> agencyId) {
        return agencies.hasId(agencyId);
    }

    @Override
    public void addRoute(MutableRoute route) {
        routes.add(route);
    }

    @Override
    public void addStation(MutableStation station) {
        stationsById.add(station);
    }

    @Override
    public void addPlatform(MutablePlatform platform) {
        platforms.add(platform);
    }

    @Override
    public void addService(MutableService service) {
        services.add(service);
    }

    @Override
    public boolean hasTripId(IdFor<Trip> tripId) {
        return trips.hasId(tripId);
    }

    @Override
    public Trip getTripById(IdFor<Trip> tripId) {
        if (tripId.isValid()) {
            return trips.get(tripId);
        } else {
            throw new RuntimeException("Cannot get trip for invalid tripId " + tripId);
        }
    }

    @Override
    public Set<Route> getRoutes() {
        return routes.getSuperValues();
    }

    @Override
    public Set<Route> getRoutes(Set<TransportMode> modes) {
        return routes.getValuesStream().filter(route -> modes.contains(route.getTransportMode())).collect(Collectors.toSet());
    }

    @Override
    public void addTrip(MutableTrip trip) {
        trips.add(trip);
    }

    @Override
    public Platform getPlatformById(IdFor<Platform> platformId) {
        return platforms.get(platformId);
    }

    @Override
    public Stream<Platform> getPlaformStream() {
        return platforms.getValuesStream();
    }

    @Override
    public Set<Service> getServicesOnDate(final TramDate date,  final EnumSet<TransportMode> modes) {
        return services.
                filterStream(service -> TransportMode.intersects(modes, service.getTransportModes()) && service.getCalendar().operatesOn(date)).
                collect(Collectors.toSet());
    }

    @Override
    public Set<Route> getRoutesRunningOn(TramDate date, EnumSet<TransportMode> modes) {
        return routes.filterStream(route -> route.isAvailableOn(date) && modes.contains(route.getTransportMode())).collect(Collectors.toSet());
    }

    public boolean hasRouteId(IdFor<Route> routeId) {
        return routes.hasId(routeId);
    }

    @Override
    public int numberOfRoutes() {
        return routes.size();
    }

    @Override
    public Set<Route> findRoutesByShortName(IdFor<Agency> agencyId, String shortName) {
        return routes.getValues().stream().
                filter(route -> route.getAgency().getId().equals(agencyId)).
                filter(route -> route.getShortName().equals(shortName)).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Route> findRoutesByName(final IdFor<Agency> agencyId, final String longName) {
        if (!agencyId.isValid()) {
            throw new RuntimeException("Invalid agency id");
        }
        return routes.getValues().stream().
                filter(route -> route.getAgency().getId().equals(agencyId)).
                filter(route -> route.getName().equals(longName)).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void addDataSourceInfo(DataSourceInfo dataSourceInfo) {
        dataSourceInfos.add(dataSourceInfo);
    }

    @Override
    public DateRangeAndVersion getDateRangeAndVersionFor(DataSourceID dataSourceID) {
        if (dateRangeAndVersionMap.containsKey(dataSourceID)) {
            return dateRangeAndVersionMap.get(dataSourceID);
        }
        Set<ServiceCalendar> inScope = services.getValues().stream().
                filter(service -> service.getDataSourceId().equals(dataSourceID)).
                map(MutableService::getCalendar).
                collect(Collectors.toSet());
        return dataSourceInfos.getDateRangeAndVersionFor(dataSourceID, inScope);
    }

    @Override
    public boolean hasDateRangeAndVersionFor(DataSourceID dataSourceID) {
        return dataSourceInfos.has(dataSourceID);
    }

    @Override
    public void addDateRangeAndVersionFor(DataSourceID dataSourceID, DateRangeAndVersion rangeAndVersion) {
        logger.info("Added " + rangeAndVersion.toString());
        if (dateRangeAndVersionMap.containsKey(dataSourceID)) {
            throw new RuntimeException("Cannot add duplicate info for " + dataSourceID);
        }
        dateRangeAndVersionMap.put(dataSourceID, rangeAndVersion);
    }

    @Override
    public String toString() {
        return "TransportDataContainer{" +
                "providesNow=" + providesNow +
                ",\n trips=" + trips +
                ",\n stationsById=" + stationsById +
                ",\n services=" + services +
                ",\n routes=" + routes +
                ",\n platforms=" + platforms +
                ",\n routeStations=" + routeStations +
                ",\n agencies=" + agencies +
                ",\n dataSourceInfos=" + dataSourceInfos +
                ",\n feedInfoMap=" + dateRangeAndVersionMap +
                ",\n sourceName='" + sourceName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransportDataContainer that = (TransportDataContainer) o;
        return trips.equals(that.trips) && stationsById.equals(that.stationsById) && services.equals(that.services) &&
                routes.equals(that.routes) && platforms.equals(that.platforms) && routeStations.equals(that.routeStations) &&
                agencies.equals(that.agencies) && dataSourceInfos.equals(that.dataSourceInfos) && dateRangeAndVersionMap.equals(that.dateRangeAndVersionMap) &&
                sourceName.equals(that.sourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trips, stationsById, services, routes, platforms, routeStations, agencies, dataSourceInfos, dateRangeAndVersionMap, sourceName);
    }

}
