package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.search.stateMachine.states.ImmutableTraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JourneyState implements ImmutableJourneyState, JourneyStateUpdate {

    // GraphState -> JourneyState -> TraversalState

    private static final Logger logger = LoggerFactory.getLogger(JourneyState.class);

    private final CoreState coreState;
    private TramDuration journeyOffset;
    private TramTime boardingTime;
    private ImmutableTraversalState traversalState;
    private final IdSet<Trip> tripsDone;
    private IdFor<Trip> currentTrip;

    public JourneyState(final TramTime queryTime, final TraversalState traversalState) {
        coreState = new CoreState(queryTime);

        this.traversalState = traversalState;
        journeyOffset = TramDuration.ZERO;
        tripsDone = new IdSet<>();
        currentTrip = Trip.InvalidId();
    }

    public static JourneyState fromPrevious(final ImmutableJourneyState previousState) {
        return new JourneyState((JourneyState) previousState);
    }

    // Copy cons
    // NOTE: vital to copy any collections here, otherwise different search branches interfere with each other
    private JourneyState(final JourneyState previousState) {
        this.coreState = new CoreState(previousState.coreState);

        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        this.tripsDone = IdSet.copy(previousState.tripsDone);
        this.currentTrip = previousState.currentTrip;
        if (coreState.onBoard()) {
            this.boardingTime = previousState.boardingTime;
        }
    }

    public void updateTraversalState(final ImmutableTraversalState traversalState) {
        this.traversalState = traversalState;
    }

    @Override
    public void updateTotalCost(final TramDuration currentTotalCost) {
        final TramDuration durationForTrip = currentTotalCost.minus(journeyOffset);

        if (coreState.onBoard()) {
            coreState.setJourneyClock(boardingTime.plus(durationForTrip));
        } else {
            coreState.incrementJourneyClock(durationForTrip);
        }
    }

    public void recordTime(final TramTime boardingTime, final TramDuration currentCost) throws TramchesterException {
        if ( !coreState.onBoard() ) {
            throw new TramchesterException("Not on a bus or tram");
        }
        coreState.setJourneyClock(boardingTime);
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
    }

    @Override
    public void seenRouteStation(final GraphNode node) {
        coreState.seenRouteStation(node);
    }

    @Override
    public void seenStationGroup(IdFor<StationLocalityGroup> stationGroupId) {
        coreState.seenStationGroup(stationGroupId);
    }

    @Override
    public void beginWalk(final GraphNode beforeWalkNode, final boolean atStart, final TramDuration unused) {
        coreState.incrementWalkingConnections();
    }

    @Override
    public void endWalk(final GraphNode stationNode) {
        // noop
    }

    @Override
    public void toNeighbour(final GraphNode startNode, final GraphNode endNode, final TramDuration cost) {
        coreState.incrementNeighbourConnections();
    }

    @Override
    public void beginDiversion(final IdFor<Station> stationId) {
        coreState.beginDiversion(stationId);
    }

    @Override
    public boolean onDiversion() {
        return coreState.isOnDiversion();
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
    public void seenStation(final IdFor<Station> stationId) {
        coreState.seenStation(stationId);
    }

    @Override
    public void leave(final TransportMode mode, final TramDuration totalDuration, final GraphNode node) throws TramchesterException {
        if (!currentTrip.isValid()) {
            throw new TramchesterException("Trying to leave a trip, not on a trip");
        }
        if (!coreState.modeEquals(mode)) {
            throw new TramchesterException("Not currently on " +mode+ " was " + coreState.currentMode);
        }
        leave(totalDuration);
        tripsDone.add(currentTrip);
        coreState.leaveVehicle();

        currentTrip = Trip.InvalidId();
    }

    @Override
    public void beginTrip(final IdFor<Trip> newTripId) {
        if (currentTrip.isValid()) {
            throw new RuntimeException("Attempted to start new trip " + newTripId + " when already on " + currentTrip);
        }
        this.currentTrip = newTripId;
    }

    private void leave(final TramDuration currentTotalCost) {
        if (Durations.lessThan(currentTotalCost, journeyOffset)) {
            throw new RuntimeException("Invalid total cost "+currentTotalCost+" less that current total offset " +journeyOffset);
        }

        final TramDuration tripCost = currentTotalCost.minus(journeyOffset); //currentTotalCost - journeyOffset;

        coreState.setJourneyClock(boardingTime.plus(tripCost));

        journeyOffset = currentTotalCost;
        boardingTime = null;
    }


    @Override
    public TramTime getJourneyClock() {
        return coreState.journeyClock;
    }

    @Override
    public int getNumberChanges() {
        return coreState.getNumberOfChanges();
    }

    @Override
    public int getNumberWalkingConnections() {
        return coreState.numberOfWalkingConnections;
    }

    @Override
    public boolean hasBegunJourney() {
        return coreState.hasBegun;
    }

    @Override
    public int getNumberNeighbourConnections() {
        return coreState.numberNeighbourConnections;
    }

    @Override
    public void board(final TransportMode mode, final GraphNode node, final boolean hasPlatform) throws TramchesterException {
        guardAlreadyOnboard();
        coreState.endDiversion(node);
        coreState.board(mode);
    }

    private void guardAlreadyOnboard() throws TramchesterException {
        if (!coreState.currentMode.equals(TransportMode.NotSet)) {
            throw new TramchesterException("Already on a " + coreState.currentMode);
        }
    }

    @Override
    public ImmutableTraversalState getTraversalState() {
        return traversalState;
    }

    @Override
    public TraversalStateType getTraversalStateType() {
        return traversalState.getStateType();
    }

    @Override
    public TramDuration getTotalDurationSoFar() {
        return traversalState.getTotalDuration();
    }

    @Override
    public boolean alreadyDeparted(final IdFor<Trip> tripId) {
        return tripsDone.contains(tripId);
    }

    @Override
    public GraphNodeId getNodeId() {
        return traversalState.nodeId();
    }

    @Override
    public LocationId<?> approxPosition() {
        return coreState.getLastVisited();
    }

//    @Override
//    public boolean justBoarded() {
//        return traversalState.getStateType().equals(TraversalStateType.JustBoardedState);
//    }

    @Override
    public boolean duplicatedBoardingSeen() {
        return coreState.duplicatedBoardingSeen();
    }

    @Override
    public boolean justBoarded() {
        return traversalState.getStateType() == TraversalStateType.JustBoardedState;
    }

    @Override
    public TransportMode getTransportMode() {
        return coreState.currentMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JourneyState that = (JourneyState) o;

        return coreState.equals(that.coreState);
    }

    @Override
    public int hashCode() {
        return coreState.hashCode();
    }

    @Override
    public String toString() {
        return "JourneyState{" +
                "traversalState=" + traversalState +
                ", coreState=" + coreState +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", currentTrip=" + currentTrip +
                '}';
    }

    private static class CoreState {

        private boolean hasBegun;
        private TramTime journeyClock;
        private TransportMode currentMode;
        private int numberOfBoardings;
        private int numberOfWalkingConnections;
        private int numberNeighbourConnections;
        private int numberOfDiversionsTaken;
        private boolean currentlyOnDiversion;
        private LocationId<?> lastSeenStation;
        private final List<LocationId<?>> boardingLocations;
        private boolean duplicatedBoardingSeen;

        public CoreState(final TramTime queryTime) {
            this(queryTime, false, 0,
                    TransportMode.NotSet, 0, 0,
                    false, 0, LocationId.wrap(Station.InvalidId()), new ArrayList<>(), false);
        }

        // COPY cons
        // NOTE: Don't pass by Ref, create duplicates for collections etc
        public CoreState(final CoreState previous) {
            this(previous.journeyClock, previous.hasBegun, previous.numberOfBoardings, previous.currentMode, previous.numberOfWalkingConnections,
                    previous.numberNeighbourConnections,
                    previous.currentlyOnDiversion, previous.numberOfDiversionsTaken, previous.lastSeenStation.copy(),
                    new ArrayList<>(previous.boardingLocations),
                    //false);
                    previous.duplicatedBoardingSeen);
        }

        private CoreState(final TramTime journeyClock, final boolean hasBegun, final int numberOfBoardings,
                                                  final TransportMode currentMode, final int numberOfWalkingConnections,
                                                  final int numberNeighbourConnections, final boolean currentlyOnDiversion,
                                                  final int numberOfDiversionsTaken, final LocationId<?> lastSeenStation,
                                                  final List<LocationId<?>> boardingLocations,
                                                    final boolean duplicatedBoardingSeen) {
            this.hasBegun = hasBegun;
            this.journeyClock = journeyClock;
            this.currentMode = currentMode;
            this.numberOfBoardings = numberOfBoardings;
            this.numberOfWalkingConnections = numberOfWalkingConnections;
            this.numberNeighbourConnections = numberNeighbourConnections;
            this.currentlyOnDiversion = currentlyOnDiversion;
            this.numberOfDiversionsTaken = numberOfDiversionsTaken;
            this.lastSeenStation = lastSeenStation;
            this.boardingLocations = boardingLocations;
            this.duplicatedBoardingSeen = duplicatedBoardingSeen;
        }

        public void incrementWalkingConnections() {
            numberOfWalkingConnections = numberOfWalkingConnections + 1;
        }

        public void incrementNeighbourConnections() {
            numberNeighbourConnections = numberNeighbourConnections + 1;
        }

        public void board(final TransportMode mode) {
            numberOfBoardings = numberOfBoardings + 1;
            if (boardingLocations.contains(lastSeenStation)) {
                // check if occurred earlier
                if (!boardingLocations.getLast().equals(lastSeenStation)) {
                    duplicatedBoardingSeen = true;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Duplicated boarding (" + numberOfBoardings + ") at " + lastSeenStation + " and boardings " + boardingLocations);
                    }
                }
            } else {
                boardingLocations.add(lastSeenStation);
            }
            currentMode = mode;
            hasBegun = true;
        }

        public void setJourneyClock(final TramTime time) {
            journeyClock = time;
        }

        public void incrementJourneyClock(final TramDuration duration) {
            journeyClock = journeyClock.plusRounded(duration);
        }

        public boolean onBoard() {
            return currentMode!=TransportMode.NotSet;
        }

        public void leaveVehicle() {
            currentMode = TransportMode.NotSet;
        }

        public int getNumberOfChanges() {
            if (numberOfBoardings==0) {
                return 0;
            }
            final int withoutDiversions =  numberOfBoardings-1; // initial boarding
            if (withoutDiversions==0) {
                return 0;
            }
            return Math.max(0, withoutDiversions-numberOfDiversionsTaken);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            CoreState coreState = (CoreState) o;
            return hasBegun == coreState.hasBegun && numberOfBoardings == coreState.numberOfBoardings &&
                    numberOfWalkingConnections == coreState.numberOfWalkingConnections &&
                    numberNeighbourConnections == coreState.numberNeighbourConnections &&
                    numberOfDiversionsTaken == coreState.numberOfDiversionsTaken &&
                    currentlyOnDiversion == coreState.currentlyOnDiversion &&
                    duplicatedBoardingSeen == coreState.duplicatedBoardingSeen &&
                    Objects.equals(journeyClock, coreState.journeyClock) &&
                    currentMode == coreState.currentMode &&
                    Objects.equals(lastSeenStation, coreState.lastSeenStation) &&
                    Objects.equals(boardingLocations, coreState.boardingLocations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hasBegun, journeyClock, currentMode, numberOfBoardings, numberOfWalkingConnections,
                    numberNeighbourConnections, numberOfDiversionsTaken, currentlyOnDiversion,
                    lastSeenStation, boardingLocations, duplicatedBoardingSeen);
        }

        @Override
        public String toString() {
            return "CoreState{" +
                    ", hasBegun=" + hasBegun +
                    ", journeyClock=" + journeyClock +
                    ", currentMode=" + currentMode +
                    ", numberOfBoardings=" + numberOfBoardings +
                    ", numberOfWalkingConnections=" + numberOfWalkingConnections +
                    ", numberNeighbourConnections=" + numberNeighbourConnections +
                    ", numberOfDiversionsTaken=" + numberOfDiversionsTaken +
                    ", currentlyOnDiversion=" + currentlyOnDiversion +
                    '}';
        }

        public boolean modeEquals(final TransportMode mode) {
            return currentMode==mode;
        }

        public void seenStation(final IdFor<Station> stationId) {
            lastSeenStation = LocationId.wrap(stationId);
        }

        public void seenRouteStation(final GraphNode node) {
            lastSeenStation = LocationId.wrap(node.getStationId());
        }

        public void seenStationGroup(final IdFor<StationLocalityGroup> stationGroupId) {
            lastSeenStation = LocationId.wrap(stationGroupId);
        }

        public void endDiversion(final GraphNode node) {
            if (currentlyOnDiversion) {
                if (logger.isDebugEnabled()) {
                    logger.debug("End diversion at " + node.getStationId());
                }
                currentlyOnDiversion = false;
            }
        }

        public void beginDiversion(final IdFor<Station> stationId) {
            if (currentlyOnDiversion) {
                String msg = "Already on diversion, at " + stationId;
                logger.error(msg);
                // WIP TODO
                throw new RuntimeException(msg);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Begin diversion at " + stationId);
                }
                currentlyOnDiversion = true;
                numberOfDiversionsTaken = numberOfDiversionsTaken + 1;
            }
        }

        public boolean isOnDiversion() {
            return currentlyOnDiversion;
        }

        public LocationId<?> getLastVisited() {
            return lastSeenStation;
        }

        public boolean duplicatedBoardingSeen() {
            return duplicatedBoardingSeen;
        }
    }

}
