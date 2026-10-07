package com.tramchester.graph.search.stateMachine.journeyState;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.search.stateMachine.journeyState.PassedStations.PassType.Diversion;
import static com.tramchester.graph.search.stateMachine.journeyState.PassedStations.PassType.JustBoarded;

public class PassedStations {
    private static final Logger logger = LoggerFactory.getLogger(PassedStations.class);

    public enum PassType {
        JustBoarded, OnTrip, EndTrip, Diversion
    }

    private final List<IdFor<Station>> passed;
    private final List<IdFor<Station>> boardingStations;
    private Pair<IdFor<Station>, PassType> lastPassed;

    public PassedStations(final PassedStations other) {
        passed = new ArrayList<>(other.passed);
        boardingStations = new ArrayList<>(other.boardingStations);
        if (other.lastPassed!=null) {
            lastPassed = Pair.of(other.lastPassed.getLeft(), other.lastPassed.getRight());
        }
    }

    public PassedStations() {
        passed = new ArrayList<>();
        boardingStations = new ArrayList<>();
        lastPassed = null;
    }

    public void record(final IdFor<Station> stationId, final PassType passType) {
        // Note: EndTrip not recorded
        // If we reboard at same station will be JustBoarded, otherwise
        // at destination, otherwise
        // leave station on diversion
        if (passType == PassType.JustBoarded) {
            boardingStations.add(stationId);
            //return;
        }

        if (passType == PassType.OnTrip | passType == PassType.JustBoarded){
            capturePass(stationId, passType);
        }
    }

    public void recordToNeighbour(final IdFor<Station> startOfDiversion) {
        if (passed.isEmpty()) {
            logger.info("At start, adding to On Trip Stations " + startOfDiversion);
            capturePass(startOfDiversion, Diversion);
            return;
        }

        if (passed.getLast().equals(startOfDiversion)) {
            logger.info("On diversion from " + startOfDiversion);
        } else {
            logger.info("Added diversion to On Trip Stations " + startOfDiversion);
            capturePass(startOfDiversion, Diversion);
        }
    }

    private void capturePass(final IdFor<Station> stationId, final PassType passType) {
        ImmutableEnumSet<PassType> previousMatchLegit = ImmutableEnumSet.of(JustBoarded);
        if (lastPassed!=null) {
            final IdFor<Station> previous = lastPassed.getKey();
            if (stationId.equals(previous)) {
                if (!previousMatchLegit.contains(passType)) {
                    // TODO FIX THIS!
                    String msg = "Matches previous %s for %s %s".formatted(lastPassed, stationId, passType);
                    logger.error(msg);
                }
            } else {
                passed.add(stationId);
            }
        } else {
            // first station
            passed.add(stationId);
        }
        lastPassed = Pair.of(stationId, passType);

    }

    public boolean alreadyBoardedAt(final IdFor<Station> stationId) {
        return boardingStations.contains(stationId);
    }

    public boolean hasAlreadySeen(final IdFor<Station> stationId) {
        if (passed.isEmpty()) {
            return false;
        }
        final IdFor<Station> lastStationPassed = lastPassed.getKey();
        if (stationId.equals(lastStationPassed)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Just passed " + lastPassed + " for " + passed);
            }
            return false;
        }

        boolean matches = passed.contains(stationId);
        if (matches && logger.isDebugEnabled()) {
            logger.debug("Already seen %s in %s".formatted(stationId, passed));
        }
        return matches;
    }

    @Override
    public String toString() {
        return "PassedStations{" +
                "passed=" + passed +
                ", boardingStations=" + boardingStations +
                '}';
    }
}
