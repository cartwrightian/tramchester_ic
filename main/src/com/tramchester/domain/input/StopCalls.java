package com.tramchester.domain.input;

import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class StopCalls {
    private static final Logger logger = LoggerFactory.getLogger(StopCalls.class);

    private final SortedMap<Integer, StopCall> orderedStopCalls;
    private final Map<IdFor<Station>, Integer> stationIndex;
    private final Trip parentTrip;
    private final List<StopLeg> legs;

    private boolean intoNextDay;

    public StopCalls(Trip parent) {
        this.parentTrip = parent;
        orderedStopCalls = new TreeMap<>();
        stationIndex = new HashMap<>();
        intoNextDay = false;
        legs = new ArrayList<>();
    }

    public void dispose() {
        orderedStopCalls.clear();
    }

    public void add(final StopCall stopCall) {
        final Station station = stopCall.getStation();
        if (station==null) {
            logger.error("Stop is missing station " + parentTrip.getId());
            return;
        }

        final int sequenceNumber = stopCall.getGetSequenceNumber();
        if (orderedStopCalls.containsKey(sequenceNumber)) {
            // this can happen as duplicated stop calls occur in tfgm data occasionally
            if (!stopCall.same(orderedStopCalls.get(sequenceNumber))) {
                logger.error(format("Different stop already present for trip %s, already had %s inserting %s ",
                        parentTrip.getId(), orderedStopCalls.get(sequenceNumber), stopCall));
            } else {
                logger.debug("Duplicated stopcall " + stopCall);
            }
        }
        orderedStopCalls.put(sequenceNumber, stopCall);
        stationIndex.put(station.getId(), sequenceNumber);
        intoNextDay = intoNextDay || stopCall.intoNextDay();
    }

    public long numberOfCallingPoints() {
        return orderedStopCalls.values().stream().filter(StopCall::callsAtStation).count();
    }

    public StopCall getStopBySequenceNumber(int callingNumber) {
        return orderedStopCalls.get(callingNumber);
    }

    public boolean callsAt(final HasId<Station> hasId) {
        return callsAt(hasId.getId());
    }

    public boolean callsAt(final IdFor<Station> stationId) {
        return stationIndex.containsKey(stationId);
    }

    public StopCall getStopFor(final IdFor<Station> stationId) {
        final int index = stationIndex.get(stationId);
        return orderedStopCalls.get(index);
    }

    public Stream<StopCall> stream() {
        return orderedStopCalls.values().stream();
    }

    @Override
    public String toString() {
        return "StopCalls{" +
                "orderedStopCalls=" + orderedStopCalls +
                ", parentTripId=" + parentTrip.getId() +
                ", intoNextDay=" + intoNextDay +
                '}';
    }

    /**
     * Create StopLeg for each pair of stopcall (a,b,c,d,e) -> (a,b), (b,c), (c,d), (d,e)
     * Respects the dropoff and pickup types so skips stopcalls that just pass a station
     * BUT does include legs that might only pickup or dropoff passengers
     * @param graphIsFiltered is filtering enabled, controls diagnostic messages
     */
    public List<StopLeg> getLegs(final boolean graphIsFiltered) {
        if (orderedStopCalls.isEmpty()) {
            String msg = "Missing stops, parent trip " + parentTrip;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        // called many times during graph build, so only populate the once
        if (legs.isEmpty()) {
            final Iterator<StopCall> stopsIter = orderedStopCalls.values().iterator();
            StopCall next = null;
            while (stopsIter.hasNext()) {
                final StopCall first = findNextStationStop(stopsIter, next);
                final StopCall second = findNextStationStop(stopsIter);
                if (first != null && second != null) {
                    final StopLeg stopLeg = new StopLeg(first, second);
                    legs.add(stopLeg);
                }
                next = second;
            }
            if (legs.isEmpty() && !graphIsFiltered) {
                logger.warn("No stop legs generated for " + this);
            }
        }
        return legs;
    }

    private StopCall findNextStationStop(final Iterator<StopCall> iter, final StopCall next) {
        if (next!=null) {
            if (next.callsAtStation()) {
                return next;
            }
        }
        return findNextStationStop(iter);
    }

    private StopCall findNextStationStop(final Iterator<StopCall> iter) {
        while (iter.hasNext()) {
            final StopCall current = iter.next();
            if (current.callsAtStation()) {
                return current;
            }
        }
        return null;
    }

    public boolean intoNextDay() {
        return intoNextDay;
    }

    public List<Station> getStationSequence(final boolean includeNotStopping) {
        return orderedStopCalls.values().stream().
                filter(stopCall -> includeNotStopping || stopCall.callsAtStation()).
                map(StopCall::getStation).collect(Collectors.toList());
    }

    public StopCall getFirstStop() {
        int firstKey = orderedStopCalls.firstKey();
        return orderedStopCalls.get(firstKey);
    }

    public StopCall getLastStop() {
        int lastKey = orderedStopCalls.lastKey();
        return orderedStopCalls.get(lastKey);
    }

    public long totalNumber() {
        return orderedStopCalls.size();
    }

    public boolean isEmpty() {
        return orderedStopCalls.isEmpty();
    }

    public Trip getTrip() {
        return parentTrip;
    }

    public static class StopLeg {
        private final StopCall first;
        private final StopCall second;
        private final Duration duration;

        private StopLeg(StopCall first, StopCall second) {
            this.first = first;
            this.second = second;
            duration = TramTime.difference(first.getDepartureTime(), second.getArrivalTime());
        }

        public StopCall getFirst() {
            return first;
        }

        public StopCall getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return "StopLeg{" +
                    "first=" + first +
                    ", second=" + second +
                    ", duration=" + duration +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StopLeg stopLeg = (StopLeg) o;

            if (!getFirst().equals(stopLeg.getFirst())) return false;
            return getSecond().equals(stopLeg.getSecond());
        }

        @Override
        public int hashCode() {
            int result = getFirst().hashCode();
            result = 31 * result + getSecond().hashCode();
            return result;
        }

        public Station getFirstStation() {
            return first.station;
        }

        public Station getSecondStation() {
            return second.station;
        }

        public TramTime getDepartureTime() {
            return first.getDepartureTime();
        }

        public Duration getCost() {
            return duration;
        }

        public StationIdPair getStations() {
            return StationIdPair.of(first.station.getId(), second.getStation().getId());
        }
    }
}
