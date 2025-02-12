package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.StationListConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.WriteableTransportData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class GTFSStopTimeLoader {
    private static final Logger logger = LoggerFactory.getLogger(GTFSStopTimeLoader.class);

    private final WriteableTransportData buildable;
    private final TransportEntityFactory factory;
    private final GTFSSourceConfig dataSourceConfig;
    private final boolean debugEnabled;

    public GTFSStopTimeLoader(WriteableTransportData buildable, TransportEntityFactory factory, GTFSSourceConfig dataSourceConfig) {
        this.buildable = buildable;
        this.factory = factory;
        this.dataSourceConfig = dataSourceConfig;
        debugEnabled = logger.isDebugEnabled();
    }

    public IdMap<Service> load(final Stream<StopTimeData> stopTimes, final PreloadedStationsAndPlatforms preloadStations,
                               final PreloadTripAndServices tripAndServices) {
        final String sourceName = dataSourceConfig.getName();
        final AtomicInteger invalidTimeCount = new AtomicInteger(0);

        final StopTimeDataLoader stopTimeDataLoader = new StopTimeDataLoader(buildable, preloadStations, factory,
                dataSourceConfig, tripAndServices);

        logger.info("Loading stop times for " + sourceName);
        stopTimes.
                filter(stopTimeData -> isValid(stopTimeData, invalidTimeCount)).
                filter(stopTimeData -> tripAndServices.hasId(stopTimeData.getTripId())).
                forEach(stopTimeDataLoader::loadStopTimeData);

        stopTimeDataLoader.close();

        addClosedStations(preloadStations);

        final int count = invalidTimeCount.get();
        if (count >0) {
            logger.warn("Got " + count + " invalid stop times, each logged to debug. Likely due to the >24Hours issue");
        }

        return stopTimeDataLoader.getAddedServices();
    }

    private void addClosedStations(final PreloadedStationsAndPlatforms preloadStations) {

        // Need to use config since data load is not complete and so can't use closed StationRepository?
        final List<StationClosures> stationClosures = dataSourceConfig.getStationClosures();
        if (stationClosures.isEmpty()) {
            logger.info("No station closures, no additional stations to add");
            return;
        }

        // TODO station pair based config??
        final IdSet<Station> closedButNotLoaded = stationClosures.stream().
                filter(stationClosure -> stationClosure instanceof StationListConfig).
                map(stationClosure -> (StationListConfig)stationClosure).
                flatMap(stationClosure -> stationClosure.getStations().stream()).
                filter(stationId -> !buildable.hasStationId(stationId)).
                collect(IdSet.idCollector());

//        final IdSet<Station> closedButNotLoaded = stationClosures.stream().
//                flatMap(closures -> closures.getStations().stream()).
//                filter(stationId -> !buildable.hasStationId(stationId)).
//                collect(IdSet.idCollector());

        if (closedButNotLoaded.isEmpty()) {
            logger.info("Station closures present but no additional stations to add");
            return;
        }

        logger.warn("The following stations were marked closed but were not added as no calling trams " + closedButNotLoaded);

        final EnumSet<TransportMode> transportModes = dataSourceConfig.getTransportModes();

        for (IdFor<Station> closedStationId : closedButNotLoaded) {
            if (!preloadStations.hasId(closedStationId)) {
                throw new RuntimeException("Missing closed station id in preloaded " + closedStationId);
            }
            final MutableStation station = preloadStations.get(closedStationId);
            transportModes.forEach(station::addMode);
            buildable.addStation(station);
            logger.info("Added closed station " + closedStationId);
        }

    }

    private boolean isValid(final StopTimeData stopTimeData, final AtomicInteger invalidTimeCount) {
        if (stopTimeData.isValid()) {
            return true;
        }
        invalidTimeCount.incrementAndGet();
        if (debugEnabled) {
            logger.debug("StopTimeData is invalid: " + stopTimeData);
        }
        return false;
    }

    private static class StopTimeDataLoader {
        private static final Logger logger = LoggerFactory.getLogger(StopTimeDataLoader.class);

        private final IdMap<Service> addedServices;
        private final IdSet<Station> excludedStations;
        private final MissingPlatforms missingPlatforms;
        private final AtomicInteger stopTimesLoaded;

        private final WriteableTransportData buildable;
        private final PreloadedStationsAndPlatforms preloadStations;
        private final TransportEntityFactory factory;
        private final GTFSSourceConfig dataSourceConfig;
        private final PreloadTripAndServices tripAndServices;

        public StopTimeDataLoader(WriteableTransportData buildable, PreloadedStationsAndPlatforms preloadStations,
                                  TransportEntityFactory factory, GTFSSourceConfig dataSourceConfig, PreloadTripAndServices tripAndServices) {
            this.buildable = buildable;
            this.preloadStations = preloadStations;
            this.factory = factory;
            this.dataSourceConfig = dataSourceConfig;
            this.tripAndServices = tripAndServices;

            addedServices = new IdMap<>();
            excludedStations = new IdSet<>();
            missingPlatforms = new MissingPlatforms();
            stopTimesLoaded = new AtomicInteger();
        }

        public void loadStopTimeData(final StopTimeData stopTimeData) {
            final IdFor<Station> stationId = factory.formStationId(stopTimeData);
            final IdFor<Trip> stopTripId = Trip.createId(stopTimeData.getTripId());

            if (preloadStations.hasId(stationId)) {
                final MutableTrip trip = tripAndServices.getTrip(stopTripId);
                final Route route = getRouteFrom(trip);
                final MutableStation station = preloadStations.get(stationId);

                final boolean routePlatforms = expectedPlatformsForRoute(route);
                final boolean stationPlatforms = station.hasPlatforms();
                if (routePlatforms && !stationPlatforms) {
                    missingPlatforms.record(stationId, stopTripId);
                } else if (stationPlatforms && !routePlatforms) {
                    logger.error(format("Platform mismatch, Skipping. Station %s has platforms but route %s does not for stop time %s",
                            station.getId(), route.getId(), stopTimeData));
                } else {
                    final Service added = addStopTimeData(stopTimeData, trip, station, route);
                    addedServices.add(added);
                    stopTimesLoaded.getAndIncrement();
                }
            } else {
                excludedStations.add(stationId);
                if (tripAndServices.hasId(stopTripId)) {
                    final MutableTrip trip = tripAndServices.getTrip(stopTripId);
                    trip.setFiltered(true);
                } else {
                    logger.warn(format("No trip %s for filtered stopcall %s", stopTripId, stationId));
                }
            }
        }

        @NotNull
        private Route getRouteFrom(final MutableTrip trip) {
            final Route route = trip.getRoute();

            if (route == null) {
                throw new RuntimeException("Null route for " + trip.getId());
            }
            return route;
        }

        private boolean expectedPlatformsForRoute(final Route route) {
            return dataSourceConfig.getTransportModesWithPlatforms().contains(route.getTransportMode());
        }

        private Service addStopTimeData(final StopTimeData stopTimeData, final MutableTrip trip,
                                        final MutableStation station, final Route route) {

            final MutableService service = tripAndServices.getService(trip.getService().getId());

            addStationAndRouteStation(route, station, stopTimeData);
            addPlatformsForStation(station);

            final StopCall stopCall = createStopCall(stopTimeData, route, trip, station);

            trip.addStop(stopCall);

            if (!buildable.hasTripId(trip.getId())) {
                buildable.addTrip(trip); // seen at least one stop for this trip
            }

            final MutableRoute mutableRoute = buildable.getMutableRoute(route.getId());
            mutableRoute.addTrip(trip);
            mutableRoute.addService(service);

            buildable.addService(service);

            return service;
        }

        private void addStationAndRouteStation(Route route, MutableStation station, StopTimeData stopTimeData) {

            final GTFSPickupDropoffType dropOffType = stopTimeData.getDropOffType();
            if (dropOffType.isDropOff()) {
                station.addRouteDropOff(route);
            }

            final GTFSPickupDropoffType pickupType = stopTimeData.getPickupType();
            if (pickupType.isPickup()) {
                station.addRoutePickUp(route);
            }

            final IdFor<Station> stationId = station.getId();
            if (!buildable.hasStationId(stationId)) {
                buildable.addStation(station);
                if (!station.getLatLong().isValid()) {
                    logger.warn("Station has invalid position " + station);
                }
            }

            if (!buildable.hasRouteStationId(RouteStation.createId(stationId, route.getId()))) {
                RouteStation routeStation = factory.createRouteStation(station, route);
                buildable.addRouteStation(routeStation);
            }
        }

        private void addPlatformsForStation(final Station station) {
            station.getPlatforms().stream().
                    map(HasId::getId).
                    filter(platformId -> !buildable.hasPlatformId(platformId)).
                    map(preloadStations::getPlatform).
                    forEach(buildable::addPlatform);
        }

        private StopCall createStopCall(final StopTimeData stopTimeData, final Route route, final Trip trip, final Station station) {
            final TransportMode transportMode = route.getTransportMode();

            // this is tfgm specific
            if (dataSourceConfig.getTransportModesWithPlatforms().contains(transportMode)) {

                final IdFor<Platform> platformId = factory.getPlatformId(stopTimeData, station);

                if (buildable.hasPlatformId(platformId)) {
                    final MutablePlatform platform = buildable.getMutablePlatform(platformId);

                    if (stopTimeData.getPickupType().isPickup()) {
                        platform.addRoutePickUp(route);
                    }
                    if (stopTimeData.getDropOffType().isDropOff()) {
                        platform.addRouteDropOff(route);
                    }

                    return factory.createPlatformStopCall(trip, platform, station, stopTimeData);
                } else {
                    final IdFor<Route> routeId = route.getId();
                    logger.error("Missing platform " + platformId + " For transport mode " + transportMode + " and route " + routeId);
                    return factory.createNoPlatformStopCall(trip, station, stopTimeData);
                }
            } else {
                return factory.createNoPlatformStopCall(trip, station, stopTimeData);
            }
        }

        public IdMap<Service> getAddedServices() {
            return addedServices;
        }

        public void close() {
            String sourceName = dataSourceConfig.getName();
            if (!excludedStations.isEmpty()) {
                logger.warn("Excluded the following station ids (flagged out of area) : " + excludedStations + " for " + sourceName);
                excludedStations.clear();
            }
            missingPlatforms.recordInLog(dataSourceConfig);
            missingPlatforms.clear();

            logger.info("Loaded " + stopTimesLoaded.get() + " stop times for " + sourceName);
        }


        private static class MissingPlatforms {
            private final Map<IdFor<Station>, IdSet<Trip>> missingPlatforms;

            private MissingPlatforms() {
                missingPlatforms = new HashMap<>();
            }

            public void record(final IdFor<Station> stationId, final IdFor<Trip> stopTripId) {
                if (!missingPlatforms.containsKey(stationId)) {
                    missingPlatforms.put(stationId, new IdSet<>());
                }
                missingPlatforms.get(stationId).add(stopTripId);
            }

            public void recordInLog(GTFSSourceConfig gtfsSourceConfig) {
                if (missingPlatforms.isEmpty()) {
                    return;
                }
                missingPlatforms.forEach((stationId, tripIds) -> logger.error(
                        format("Did not find platform for stationId: %s TripId: %s source:'%s'",
                                stationId, tripIds, gtfsSourceConfig.getName())));
            }

            public void clear() {
                missingPlatforms.clear();
            }
        }
    }
}
