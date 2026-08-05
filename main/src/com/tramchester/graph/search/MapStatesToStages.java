package com.tramchester.graph.search;

import com.tramchester.domain.Platform;
import com.tramchester.domain.StationGroup;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.*;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class MapStatesToStages implements JourneyStateUpdate {
    private static final Logger logger = LoggerFactory.getLogger(MapStatesToStages.class);
    private State state;

    private enum State {
        NotStarted,
        Boarded,
        BoardedTimeRecorded,
        OnTrip,
        OnTripTimeRecorded,
        WalkAtStart,
        Walk,
        Waiting,
        WalkDuring,
        ToNeighbour,
        Destination
    }

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TripRepository tripRepository;

    private final List<TransportStage<?, ?>> stages;

    private final TramTime queryTime;
    private TramTime lastVehicleArrivalTime; // updated when leave a vehicle
    private TramTime timeAtLastMinuteNode; // updated each time pass minute node and know 'actual' time

    private TramDuration totalCost; // total cost of entire journey

    @Deprecated
    private TramDuration costOffsetAtActual; // total cost at point got 'actual' time update

    private WalkPending walkingPending;
    private VehicleStagePending vehicleStagePending;

    private IdFor<Trip> currentTrip;
    private boolean onDiversion;

    public MapStatesToStages(StationRepository stationRepository, PlatformRepository platformRepository,
                             TripRepository tripRepository, TramTime queryTime) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.tripRepository = tripRepository;
        this.queryTime = queryTime;

        timeAtLastMinuteNode = TramTime.invalid();
        lastVehicleArrivalTime = TramTime.invalid();

        stages = new ArrayList<>();
        totalCost = TramDuration.ZERO;
        costOffsetAtActual = TramDuration.ZERO;
        onDiversion = false;
        currentTrip = Trip.InvalidId();

        state = State.NotStarted;
    }

    @Override
    public void board(final TransportMode transportMode, final GraphNode node, final boolean hasPlatform) {
        stateTransition(List.of(State.NotStarted, State.Waiting, State.ToNeighbour),
                List.of(State.Boarded, State.Boarded, State.Boarded));

        final IdFor<Station> actionStationId = node.getStationId();

        if (onDiversion) {
            logger.info("End diversion at " + actionStationId);
            onDiversion = false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Board " + transportMode + " " + actionStationId + " totalcost  " + totalCost);
        }
        vehicleStagePending = VehicleStagePending.board(stationRepository, tripRepository, platformRepository,
                actionStationId, totalCost);
        if (hasPlatform) {
            final IdFor<Platform> boardingPlatformId = node.getPlatformId();
            vehicleStagePending.addPlatform(boardingPlatformId);
        }
    }

    private State stateTransition(State allowed, State target) {
        return stateTransition(List.of(allowed), List.of(target));
    }

    private State stateTransition(List<State> allowed, List<State> targets) {
        if (allowed.size()!=targets.size()) {
            throw new RuntimeException("Mismatch on allowed " + allowed + " and targets " + targets);
        }
        if (!allowed.contains(state)) {
            throw new RuntimeException("Wrong state " + state + " Expected " + allowed);
        }
        State previous = state;
        int index = allowed.indexOf(state);
        state = targets.get(index);
        return previous;
    }

    @Override
    public void recordTimeAtMinuteNode(final TramTime timeAtMinuteNode, final TramDuration totalCost) {
        State previousState = stateTransition(List.of(State.Boarded, State.OnTrip, State.OnTripTimeRecorded),
                List.of(State.BoardedTimeRecorded, State.OnTripTimeRecorded, State.OnTripTimeRecorded));

        logger.debug("Record actual time " + timeAtMinuteNode + " total cost:" + totalCost);
        this.timeAtLastMinuteNode = timeAtMinuteNode;
        costOffsetAtActual = totalCost;

        if (previousState==State.Boarded) {
            vehicleStagePending.setBoardingTime(timeAtLastMinuteNode);
        }

        // Walking -> ???
        if (walkingPending != null) {
            WalkingStage<? extends Location<?>, ? extends Location<? extends Location<?>>> walkingToStationStage = walkingPending.createStage(timeAtMinuteNode);
            logger.info("Add " + walkingToStationStage);
            stages.add(walkingToStationStage);
            walkingPending = null;
        }
    }

    @Override
    public void leave(final TransportMode mode, final TramDuration currentTotalCost, final GraphNode routeStationNode) {
        stateTransition(State.OnTripTimeRecorded,  State.Waiting);

        // TODO
        // currentTotalCost should always be the same as this.totalCost

        if (!currentTrip.isValid()) {
            throw new RuntimeException("Not on a trip");
        }

        final IdFor<Station> stationId = routeStationNode.getStationId();

        final VehicleStage vehicleStage = vehicleStagePending.createStageAtLeave(stationId, currentTotalCost, currentTrip, mode);
        stages.add(vehicleStage);

        logger.info(format("Leave: At %s query:%s Last Minute Seen:%s Total Cost: %s Stage Departure: %s",
                stationId, queryTime, timeAtLastMinuteNode, currentTotalCost, vehicleStage.getFirstDepartureTime()));

        lastVehicleArrivalTime = vehicleStage.getExpectedArrivalTime();

        if (!lastVehicleArrivalTime.equals(timeAtLastMinuteNode)) {
            logger.warn(format("Mismatch between arrival time %s and last minute seen %s for stage %s",
                    lastVehicleArrivalTime, timeAtLastMinuteNode, vehicleStage));
        }


        if (logger.isDebugEnabled()) {
            logger.debug("Added " + vehicleStage);
        }
        currentTrip = Trip.InvalidId();
    }

    protected void passStop(final GraphRelationship fromMinuteNodeRelationship) {
        stateTransition(State.OnTripTimeRecorded, State.OnTripTimeRecorded);
        logger.debug("pass stop");
        int stopSequenceNumber = fromMinuteNodeRelationship.getStopSeqNumber();
        vehicleStagePending.addStopSeqNumber(stopSequenceNumber);
    }

    @Override
    public void updateTotalCost(final TramDuration total) {
        this.totalCost = total;
    }

    // TODO too many ways of calculating times/totals etc
    @Deprecated
    private TramTime getActualClock() {
        if (timeAtLastMinuteNode.isValid()) {
            return timeAtLastMinuteNode.plusRounded(totalCost.minus(costOffsetAtActual));
        } else {
            throw new RuntimeException("No valid time yet, state is " + state);
        }
    }

    @Override
    public void beginTrip(final IdFor<Trip> newTripId) {
        stateTransition(State.BoardedTimeRecorded, State.OnTripTimeRecorded);
        logger.debug("Begin trip:" + newTripId);
        this.currentTrip = newTripId;
    }

    @Override
    public void beginWalk(final GraphNode beforeWalkNode) {
        stateTransition(State.NotStarted, State.WalkAtStart);

        final Location<?> walkStart;
        if (beforeWalkNode.hasLabel(GraphLabel.STATION)) {
            IdFor<Station> startId = beforeWalkNode.getStationId();
            walkStart = stationRepository.getStationById(startId);
        } else if (beforeWalkNode.hasLabel(GraphLabel.QUERY_NODE)) {
            final LatLong latLong = beforeWalkNode.getLatLong();
            walkStart = MyLocation.create(latLong);
        } else {
            throw new RuntimeException("Not implemented for " + beforeWalkNode);
        }

        walkingPending = new WalkPending(walkStart, queryTime);
    }

    @Override
    public void beginWalk(final GraphNode beforeWalkNode, final TramDuration previousCost) {
        State previousState = stateTransition(List.of(State.Waiting, State.NotStarted),
                List.of(State.WalkDuring, State.WalkAtStart));

        final Location<?> walkStart;
        if (beforeWalkNode.hasLabel(GraphLabel.STATION)) {
            IdFor<Station> startId = beforeWalkNode.getStationId();
            walkStart = stationRepository.getStationById(startId);
        } else if (beforeWalkNode.hasLabel(GraphLabel.QUERY_NODE)) {
            final LatLong latLong = beforeWalkNode.getLatLong();
            walkStart = MyLocation.create(latLong);
        } else {
            throw new RuntimeException("Not implemented for " + beforeWalkNode);
        }

        if (previousState==State.Waiting) {
            if (!lastVehicleArrivalTime.isValid()) {
                throw new RuntimeException("No valid time to use for last vehicle arrival time");
            }
            walkingPending = new WalkPending(walkStart, previousCost, lastVehicleArrivalTime);
            lastVehicleArrivalTime = TramTime.invalid();
        } else {
            walkingPending = new WalkPending(walkStart, queryTime);
        }
    }

    @Override
    public void endWalk(final GraphNode endWalkNode, final TramDuration cost) {
        State previousState = stateTransition(List.of(State.WalkAtStart, State.WalkDuring), List.of(State.Waiting, State.Waiting));

        if (walkingPending != null) {
            boolean atStation = endWalkNode.hasLabel(GraphLabel.STATION);
            if (atStation) {
                final IdFor<Station> destinationStationId = endWalkNode.getStationId();
                final Station destination = stationRepository.getStationById(destinationStationId);
                walkingPending.setDestinationAndCost(destination, cost);
            }  else {
                if (previousState==State.WalkAtStart) {
                    final LatLong destLatLong = endWalkNode.getLatLong();
                    walkingPending.setDestination(MyLocation.create(destLatLong));
                } else {
                    final LatLong destLatLong = endWalkNode.getLatLong();
                    walkingPending.setDestinationAndCost(MyLocation.create(destLatLong), cost);
                }
            }
        } else {
            throw new RuntimeException("Unexpected end of walk not form a station " +state);
        }

    }

    @Override
    public void toNeighbour(final GraphNode startNode, final GraphNode endNode, final TramDuration cost) {
        State previousState = stateTransition(List.of(State.NotStarted, State.Waiting),
                List.of(State.ToNeighbour, State.ToNeighbour));

        final IdFor<Station> startId = startNode.getStationId();
        final IdFor<Station> endId = endNode.getStationId();

        if (walkingPending !=null) {
            String message = format("Skip connect stage as already on a walk, start %s end %s cost %s ",
                    startId, endId, cost);
            logger.info(message);
        } else {
            final Station start = stationRepository.getStationById(startId);
            final Station end = stationRepository.getStationById(endId);
            TramTime connectingStageBegin = (previousState==State.NotStarted) ? queryTime : getActualClock();
            final ConnectingStage<Station, Station> connectingStage = new ConnectingStage<>(start, end, cost, connectingStageBegin);
            logger.info("Added connecting stage " + connectingStage);
            stages.add(connectingStage);
        }
    }

    @Override
    public void recordStation(IdFor<Station> stationId) {
        // no-op
    }

    @Override
    public void recordStationGroup(IdFor<StationGroup> stationGroupId) {
        // no-op
    }


    @Override
    public void recordRouteStation(GraphNode node) {
        // no-op
    }

    @Override
    public void beginDiversion(final IdFor<Station> stationId) {
        if (onDiversion) {
            throw new RuntimeException("Already on diversion at " + stationId);
        }
        logger.info("Begin diversion at " + stationId);
        onDiversion = true;
    }

    @Override
    public boolean onDiversion() {
        return onDiversion;
    }

    @Override
    public boolean onTrip() {
        return currentTrip.isValid();
    }

    @Override
    public IdFor<Trip> getCurrentTrip() {
        return currentTrip;
    }

    @Override
    public boolean alreadyPassed(IdFor<Station> stationId) {
        // noop
        return false;
    }

    @Override
    public void atDestination(final TramDuration cost) {
        stateTransition(List.of(State.Waiting, State.NotStarted, State.ToNeighbour),
                List.of(State.Destination, State.Destination, State.Destination));

        if (walkingPending != null) {
            WalkingStage<?,?> walkingStage = walkingPending.createStage(totalCost);
            logger.info("Add final pending walking stage " + walkingStage);
            stages.add(walkingStage);
        }
    }


    public List<TransportStage<?, ?>> getStages() {
        stateTransition(State.Destination, State.Destination);
        return stages;
    }

    private static class WalkPending {

        private final Location<?> walkStart;
        private boolean startTimePending;
        private Location<?> walkDest;

        private TramDuration duration;
        private TramTime startTime;

        private WalkPending(Location<?> walkStart, TramDuration duration, TramTime startTime, boolean startTimePending) {
            this.walkStart = walkStart;
            this.duration = duration;
            this.startTime = startTime;
            this.startTimePending = startTimePending;
        }

        public WalkPending(Location<?> walkStart, TramDuration duration, TramTime startTime) {
            this(walkStart, duration, startTime, false);
        }

        public WalkPending(Location<?> walkStart, TramTime startTime) {
            this(walkStart, TramDuration.getInvalid(), startTime, true);
        }

        public void setDestination(final Location<?> destination) {
            this.walkDest = destination;
        }

        public void setDestinationAndCost(Location<?> destination, TramDuration duration) {
            this.walkDest = destination;
            if (!this.duration.isValid()) {
                this.duration = duration;
            } else {
                if (!duration.equals(TramDuration.ZERO)) {
                    throw new RuntimeException("Already had a duration set " +
                            this.duration + " but got " + duration + " when expecting ZERO");
                }
            }
        }

        public WalkingStage<?, ?> createStage(TramDuration actualCost) {
            if (!duration.isValid()) {
                duration = actualCost;
            } else {
                if (!actualCost.equals(duration)) {
                    logger.warn(format("Mismatch on durations, had %s then got %s", duration, actualCost));
                }
            }
            return createStage();
        }

        public WalkingStage<? extends Location<?>, ? extends Location<? extends Location<?>>> createStage(final TramTime time) {
            if (startTimePending) {
                startTime = time.minusRounded(duration);
                startTimePending = false;
            }
            return createStage();
        }

        public WalkingStage<? extends Location<?>, ? extends Location<? extends Location<?>>> createStage() {
            logger.info(format("End walk from %s to %s %s %s", walkStart.getId(), walkDest.getId(), startTime, duration));

            if (walkStart.getLocationType() == LocationType.Station) {

                if (walkDest.getLocationType()==LocationType.MyLocation) {
                    return new WalkingFromStationStage((Station) walkStart, (MyLocation) walkDest, duration, startTime);
                }
                else {
                    throw new RuntimeException("Not implemented " + this);
                }

            } else { // start is a location

                if (walkDest.getLocationType()==LocationType.Station) {
                    return new WalkingToStationStage(walkStart, (Station) walkDest, duration, startTime);
                } else {
                    throw new RuntimeException("Not implemented for " + this);
                }
            }

        }

        @Override
        public String toString() {
            return "WalkFromStartPending{" +
                    "walkStart=" + walkStart.getId() +
                    ", walkDest=" + walkDest.getId() +
                    ", duration=" + duration +
                    ", startTime=" + startTime +
                    '}';
        }

    }

    private static class VehicleStagePending {

        private final StationRepositoryPublic stationRepository;
        private final TripRepository tripRepository;
        private final PlatformRepository platformRepository;

        private final ArrayList<Integer> stopSequenceNumbers;
        private final IdFor<Station> actionStationId;

        private final TramDuration costAtBoardingPoint;
        private TramTime boardingTime;
        private IdFor<Platform> boardingPlatformId;

        private VehicleStagePending(StationRepositoryPublic stationRepository, TripRepository tripRepository,
                                   PlatformRepository platformRepository,
                                   IdFor<Station> actionStationId, TramDuration costAtBoardingPoint) {
            this.stationRepository = stationRepository;
            this.tripRepository = tripRepository;
            this.platformRepository = platformRepository;
            this.actionStationId = actionStationId;
            this.costAtBoardingPoint = costAtBoardingPoint;
            this.stopSequenceNumbers = new ArrayList<>();
            this.boardingTime = null;
        }

        public static VehicleStagePending board(StationRepository stationRepository, TripRepository tripRepository,
                                                PlatformRepository platformRepository, IdFor<Station> actionStationId,
                                                TramDuration costAtBoardingPoint) {
            return new VehicleStagePending(stationRepository, tripRepository, platformRepository, actionStationId, costAtBoardingPoint);
        }

        public void addPlatform(IdFor<Platform> boardingPlatformId) {
            this.boardingPlatformId = boardingPlatformId;
        }

        public void setBoardingTime(final TramTime actualTime) {
            // actual boarding time at MinuteNode
            if (boardingTime==null) {
                boardingTime = actualTime;
            }
        }

        public VehicleStage createStageAtLeave(final IdFor<Station> lastStationId, final TramDuration currentTotalCost,
                                               final IdFor<Trip> tripId, final TransportMode mode) {

            final TramDuration costForStage = currentTotalCost.minus(costAtBoardingPoint);

            logger.info("Leave " + mode + " at " + lastStationId + "  costForStage = " + costForStage +
                    " totalCost = " + currentTotalCost);

            final Station firstStation = stationRepository.getStationById(actionStationId);
            final Station lastStation = stationRepository.getStationById(lastStationId);
            final Trip trip = tripRepository.getTripById(tripId);
            removeDestinationFrom(stopSequenceNumbers, trip, lastStationId);

            final VehicleStage vehicleStage = new VehicleStage(firstStation, trip.getRoute(), mode, trip, boardingTime,
                    lastStation, stopSequenceNumbers);
            vehicleStage.setCost(costForStage);

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

    }
}
