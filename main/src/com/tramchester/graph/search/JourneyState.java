package com.tramchester.graph.search;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

public class JourneyState implements ImmutableJourneyState, JourneyStateUpdate {
    private static final Logger logger = LoggerFactory.getLogger(JourneyState.class);

    private final CoreState coreState;

    private Duration journeyOffset;
    private TramTime boardingTime;
    private TraversalState traversalState;
    private final IdSet<Trip> tripsDone;

    public JourneyState(TramTime queryTime, TraversalState traversalState) {
        coreState = new CoreState(queryTime);

        this.traversalState = traversalState;
        journeyOffset = Duration.ZERO;
        tripsDone = new IdSet<>();
    }

    public static JourneyState fromPrevious(ImmutableJourneyState previousState) {
        return new JourneyState((JourneyState) previousState);
    }

    // Copy cons
    // NOTE: vital to copy any collections here, otherwise different search branches interfere with each other
    private JourneyState(JourneyState previousState) {
        this.coreState = new CoreState(previousState.coreState);

        this.journeyOffset = previousState.journeyOffset;
        this.traversalState = previousState.traversalState;
        this.tripsDone = IdSet.copy(previousState.tripsDone);
        if (coreState.onBoard()) {
            this.boardingTime = previousState.boardingTime;
        }
    }

    public static InitialBranchState<JourneyState> initialState(TramTime queryTime, TraversalState traversalState) {
        return new InitialBranchState<>() {
            @Override
            public JourneyState initialState(Path path) {
                return new JourneyState(queryTime, traversalState);
            }

            @Override
            public InitialBranchState<JourneyState> reverse() {
                return null;
            }
        };
    }

    public TramTime getJourneyClock() {
        return coreState.journeyClock;
    }

    @Override
    public void updateTotalCost(Duration currentTotalCost) {
        Duration durationForTrip = currentTotalCost.minus(journeyOffset);

        if (coreState.onBoard()) {
            coreState.setJourneyClock(boardingTime.plus(durationForTrip));
        } else {
            coreState.incrementJourneyClock(durationForTrip);
        }
    }

    public void recordTime(TramTime boardingTime, Duration currentCost) throws TramchesterException {
        if ( !coreState.onBoard() ) {
            throw new TramchesterException("Not on a bus or tram");
        }
        coreState.setJourneyClock(boardingTime);
        this.boardingTime = boardingTime;
        this.journeyOffset = currentCost;
    }

    @Override
    public void beginWalk(GraphNode beforeWalkNode, boolean atStart, Duration unused) {
        coreState.incrementWalkingConnections();
    }

    @Override
    public void beginTrip(IdFor<Trip> newTripId) {
        // noop
    }

    @Override
    public void endWalk(GraphNode stationNode) {
        // noop
    }

    @Override
    public void toNeighbour(GraphNode startNode, GraphNode endNode, Duration cost) {
        coreState.incrementNeighbourConnections();
    }

    public void beginDiversion(IdFor<Station> stationId) {
        coreState.beginDiversion(stationId);
    }

    @Override
    public void seenStation(IdFor<Station> stationId) {
        coreState.seenStation(stationId);
    }

    @Override
    public void leave(IdFor<Trip> tripId, TransportMode mode, Duration totalDuration, GraphNode node) throws TramchesterException {
        if (!coreState.modeEquals(mode)) {
            throw new TramchesterException("Not currently on " +mode+ " was " + coreState.currentMode);
        }
        leave(totalDuration);
        tripsDone.add(tripId);
        coreState.leaveVehicle();
    }

