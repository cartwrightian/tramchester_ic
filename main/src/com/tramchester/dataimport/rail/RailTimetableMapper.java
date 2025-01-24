package com.tramchester.dataimport.rail;

import com.tramchester.config.RailConfig;
import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.*;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.RailPlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.WriteableTransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.domain.reference.TransportMode.RailReplacementBus;
import static com.tramchester.domain.reference.TransportMode.Train;
import static java.lang.String.format;
import static java.time.temporal.ChronoField.*;

public class RailTimetableMapper {
    private static final Logger logger = LoggerFactory.getLogger(RailTimetableMapper.class);

    public static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2).toFormatter();

    private final RailServiceGroups railServiceGroups;
    private final RailStationRecordsRepository stationRecords;

    private enum State {
        SeenSchedule,
        SeenScheduleExtra,
        SeenOrigin,
        Between
    }

    private final CreatesTransportDataForRail processor;
    private final Set<Pair<TrainStatus, TrainCategory>> travelCombinations;
    private final AtomicInteger skippedService;

    private State currentState;
    private boolean overlay;
    private RawService rawService;

    public RailTimetableMapper(RailStationRecordsRepository stationRecords, WriteableTransportData container,
                               RailConfig config, GraphFilterActive filter, BoundingBox bounds, RailRouteIdRepository railRouteRepository) {

        currentState = State.Between;
        overlay = false;
        travelCombinations = new HashSet<>();
        skippedService = new AtomicInteger(0);

        railServiceGroups = new RailServiceGroups(container);
        this.stationRecords = stationRecords;
        processor = new CreatesTransportDataForRail(stationRecords, container, travelCombinations,
                config, filter, bounds, railServiceGroups, railRouteRepository);
    }

    public void seen(final RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case TiplocInsert -> tipLocInsert(record);
            case BasicSchedule -> seenBegin(record);
            case BasicScheduleExtra -> seenExtraInfo(record);
            case TerminatingLocation -> seenEnd(record);
            case OriginLocation -> seenOrigin(record);
            case IntermediateLocation -> seenIntermediate(record);
        }
    }

    private void tipLocInsert(final RailTimetableRecord record) {
        // these are handled via ExtractAgencyCallingPointsFromLocationRecords

    }

    private void seenExtraInfo(final RailTimetableRecord record) {
        guardState(State.SeenSchedule, record);
        rawService.addScheduleExtra(record);
        currentState = State.SeenScheduleExtra;
    }

    private void seenIntermediate(final RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        rawService.addIntermediate(record);
    }

    private void seenOrigin(final RailTimetableRecord record) {
        guardState(State.SeenScheduleExtra, record);
        rawService.addOrigin(record);
        currentState = State.SeenOrigin;
    }

    private void seenEnd(final RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        rawService.finish(record);
        processor.consume(rawService, overlay, skippedService);
        currentState = State.Between;
        overlay = false;
    }

    private void seenBegin(final RailTimetableRecord record) {
        final BasicSchedule basicSchedule = (BasicSchedule) record;
        guardState(State.Between, record);

        rawService = new RawService(basicSchedule);

        switch (basicSchedule.getSTPIndicator()) {
            case Cancellation -> {
                processor.recordCancellations(basicSchedule);
                currentState = State.Between;
            }
            case New, Permanent -> currentState = State.SeenSchedule;
            case Overlay -> {
                overlay = true;
                currentState = State.SeenSchedule;
            }
            default -> logger.warn("Not handling " + basicSchedule);
        }
    }

    public void reportDiagnostics() {
        travelCombinations.forEach(pair -> logger.info(String.format("Rail loaded: Status: %s Category: %s",
                pair.getLeft(), pair.getRight())));
        railServiceGroups.reportUnmatchedCancellations();
        reportSkipped();
    }

    private void reportSkipped() {
        int count = skippedService.get();
        if (count>0) {
            logger.warn("Skipped " + count + " records");
        }
    }

    private void guardState(final State expectedState, final RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    static class RawService {

        private final BasicSchedule basicScheduleRecord;
        private final List<IntermediateLocation> intermediateLocations;
        private BasicScheduleExtraDetails extraDetails;
        private OriginLocation originLocation;
        private TerminatingLocation terminatingLocation;

        public RawService(RailTimetableRecord basicScheduleRecord) {
            this.basicScheduleRecord = (BasicSchedule) basicScheduleRecord;
            intermediateLocations = new ArrayList<>();
        }

        public void addIntermediate(RailTimetableRecord record) {
            intermediateLocations.add((IntermediateLocation) record);
        }

        public void addOrigin(RailTimetableRecord record) {
            this.originLocation = (OriginLocation) record;
        }

        public void finish(RailTimetableRecord record) {
            this.terminatingLocation = (TerminatingLocation) record;
        }

        public void addScheduleExtra(RailTimetableRecord record) {
            this.extraDetails = (BasicScheduleExtraDetails) record;
        }

//        public RailLocationRecord getTerminatingLocation() {
//            return terminatingLocation;
//        }
    }

    private static class CreatesTransportDataForRail {
        private static final Logger logger = LoggerFactory.getLogger(CreatesTransportDataForRail.class);

        private final RailStationRecordsRepository stationRecords;
        private final WriteableTransportData container;
        private final RailServiceGroups railServiceGroups;
        private final RailRouteIdRepository railRouteIdRepository;
        private final Set<Pair<TrainStatus, TrainCategory>> travelCombinations;
        private final RailConfig config;
        private final GraphFilterActive filter;
        private final BoundingBox bounds;

        private final Map<PlatformId, MutablePlatform> platformLookup;

        private CreatesTransportDataForRail(RailStationRecordsRepository stationRecords, WriteableTransportData container,
                                            Set<Pair<TrainStatus, TrainCategory>> travelCombinations,
                                            RailConfig config, GraphFilterActive filter, BoundingBox bounds, RailServiceGroups railServiceGroups,
                                            RailRouteIdRepository railRouteIdRepository) {
            this.stationRecords = stationRecords;
            this.container = container;

            this.travelCombinations = travelCombinations;
            this.config = config;
            this.filter = filter;
            this.bounds = bounds;
            this.railServiceGroups = railServiceGroups;
            this.railRouteIdRepository = railRouteIdRepository;

            platformLookup = new HashMap<>();
        }

        public void consume(final RawService rawService, final boolean isOverlay, final AtomicInteger skipped) {
            BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            switch (basicSchedule.getTransactionType()) {
                case New -> {
                    if (!createNew(rawService, isOverlay)) {
                        skipped.getAndIncrement();
                    }
                }
                case Delete -> delete(rawService.basicScheduleRecord);
                case Revise -> revise(rawService);
                case Unknown -> logger.warn("Unknown transaction type for " + rawService.basicScheduleRecord);
            }
        }

        private void delete(final BasicSchedule basicScheduleRecord) {
            logger.error("Delete schedule " + basicScheduleRecord);
        }

        private void revise(final RawService rawService) {
            logger.error("Revise schedule " + rawService);
        }

        private void recordCancellations(final BasicSchedule basicSchedule) {
            railServiceGroups.applyCancellation(basicSchedule);
        }

        private boolean createNew(final RawService rawService, final boolean isOverlay) {
            final BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            // assists with diagnosing data issues
            travelCombinations.add(Pair.of(basicSchedule.getTrainStatus(), basicSchedule.getTrainCategory()));

            final TransportMode mode = RailTransportModeMapper.getModeFor(rawService.basicScheduleRecord);

            final String uniqueTrainId = basicSchedule.getUniqueTrainId();
            if (!shouldInclude(mode)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Skipping %s of category %s and status %s", uniqueTrainId, basicSchedule.getTrainCategory(),
                            basicSchedule.getTrainStatus()));
                }
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            // Calling points
            final List<Station> allCalledAtStations = getRouteStationCallingPoints(rawService);

            if (allCalledAtStations.isEmpty() || allCalledAtStations.size()==1) {
                logger.warn(format("Skip, Not enough calling points (%s) for (%s) without bounds checking",
                        HasId.asIds(allCalledAtStations), rawService));
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            final List<Station> withinBoundsCallingStations = allCalledAtStations.stream().
                    filter(station -> station.getGridPosition().isValid()).
                    filter(bounds::contained).
                    toList();

            if (withinBoundsCallingStations.isEmpty() || withinBoundsCallingStations.size()==1) {
                // likely due to all stations being filtered out as beyond geo bounds
                //logger.debug(format("Skip, Not enough calling points (%s) for (%s)", inBoundsCalledAtStations.stream(), rawService));
                railServiceGroups.recordSkip(basicSchedule);
                return false;
            }

            // Agency
            final String atocCode = rawService.extraDetails.getAtocCode();
            final MutableAgency mutableAgency = getOrCreateAgency(atocCode);
            final IdFor<Agency> agencyId = mutableAgency.getId();

            // route ID uses "national" ids, so without calling points filtered to be within bounds
            // this is so routes that start and/or finish out-of-bounds are named correctly
            final RailRouteId routeId = railRouteIdRepository.getRouteIdFor(agencyId, allCalledAtStations);

            final MutableService service = railServiceGroups.getOrCreateService(basicSchedule, isOverlay, DataSourceID.openRailData);
            final MutableRoute route = getOrCreateRoute(routeId, rawService, mutableAgency, mode, allCalledAtStations);

            route.addService(service);
            mutableAgency.addRoute(route);

            // Trip
            final MutableTrip trip = getOrCreateTrip(basicSchedule, service, route, mode);
            route.addTrip(trip);

            addStationsPlatformsAndStopcalls(rawService, route, trip);

            return true;
        }

        private void addStationsPlatformsAndStopcalls(final RawService rawService, final Route route, final MutableTrip trip) {
            // Stations, Platforms, StopCalls
            final OriginLocation originLocation = rawService.originLocation;
            final List<IntermediateLocation> intermediateLocations = rawService.intermediateLocations;
            final TerminatingLocation terminatingLocation = rawService.terminatingLocation;

            final TramTime originTime = originLocation.getDeparture();

            int stopSequence = 1;
            populateForLocationIfWithinBounds(originLocation, route, trip, stopSequence, originTime);
            stopSequence = stopSequence + 1;
            for (IntermediateLocation intermediateLocation : intermediateLocations) {
                populateForLocationIfWithinBounds(intermediateLocation, route, trip, stopSequence, originTime);
                // if ....  stopSequence = stopSequence + 1; - keep sequence same as source even if skipping
                stopSequence = stopSequence + 1;
            }
            populateForLocationIfWithinBounds(terminatingLocation, route, trip, stopSequence, originTime);
        }

        private boolean shouldInclude(final TransportMode mode) {
            return config.getModes().contains(mode);
        }

        private void populateForLocationIfWithinBounds(final RailLocationRecord railLocation,
                                                       final Route route, final MutableTrip trip,
                                                       final int stopSequence, final TramTime originTime) {
            if (!railLocation.getArrival().isValid()) {
                logger.warn("Invalid arrival time for " + railLocation);
            }
            if (!railLocation.getDeparture().isValid()) {
                logger.warn("Invalid departure time for " + railLocation);
            }

            if (!stationRecords.hasStationRecord(railLocation)) {
                return;
            }

            final MutableStation station = findStationFor(railLocation);

            if (!station.getGridPosition().isValid()) {
                return;
            }

            if (!bounds.contained(station)) {
                return;
            }

            stationRecords.markAsInUse(station);

            final EnumSet<LocationActivityCode> activity = railLocation.getActivity();

            // Platform
            final IdFor<NPTGLocality> areaId = station.getLocalityId(); // naptan seems only to have rail stations, not platforms
            final MutablePlatform platform = getOrCreatePlatform(station, railLocation, areaId);

            station.addPlatform(platform);

            final boolean doesPickup = LocationActivityCode.doesPickup(activity);
            final boolean doesDropOff = LocationActivityCode.doesDropOff(activity);

            if (doesDropOff) {
                station.addRouteDropOff(route);
                platform.addRouteDropOff(route);
            }
            if (doesPickup) {
                station.addRoutePickUp(route);
                platform.addRoutePickUp(route);
            }

            // Route Station
            final RouteStation routeStation = new RouteStation(station, route);
            container.addRouteStation(routeStation);

            final StopCall stopCall;
            if (railLocation.doesStop()) {
                // TODO this doesn't cope with journeys that cross 2 days....
                final TramTime arrivalTime = getDayAdjusted(railLocation.getArrival(), originTime);
                final TramTime departureTime = getDayAdjusted(railLocation.getDeparture(), originTime);

                // TODO Request stops?
                final GTFSPickupDropoffType pickup = doesPickup ? Regular : None;
                final GTFSPickupDropoffType dropoff = doesDropOff ? Regular : None;
                stopCall = createStopCall(trip, station, platform, stopSequence, arrivalTime, departureTime, pickup, dropoff);

                if (Durations.greaterThan(TramTime.difference(arrivalTime, departureTime), Duration.ofHours(1))) {
                    // this definitely happens, so an info not a warning
                    logger.info("Delay of more than one hour for " + stopCall + " on trip " + trip.getId());
                }
            } else {
                stopCall = createStopcallForNoneStopping(railLocation, trip, stopSequence, station, platform, originTime);
            }

            trip.addStop(stopCall);

        }

        private TramTime getDayAdjusted(final TramTime arrivalTime, final TramTime originTime) {
            TramTime result = arrivalTime;
            if (arrivalTime.isBefore(originTime)) {
                result = TramTime.nextDay(arrivalTime);
            }
            return result;
        }

        @NotNull
        private StopCall createStopcallForNoneStopping(final RailLocationRecord railLocation, final MutableTrip trip,
                                                       final int stopSequence, final Station station,
                                                       final Platform platform, final TramTime originTime) {
            TramTime passingTime;
            if (railLocation.isOrigin()) {
                passingTime = railLocation.getDeparture();
            } else if (railLocation.isTerminating()) {
                passingTime = railLocation.getArrival();
            } else {
                passingTime = railLocation.getPassingTime();
            }

            if (!passingTime.isValid()) {
                throw new RuntimeException("Invalid passing time for " + railLocation);
            }

            passingTime = getDayAdjusted(passingTime, originTime);

            return createStopCall(trip, station, platform, stopSequence, passingTime, passingTime, None, None);
        }

        private MutableStation findStationFor(final RailLocationRecord record) {
            final MutableStation station;
            if (!stationRecords.hasStationRecord(record)) {
                throw new RuntimeException(format("Missing stationid %s encountered for %s", record.getTiplocCode(), record));
            } else {
                station = stationRecords.getMutableStationFor(record);
            }
            // can't do this here, not always filtered by geo bounds at this stage
            //stationRecords.markAsInUse(station);
            return station;
        }

        @NotNull
        private RailPlatformStopCall createStopCall(final Trip trip, final Station station,
                                                final Platform platform, final int stopSequence, final TramTime arrivalTime,
                                                final TramTime departureTime, final GTFSPickupDropoffType pickup,
                                                    final GTFSPickupDropoffType dropoff) {
            if (!arrivalTime.isValid()) {
                throw new RuntimeException(format("Invalid arrival time %s for %s on trip %s ",
                        arrivalTime, station.getId(), trip));
            }
            if (!departureTime.isValid()) {
                throw new RuntimeException(format("Invalid departure time %s for %s on trip %s ",
                        departureTime, station.getId(), trip));
            }

            return new RailPlatformStopCall(station, arrivalTime, departureTime, stopSequence, pickup, dropoff, trip, platform);
        }

        private MutableAgency getOrCreateAgency(final String atocCode) {
            final MutableAgency mutableAgency;
            final IdFor<Agency> agencyId = Agency.createId(atocCode);
            if (container.hasAgencyId(agencyId)) {
                mutableAgency = container.getMutableAgency(agencyId);
            } else {
                // todo get list of atoc names
                logger.info("Creating agency for atco code " + atocCode);

                final String agencyName = TrainOperatingCompanies.companyNameFor(agencyId);
                if (agencyName.equals(TrainOperatingCompanies.UNKNOWN.getCompanyName())) {
                    logger.warn("Unable to find name for agency " + atocCode);
                }
                mutableAgency = new MutableAgency(DataSourceID.openRailData, agencyId, agencyName);
                container.addAgency(mutableAgency);
            }
            return mutableAgency;
        }

        private MutablePlatform getOrCreatePlatform(final Station originStation, final RailLocationRecord originLocation,
                                                    final IdFor<NPTGLocality> areaId) {

            final String originLocationPlatform = originLocation.getPlatform();
            final String platformNumber = originLocationPlatform.isEmpty() ? "UNK" : originLocationPlatform;

            final PlatformId platformId = PlatformId.createId(originStation, platformNumber);

            final MutablePlatform platform;
            if (platformLookup.containsKey(platformId)) {
                platform = platformLookup.get(platformId);
            } else {
                platform = new MutablePlatform(platformId, originStation, originStation.getName(), DataSourceID.openRailData, platformNumber,
                        areaId, originStation.getLatLong(), originStation.getGridPosition(), originStation.isMarkedInterchange());
                container.addPlatform(platform);
                platformLookup.put(platformId, platform);
            }

            return platform;
        }

        private MutableTrip getOrCreateTrip(final BasicSchedule schedule, final MutableService service, final Route route,
                                            final TransportMode mode) {
            final MutableTrip trip;
            final IdFor<Trip> tripId = createTripIdFor(service);
            if (container.hasTripId(tripId)) {
                logger.info("Had existing tripId: " + tripId + " for " + schedule);
                trip = container.getMutableTrip(tripId);
            } else {
                trip = new MutableTrip(tripId, schedule.getTrainIdentity(), service, route, mode);
                container.addTrip(trip);
                service.addTrip(trip);
            }
            return trip;
        }

        private IdFor<Trip> createTripIdFor(final Service service) {
            return StringIdFor.withPrefix("trip:", service.getId(), Trip.class);
        }

        private MutableRoute getOrCreateRoute(final RailRouteId routeId, final RawService rawService, final Agency agency,
                                              final TransportMode mode, final List<Station> allCallingPoints) {
            final IdFor<Agency> agencyId = agency.getId();

            final MutableRoute route;
            if (container.hasRouteId(routeId)) {
                // route id already present
                route = container.getMutableRoute(routeId);
                final IdFor<Agency> routeAgencyCode = route.getAgency().getId();
                if (!routeAgencyCode.equals(agencyId)) {
                    String msg = String.format("Got route %s wrong agency id (%s) expected: %s\nSchedule: %s\nExtraDetails: %s",
                            routeId, routeAgencyCode, agencyId, rawService.basicScheduleRecord, rawService.extraDetails);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
                if (!matchingTransportModes(route, mode)) {
                    String msg = String.format("Got route %s wrong TransportMode (%s) route had: %s\nSchedule: %s\nExtraDetails: %s",
                            routeId, mode, route.getTransportMode(), rawService.basicScheduleRecord, rawService.extraDetails);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
            } else {
                // note:create RailReplacementBus routes as Train
                final TransportMode actualMode = (mode==RailReplacementBus) ? Train : mode;
                route = new MutableRailRoute(routeId, allCallingPoints, agency, actualMode);
                container.addRoute(route);
            }
            return route;
        }

        private boolean matchingTransportModes(final Route route, final TransportMode mode) {
            if (route.getTransportMode()==Train) {
                return mode==RailReplacementBus || mode==Train;
            }
            return route.getTransportMode()==mode;
        }

        private List<Station> getRouteStationCallingPoints(final RawService rawService) {


            // TODO NOTE potential duplication here as create list of stations but this is NOT used to for
            // the trip callings points

            final List<Station> result = new ArrayList<>();

            // add the starting point
            if (stationRecords.hasStationRecord(rawService.originLocation)) {
                final MutableStation station = findStationFor(rawService.originLocation);
                result.add(station);
            }

            // add the intermediates
            // TODO for now don't record passed stations (not stopping) but might want to do so in future to assist with live data processing
            final List<IntermediateLocation> callingRecords = rawService.intermediateLocations.stream().
                    filter(IntermediateLocation::doesStop).
                    toList();

            final List<Station> intermediates = callingRecords.stream().
                    filter(stationRecords::hasStationRecord).
                    map(this::findStationFor).
                    collect(Collectors.toList());

            result.addAll(intermediates);

            // add the final station
            if (stationRecords.hasStationRecord(rawService.terminatingLocation)) {
                result.add(findStationFor(rawService.terminatingLocation));
            }

            if (!filter.isActive()) {
                // This seems to be happening where train appears to stop at a location not listed in MSN as a station
                // TODO check the docs, guessing is edge case which should be be recorded as an actual stop even if
                // flagged as 'T'
                if (callingRecords.size() != intermediates.size()) {
                    // replacement bus often seem to be missing 1 station
                    if (rawService.basicScheduleRecord.getTrainCategory()!=TrainCategory.BusReplacement) {
                        Set<IntermediateLocation> missing = callingRecords.stream().
                                filter(record -> !stationRecords.hasStationRecord(record)).
                                //map(IntermediateLocation::getTiplocCode).
                                        collect(Collectors.toSet());
                        logger.warn(format("Did not match all calling points (got %s of %s) for %s Missing: %s",
                                intermediates.size(), callingRecords.size(), rawService.basicScheduleRecord,
                                missing));
                    }
                }
            }

            return result;
        }

    }

}
