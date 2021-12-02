package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.*;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.RailPlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportDataContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.None;
import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static java.lang.String.format;
import static java.time.temporal.ChronoField.*;

public class RailTimetableMapper {
    private static final Logger logger = LoggerFactory.getLogger(RailTimetableMapper.class);

    public static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4)
                .appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2).toFormatter();

    private enum State {
        SeenSchedule,
        SeenScheduleExtra,
        SeenOrigin,
        Between
    }

    private State currentState;
    private boolean overlay;
    private RawService rawService;
    private final CreatesTransportDataForRail processor;
    private final Map<String,TIPLOCInsert> tiplocInsertRecords;
    private final MissingStations missingStations;

    public RailTimetableMapper(TransportDataContainer container) {
        currentState = State.Between;
        overlay = false;
        tiplocInsertRecords = new HashMap<>();
        missingStations = new MissingStations();
        processor = new CreatesTransportDataForRail(container, missingStations);
    }

    public void seen(RailTimetableRecord record) {
        switch (record.getRecordType()) {
            case TiplocInsert -> tipLocInsert(record);
            case BasicSchedule -> seenBegin(record);
            case BasicScheduleExtra -> seenExtraInfo(record);
            case TerminatingLocation -> seenEnd(record);
            case OriginLocation -> seenOrigin(record);
            case IntermediateLocation -> seenIntermediate(record);
        }
    }

    private void tipLocInsert(RailTimetableRecord record) {
        TIPLOCInsert insert = (TIPLOCInsert) record;
        String atocCode = insert.getTiplocCode();
        tiplocInsertRecords.put(atocCode, insert);
    }

    private void seenExtraInfo(RailTimetableRecord record) {
        guardState(State.SeenSchedule, record);
        rawService.addScheduleExtra(record);
        currentState = State.SeenScheduleExtra;
    }

    private void seenIntermediate(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        rawService.addIntermediate(record);
    }

    private void seenOrigin(RailTimetableRecord record) {
        guardState(State.SeenScheduleExtra, record);
        rawService.addOrigin(record);
        currentState = State.SeenOrigin;
    }

    private void seenEnd(RailTimetableRecord record) {
        guardState(State.SeenOrigin, record);
        rawService.finish(record);
        processor.consume(rawService, overlay);
        currentState = State.Between;
        overlay = false;
    }

    private void seenBegin(RailTimetableRecord record) {
        BasicSchedule basicSchedule = (BasicSchedule) record;
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
        Set<String> missingTiplocs = missingStations.report(logger);
        missingTiplocs.stream().
                filter(tiplocInsertRecords::containsKey).
                map(tiplocInsertRecords::get).
                forEach(record -> logger.info("Had record for missing tiploc: "+record));
    }

    private void guardState(State expectedState, RailTimetableRecord record) {
        if (currentState != expectedState) {
            throw new RuntimeException(format("Expected state %s not %s at %s", expectedState, currentState, record));
        }
    }

    private static class RawService {

        private final BasicSchedule basicScheduleRecord;
        private final List<IntermediateLocation> intermediateLocations;
        private OriginLocation originLocation;
        private TerminatingLocation terminatingLocation;
        private BasicScheduleExtraDetails extraDetails;

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
    }

    private static class CreatesTransportDataForRail {
        private static final Logger logger = LoggerFactory.getLogger(CreatesTransportDataForRail.class);

        private final TransportDataContainer container;
        private final MissingStations missingStations; // stations missing unexpectedly
        private final RailServiceGroups railServiceGroups;
        private final RailRouteIDBuilder railRouteIDBuilder;

        private CreatesTransportDataForRail(TransportDataContainer container, MissingStations missingStations) {
            this.container = container;
            this.missingStations = missingStations;

            this.railServiceGroups = new RailServiceGroups(container);
            this.railRouteIDBuilder = new RailRouteIDBuilder();
        }

        public void consume(RawService rawService, boolean isOverlay) {
            BasicSchedule basicSchedule = rawService.basicScheduleRecord;

            switch (basicSchedule.getTransactionType()) {
                case N -> createNew(rawService, isOverlay);
                case D -> delete(rawService.basicScheduleRecord);
                case R -> revise(rawService);
                case Unknown -> logger.warn("Unknown transaction type for " + rawService.basicScheduleRecord);
            }
        }

        private void delete(BasicSchedule basicScheduleRecord) {
            logger.error("Delete schedule " + basicScheduleRecord);
        }

        private void revise(RawService rawService) {
            logger.error("Revise schedule " + rawService);
        }

        private void recordCancellations(BasicSchedule basicSchedule) {
            railServiceGroups.applyCancellation(basicSchedule);
        }

        private void createNew(RawService rawService, boolean isOverlay) {
            final BasicSchedule basicSchedule = rawService.basicScheduleRecord;
            TrainCategory cat = basicSchedule.getTrainCategory();
            final String uniqueTrainId = basicSchedule.getUniqueTrainId();
            if (cat==TrainCategory.LondonUndergroundOrMetroService || cat==TrainCategory.BusService) {
                logger.debug(format("Skipping %s of category %s and status %s", uniqueTrainId, basicSchedule.getTrainCategory(),
                        basicSchedule.getTrainStatus()));
                railServiceGroups.recordSkip(basicSchedule);
                return;
            }

            final OriginLocation originLocation = rawService.originLocation;
            final List<IntermediateLocation> intermediateLocations = rawService.intermediateLocations;
            final TerminatingLocation terminatingLocation = rawService.terminatingLocation;

            logger.debug("Create schedule for " + uniqueTrainId);
            final String atocCode = rawService.extraDetails.getAtocCode();

            // Agency
            MutableAgency mutableAgency = getOrCreateAgency(atocCode);
            final IdFor<Agency> agencyId = mutableAgency.getId();

            // Service
            MutableService service = railServiceGroups.getOrCreateService(basicSchedule, isOverlay);

            // Route
            MutableRoute route = getOrCreateRoute(rawService, mutableAgency, atocCode);
            route.addService(service);
            mutableAgency.addRoute(route);

            // Trip
            MutableTrip trip = getOrCreateTrip(basicSchedule, service, route);
            route.addTrip(trip);

            // Stations, Platforms, StopCalls
            int stopSequence = 1;
            populateForLocation(originLocation, route, trip, stopSequence, false, agencyId, basicSchedule);
            stopSequence = stopSequence + 1;
            for (IntermediateLocation intermediateLocation : intermediateLocations) {
                if (populateForLocation(intermediateLocation, route, trip, stopSequence, false, agencyId, basicSchedule)) {
                    stopSequence = stopSequence + 1;
                }
            }
            populateForLocation(terminatingLocation, route, trip, stopSequence, true, agencyId, basicSchedule);
        }

        private boolean populateForLocation(RailLocationRecord railLocation, MutableRoute route, MutableTrip trip, int stopSequence,
                                         boolean lastStop, IdFor<Agency> agencyId, BasicSchedule schedule) {

            if (railLocation.isPassingRecord()) {
                return false;
            }

            if (!isStation(railLocation)) {
                missingStationDiagnosticsFor(railLocation, agencyId, schedule);
                return false;
            }

            // Station
            MutableStation station = getStationFor(railLocation);
            station.addRoute(route);

            // Route Station
            RouteStation routeStation = new RouteStation(station, route);
            container.addRouteStation(routeStation);

            // Platform
            MutablePlatform platform = getOrCreatePlatform(station, railLocation);
            platform.addRoute(route);
            station.addPlatform(platform);

            final GTFSPickupDropoffType pickup = lastStop ? None : Regular;
            final GTFSPickupDropoffType dropoff = stopSequence==1 ? None : Regular;

            // Stop Call
            TramTime arrivalTime = railLocation.getPublicArrival();
            TramTime departureTime = railLocation.getPublicDeparture();
            StopCall stopCall = createStopCall(trip, station, platform, stopSequence,
                    arrivalTime, departureTime, pickup, dropoff);
            trip.addStop(stopCall);

            return true;
        }

        private void missingStationDiagnosticsFor(RailLocationRecord railLocation, IdFor<Agency> agencyId, BasicSchedule schedule) {
            final RailRecordType recordType = railLocation.getRecordType();
            boolean intermediate = (recordType==RailRecordType.IntermediateLocation);
            if (intermediate) {
                return;
            }

            String tiplocCode = railLocation.getTiplocCode();
            missingStations.record(tiplocCode, railLocation, agencyId, schedule);
        }

        private boolean isStation(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            return container.hasStationId(stationId);
        }

        private MutableStation getStationFor(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            MutableStation station;
            if (!container.hasStationId(stationId)) {
                    throw new RuntimeException(format("Missing stationid %s encountered for %s", stationId, record));
            } else {
                station = container.getMutableStation(stationId);
            }
            return station;
        }

        @NotNull
        private RailPlatformStopCall createStopCall(MutableTrip trip, MutableStation station,
                                                MutablePlatform platform, int stopSequence, TramTime arrivalTime,
                                                TramTime departureTime, GTFSPickupDropoffType pickup, GTFSPickupDropoffType dropoff) {
            return new RailPlatformStopCall(station, arrivalTime, departureTime, stopSequence, pickup, dropoff, trip, platform);
        }

        private MutableAgency getOrCreateAgency(String atocCode) {
            MutableAgency mutableAgency;
            final IdFor<Agency> agencyId = StringIdFor.createId(atocCode);
            if (container.hasAgencyId(agencyId)) {
                mutableAgency = container.getMutableAgency(agencyId);
            } else {
                // todo get list of atoc names
                mutableAgency = new MutableAgency(DataSourceID.rail, agencyId, atocCode);
                container.addAgency(mutableAgency);
            }
            return mutableAgency;
        }

        private MutablePlatform getOrCreatePlatform(MutableStation originStation, RailLocationRecord originLocation) {
            String platformNumber = originLocation.getPlatform();
            IdFor<Platform> platformId = StringIdFor.createId(originLocation.getTiplocCode() + ":" + platformNumber);
            MutablePlatform platform;
            if (container.hasPlatformId(platformId)) {
                platform = container.getMutablePlatform(platformId);
            } else {
                platform = new MutablePlatform(platformId, originStation.getName(), platformNumber,
                        originStation.getLatLong());
                container.addPlatform(platform);
            }

            return platform;
        }

        private MutableTrip getOrCreateTrip(BasicSchedule schedule, MutableService service, MutableRoute route) {
            MutableTrip trip;
            IdFor<Trip> tripId = createTripIdFor(service);
            if (container.hasTripId(tripId)) {
                logger.info("Had existing tripId: " + tripId + " for " + schedule);
                trip = container.getMutableTrip(tripId);
            } else {
                trip = new MutableTrip(tripId, schedule.getTrainIdentity(), service, route);
                container.addTrip(trip);
            }
            return trip;
        }

        private IdFor<Trip> createTripIdFor(MutableService service) {
            return StringIdFor.createId("trip:"+service.getId().forDTO());
        }

        private MutableRoute getOrCreateRoute(RawService rawService,
                                              MutableAgency mutableAgency, String atocCode) {
            MutableRoute route;
            List<Station> callingPoints = getRouteStationCallingPoints(rawService);
            IdFor<Route> routeId = railRouteIDBuilder.getIdFor(atocCode, callingPoints);

            if (container.hasRouteId(routeId)) {
                route = container.getMutableRoute(routeId);
            } else {
                route = new MutableRailRoute(routeId, callingPoints, mutableAgency, TransportMode.Train);
                container.addRoute(route);
            }
            return route;
        }

        private List<Station> getRouteStationCallingPoints(RawService rawService) {
            List<Station> result = new ArrayList<>();
            if (isStation(rawService.originLocation)) {
                result.add(getStationFor(rawService.originLocation));
            }
            List<Station> inters = rawService.intermediateLocations.stream().
                    filter(intermediateLocation -> !intermediateLocation.isPassingRecord()).
                    filter(this::isStation).
                    map(this::getStationFor).
                    collect(Collectors.toList());
            result.addAll(inters);
            if (isStation(rawService.terminatingLocation)) {
                result.add(getStationFor(rawService.terminatingLocation));
            }
            return result;
        }

    }

    private static class MissingStations {
        private final Map<String, MissingStation> missing;

        private MissingStations() {
            missing = new HashMap<>();
        }

        public void record(String tiplocCode, RailLocationRecord railLocation, IdFor<Agency> agencyId, BasicSchedule schedule) {
            if (!missing.containsKey(tiplocCode)) {
                missing.put(tiplocCode,new MissingStation(tiplocCode, agencyId, schedule));
            }
            missing.get(tiplocCode).addRecord(railLocation);
        }

        public Set<String> report(Logger logger) {
            if (missing.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> tiplocCodes = new HashSet<>();
            missing.forEach((tiplocCode, missingStation) -> {
                missingStation.report(logger);
                tiplocCodes.add(tiplocCode);
            });
            return tiplocCodes;
        }
    }

    private static class MissingStation {
        private final String tiplocCode;
        private final Set<RailLocationRecord> records;
        private final IdFor<Agency> agencyId;
        private final BasicSchedule schedule;

        private MissingStation(String tiplocCode, IdFor<Agency> agencyId, BasicSchedule schedule) {
            this.tiplocCode = tiplocCode;
            this.agencyId = agencyId;
            this.schedule = schedule;
            records = new HashSet<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MissingStation that = (MissingStation) o;

            if (!tiplocCode.equals(that.tiplocCode)) return false;
            return agencyId.equals(that.agencyId);
        }

        @Override
        public int hashCode() {
            int result = tiplocCode.hashCode();
            result = 31 * result + agencyId.hashCode();
            return result;
        }

        public void addRecord(RailLocationRecord railLocation) {
            records.add(railLocation);
        }

        public void report(Logger logger) {
            logger.info(format("Missing tiplocCode %s for %s %s in records %s",
                    tiplocCode, agencyId, schedule, records));
        }
    }
}