    private void leave(Duration currentTotalCost) {
        if (Durations.lessThan(currentTotalCost, journeyOffset)) {
            throw new RuntimeException("Invalid total cost "+currentTotalCost+" less that current total offset " +journeyOffset);
        }

        Duration tripCost = currentTotalCost.minus(journeyOffset); //currentTotalCost - journeyOffset;

        coreState.setJourneyClock(boardingTime.plus(tripCost));

        journeyOffset = currentTotalCost;
        boardingTime = null;
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
    public void board(TransportMode mode, GraphNode node, boolean hasPlatform) throws TramchesterException {
        guardAlreadyOnboard();
        coreState.endDiversion(node);
        coreState.board(mode);
    }

    private void guardAlreadyOnboard() throws TramchesterException {
        if (!coreState.currentMode.equals(TransportMode.NotSet)) {
            throw new TramchesterException("Already on a " + coreState.currentMode);
        }
    }

    public TraversalState getTraversalState() {
        return traversalState;
    }

    @Override
    public String getTraversalStateName() {
        return traversalState.getClass().getSimpleName();
    }

    @Override
    public Duration getTotalDurationSoFar() {
        return traversalState.getTotalDuration();
    }

    @Override
    public boolean isOnDiversion() {
        return coreState.isOnDiversion();
    }

    @Override
    public boolean alreadyDeparted(IdFor<Trip> tripId) {
        return tripsDone.contains(tripId);
    }

    @Override
    public boolean hasVisited(IdFor<Station> stationId) {
        return coreState.visitedStations.contains(stationId);
    }

    public void updateTraversalState(TraversalState traversalState) {
        this.traversalState = traversalState;
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
                "coreState=" + coreState +
                ", journeyOffset=" + journeyOffset +
                ", boardingTime=" + boardingTime +
                ", traversalState=" + traversalState +
                '}';
    }



    private static class CoreState {
        private final List<IdFor<Station>> visitedStations;

        private boolean hasBegun;
        private TramTime journeyClock;
        private TransportMode currentMode;
        private int numberOfBoardings;
        private int numberOfWalkingConnections;
        private int numberNeighbourConnections;
        private int numberOfDiversionsTaken;
        private boolean currentlyOnDiversion;

        public CoreState(TramTime queryTime) {
            this(queryTime, false, 0,
                    TransportMode.NotSet, 0, 0, new LinkedList<>(),
                    false, 0);
        }

        // Copy cons
        public CoreState(CoreState previous) {
            this(previous.journeyClock, previous.hasBegun, previous.numberOfBoardings, previous.currentMode, previous.numberOfWalkingConnections,
                    previous.numberNeighbourConnections, previous.visitedStations,
                    previous.currentlyOnDiversion, previous.numberOfDiversionsTaken);
        }

        private CoreState(TramTime journeyClock, boolean hasBegun, int numberOfBoardings, TransportMode currentMode,
                          int numberOfWalkingConnections, int numberNeighbourConnections, List<IdFor<Station>> visitedStations,
                          boolean currentlyOnDiversion, int numberOfDiversionsTaken) {
            this.hasBegun = hasBegun;
            this.journeyClock = journeyClock;
            this.currentMode = currentMode;
            this.numberOfBoardings = numberOfBoardings;
            this.numberOfWalkingConnections = numberOfWalkingConnections;
            this.numberNeighbourConnections = numberNeighbourConnections;
            this.visitedStations = visitedStations;
            this.currentlyOnDiversion = currentlyOnDiversion;
        }

        public void incrementWalkingConnections() {
            numberOfWalkingConnections = numberOfWalkingConnections + 1;
        }

        public void incrementNeighbourConnections() {
            numberNeighbourConnections = numberNeighbourConnections + 1;
        }

        public void board(TransportMode mode) {
            numberOfBoardings = numberOfBoardings + 1;
            currentMode = mode;
            hasBegun = true;
        }

        public void setJourneyClock(TramTime time) {
            journeyClock = time;
        }

        public void incrementJourneyClock(Duration duration) {
            journeyClock = journeyClock.plus(duration);
        }

        public boolean onBoard() {
            return !currentMode.equals(TransportMode.NotSet);
        }

        public void leaveVehicle() {
            currentMode = TransportMode.NotSet;
        }

        public int getNumberOfChanges() {
            if (numberOfBoardings==0) {
                return 0;
            }
            int withoutDiversions =  numberOfBoardings-1; // initial boarding
            if (withoutDiversions==0) {
                return 0;
            }
            return Math.max(0, withoutDiversions-numberOfDiversionsTaken);

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CoreState coreState = (CoreState) o;

            if (hasBegun != coreState.hasBegun) return false;
            if (numberOfBoardings != coreState.numberOfBoardings) return false;
            if (numberOfWalkingConnections != coreState.numberOfWalkingConnections) return false;
            if (numberNeighbourConnections != coreState.numberNeighbourConnections) return false;
            if (!journeyClock.equals(coreState.journeyClock)) return false;
            return currentMode == coreState.currentMode;
        }

        @Override
        public int hashCode() {
            int result = (hasBegun ? 1 : 0);
            result = 31 * result + journeyClock.hashCode();
            result = 31 * result + currentMode.hashCode();
            result = 31 * result + numberOfBoardings;
            result = 31 * result + numberOfWalkingConnections;
            result = 31 * result + numberNeighbourConnections;
            return result;
        }

        @Override
        public String toString() {
            return "CoreState{" +
                    "visitedStations=" + visitedStations +
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

        public boolean modeEquals(TransportMode mode) {
            return currentMode==mode;
        }

        public void seenStation(IdFor<Station> stationId) {
            visitedStations.add(stationId);
        }

        public void endDiversion(GraphNode node) {
            if (currentlyOnDiversion) {
                //return getStationIdFrom(node.getNode());
                logger.info("End diversion at " + node.getStationId());
                this.currentlyOnDiversion = false;
            }
        }

        public void beginDiversion(IdFor<Station> stationId) {
            //IdFor<Station> stationId = diversionNode.getStationId();
            if (currentlyOnDiversion) {
                String msg = "Already on diversion, at " + stationId;
                logger.error(msg);
                // WIP TODO
                throw new RuntimeException(msg);
            } else {
                logger.info("Begin diversion at " + stationId);
                currentlyOnDiversion = true;
                numberOfDiversionsTaken = numberOfDiversionsTaken + 1;
            }
        }

        public boolean isOnDiversion() {
            return currentlyOnDiversion;
        }
    }

}
