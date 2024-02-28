package com.tramchester.graph.search;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.*;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

class MapStatesToStages implements JourneyStateUpdate {
    private static final Logger logger = LoggerFactory.getLogger(MapStatesToStages.class);

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TripRepository tripRepository;
    private final List<TransportStage<?, ?>> stages;

    private boolean onVehicle;
    private boolean onDiversion;

    private Duration totalCost; // total cost of entire journey
    private TramTime actualTime; // updated each time pass minute node and know 'actual' time
    private Duration costOffsetAtActual; // total cost at point got 'actual' time update

    @Deprecated
    private TramTime boardingTime;

    @Deprecated
    private TramTime beginWalkClock;

    private IdFor<Station> walkStartStation;

    private WalkFromStartPending walkFromStartPending;
    private VehicleStagePending vehicleStagePending;


    public MapStatesToStages(StationRepository stationRepository, PlatformRepository platformRepository,
                             TripRepository tripRepository, TramTime queryTime) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.tripRepository = tripRepository;

        actualTime = queryTime;
        stages = new ArrayList<>();
        onVehicle = false;
        totalCost = Duration.ZERO;
        costOffsetAtActual = Duration.ZERO;
        onDiversion = false;
    }

    @Override
    public void board(final TransportMode transportMode, final GraphNode node, final boolean hasPlatform) {
        onVehicle = true;
        boardingTime = null;
        if (onDiversion) {
            logger.info("End diversion");
            onDiversion = false;
        }

        final IdFor<Station> actionStationId = node.getStationId();
        if (logger.isDebugEnabled()) {
            logger.debug("Board " + transportMode + " " + actionStationId + " totalcost  " + totalCost);
        }
        vehicleStagePending = new VehicleStagePending(stationRepository, tripRepository, platformRepository,
                actionStationId, totalCost);
        if (hasPlatform) {
            final IdFor<Platform> boardingPlatformId = node.getPlatformId();
            vehicleStagePending.addPlatform(boardingPlatformId);
        }
    }

    @Override
    public void recordTime(final TramTime time, final Duration totalCost) {
        logger.debug("Record actual time " + time + " total cost:" + totalCost);
        this.actualTime = time;
        costOffsetAtActual = totalCost;
        if (onVehicle && boardingTime == null) {
            vehicleStagePending.setBoardingTime(actualTime);
            boardingTime = time;
        }
        if (walkFromStartPending != null) {
            WalkingToStationStage walkingToStationStage = walkFromStartPending.createStage(actualTime, totalCost);
            logger.info("Add " + walkingToStationStage);
            stages.add(walkingToStationStage);
            walkFromStartPending = null;
        }
    }

    @Override
    public void leave(final IdFor<Trip> tripId, final TransportMode mode, final Duration totalCost, final GraphNode routeStationNode) {
        if (!onVehicle) {
            throw new RuntimeException("Not on vehicle");
        }
        onVehicle = false;

        final VehicleStage vehicleStage = vehicleStagePending.createStage(routeStationNode, totalCost, tripId, mode);
        stages.add(vehicleStage);
        if (logger.isDebugEnabled()) {
            logger.debug("Added " + vehicleStage);
        }
        reset();
    }

    protected void passStop(final GraphRelationship fromMinuteNodeRelationship) {
        logger.debug("pass stop");
        if (onVehicle) {
            int stopSequenceNumber = fromMinuteNodeRelationship.getStopSeqNumber(); //GraphProps.getStopSequenceNumber(fromMinuteNodeRelationship);
            vehicleStagePending.addStopSeqNumber(stopSequenceNumber);
        } else {
            logger.error("Passed stop but not on vehicle");
        }
    }

    @Override
    public void updateTotalCost(Duration total) {
        this.totalCost = total;
    }

    private TramTime getActualClock() {
        return actualTime.plusRounded(totalCost.minus(costOffsetAtActual));
    }

    @Override
    public void beginTrip(IdFor<Trip> newTripId) {
        logger.debug("Begin trip:" + newTripId);
        //this.tripId = newTripId;
    }

    @Override
    public void beginWalk(final GraphNode beforeWalkNode, final boolean atStart, final Duration cost) {
        logger.debug("Walk cost " + cost);
        if (atStart) {
            final LatLong walkStartLocation = beforeWalkNode.getLatLong();
            walkFromStartPending = new WalkFromStartPending(walkStartLocation);
            walkStartStation = null;
            beginWalkClock = getActualClock();
            logger.info("Begin walk from start " + walkStartLocation + " at " + beginWalkClock) ;
        } else {
            walkStartStation = beforeWalkNode.getStationId();
            beginWalkClock = getActualClock().minusRounded(cost);
            logger.info("Begin walk from station " + walkStartStation + " at " + beginWalkClock);
        }
    }

    @Override
    public void endWalk(final GraphNode endWalkNode) {

        final Duration duration = TramTime.difference(beginWalkClock, getActualClock());

        if (walkFromStartPending != null) {
            boolean atStation = endWalkNode.hasStationId();
            if (atStation) {
                final IdFor<Station> destinationStationId = endWalkNode.getStationId();
                final Station destination = stationRepository.getStationById(destinationStationId);
                walkFromStartPending.setDestinationAndDuration(totalCost, destination, duration);
            }  else {
                throw new RuntimeException("Ended walked at unexpected node " + endWalkNode.getAllProperties());
            }
        } else {
            if (walkStartStation!=null) {
                // walk from a station
                final Station walkStation = stationRepository.getStationById(walkStartStation);
                final LatLong walkEnd = endWalkNode.getLatLong();
                final MyLocation destination = MyLocation.create(walkEnd);

                logger.info("End walk from station to " + walkEnd + " duration " + duration);
                final WalkingFromStationStage stage = new WalkingFromStationStage(walkStation, destination, duration, beginWalkClock);
                stages.add(stage);
            } else {
                throw new RuntimeException("Unexpected end of walk not form a station");
            }
        }

        reset();
    }

    @Override
    public void toNeighbour(final GraphNode startNode, final GraphNode endNode, final Duration cost) {
        final IdFor<Station> startId = startNode.getStationId();
        final IdFor<Station> endId = endNode.getStationId();
        final Station start = stationRepository.getStationById(startId);
        final Station end = stationRepository.getStationById(endId);
        final ConnectingStage<Station,Station> connectingStage = new ConnectingStage<>(start, end, cost, getActualClock());
        logger.info("Added stage " + connectingStage);
        stages.add(connectingStage);
    }

    @Override
    public void seenStation(IdFor<Station> stationId) {
        // no-op
    }

    @Override
    public void seenRouteStation(IdFor<Station> correspondingStationId) {
        // no-op
    }

    @Override
    public void seenStationGroup(IdFor<StationGroup> stationGroupId) {
        // no-op
    }

    @Override
    public void beginDiversion(IdFor<Station> stationId) {
        if (onDiversion) {
            throw new RuntimeException("Already on diversion at " + stationId);
        }
        onDiversion = true;
    }

    @Override
    public boolean onDiversion() {
        return onDiversion;
    }

    public List<TransportStage<?, ?>> getStages() {
        if (walkFromStartPending != null) {
            WalkingStage<?,?> walkingStage = walkFromStartPending.createStage(getActualClock(), totalCost);
            logger.info("Add final pending walking stage " + walkingStage);
            stages.add(walkingStage);
        }
        return stages;
    }

    private void reset() {
        beginWalkClock = null;
    }

    private static class WalkFromStartPending {

        private final LatLong walkStart;
        private Duration totalCostAtDestination;
        private Station destination;
        private Duration duration;

        public WalkFromStartPending(LatLong walkStart) {
            this.walkStart = walkStart;
        }

        public void setDestinationAndDuration(Duration totalCost, Station destination, Duration duration) {
            totalCostAtDestination = totalCost;
            this.destination = destination;
            this.duration = duration;
        }

        public WalkingToStationStage createStage(TramTime actualTime, Duration totalCostNow) {
            MyLocation walkStation = MyLocation.create(walkStart);
            logger.info("End walk to station " + destination.getId() + " duration " + duration);

            // offset for boarding cost
            Duration offset = totalCostNow.minus(totalCostAtDestination);

            TramTime walkStartTime = actualTime.minus(duration.plus(offset));
            return new WalkingToStationStage(walkStation, destination, duration, walkStartTime);
        }
    }

    private static class VehicleStagePending {

        private final StationRepositoryPublic stationRepository;
        private final TripRepository tripRepository;
        private final PlatformRepository platformRepository;

        private final ArrayList<Integer> stopSequenceNumbers;
        private final IdFor<Station> actionStationId;

        private final Duration costOffsetAtBoarding;
        private TramTime boardingTime;
        private IdFor<Platform> boardingPlatformId;

        public VehicleStagePending(StationRepositoryPublic stationRepository, TripRepository tripRepository,
                                   PlatformRepository platformRepository,
                                   IdFor<Station> actionStationId, Duration costOffsetAtBoarding) {
            this.stationRepository = stationRepository;
            this.tripRepository = tripRepository;
            this.platformRepository = platformRepository;
            this.actionStationId = actionStationId;
            this.costOffsetAtBoarding = costOffsetAtBoarding;
            this.stopSequenceNumbers = new ArrayList<>();
            this.boardingTime = null;
        }

        public void addPlatform(IdFor<Platform> boardingPlatformId) {
            this.boardingPlatformId = boardingPlatformId;
        }

        public VehicleStage createStage(final GraphNode routeStationNode, final Duration totalCost, final IdFor<Trip> tripId, final TransportMode mode) {
            final IdFor<Station> lastStationId = routeStationNode.getStationId();
            final Duration cost = totalCost.minus(costOffsetAtBoarding);

            if (logger.isDebugEnabled()) {
                logger.debug("Leave " + mode + " at " + lastStationId + "  cost = " + cost);
            }

            final Station firstStation = stationRepository.getStationById(actionStationId);
            final Station lastStation = stationRepository.getStationById(lastStationId);
            final Trip trip = tripRepository.getTripById(tripId);
            removeDestinationFrom(stopSequenceNumbers, trip, lastStationId);

            final VehicleStage vehicleStage = new VehicleStage(firstStation, trip.getRoute(), mode, trip, boardingTime,
                    lastStation, stopSequenceNumbers);
            vehicleStage.setCost(cost);
            if (boardingPlatformId != null) {
                if (platformRepository.hasPlatformId(boardingPlatformId)) {
                    final Platform platform = platformRepository.getPlatformById(boardingPlatformId);
                    vehicleStage.setBoardingPlatform(platform);
                }

            }

            return vehicleStage;
        }

        public void addStopSeqNumber(int stopSequenceNumber) {
            stopSequenceNumbers.add(stopSequenceNumber);
        }

        private void removeDestinationFrom(ArrayList<Integer> stopSequenceNumbers, Trip trip, IdFor<Station> lastStationId) {
            if (stopSequenceNumbers.isEmpty()) {
                return;
            }
            int lastIndex = stopSequenceNumbers.size() - 1;
            int lastJourneyStopsSequenceNumber = stopSequenceNumbers.get(lastIndex);
            StopCall finalPassed = trip.getStopCalls().getStopBySequenceNumber(lastJourneyStopsSequenceNumber);
            if (finalPassed.getStationId().equals(lastStationId)) {
                stopSequenceNumbers.remove(lastIndex);
            }
        }

        public void setBoardingTime(TramTime actualTime) {
            if (boardingTime==null) {
                boardingTime = actualTime;
            }
        }
    }
}
