package com.tramchester.domain.input;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class MutableTrip implements Trip {
    private static final Logger logger = LoggerFactory.getLogger(MutableTrip.class);

    private final IdFor<Trip> tripId;
    private final String headSign;
    private final Service service;
    private final Route route;
    private final StopCalls stopCalls;
    private final TransportMode actualMode; // used for things like RailReplacementBus where parent route has different mode

    private boolean filtered; // at least one station on this trip was filtered out

    public MutableTrip(final IdFor<Trip> tripId, String headSign, Service service, Route route, TransportMode actualMode) {
        this.tripId = tripId;
        this.headSign = headSign.intern();
        this.service = service;
        this.route = route;
        this.actualMode = actualMode;

        filtered = false;
        stopCalls = new StopCalls(tripId);
    }

    // test support
    public static Trip build(IdFor<Trip> tripId, String headSign, Service service, Route route) {
        return new MutableTrip(tripId, headSign, service, route, route.getTransportMode());
    }

    public static IdFor<Trip> createId(final String text) {
        return StringIdFor.createId(text, Trip.class);
    }

    public void dispose() {
        stopCalls.dispose(); // needed for test memory issues
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutableTrip trip = (MutableTrip) o;
        return tripId.equals(trip.tripId);
    }

    @Override
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }

    @Override
    public IdFor<Trip> getId() {
        return tripId;
    }

    @Override
    public StopCalls getStopCalls() {
        return stopCalls;
    }

    public void addStop(final StopCall stop) {
        stopCalls.add(stop);
    }

    @Override
    public String toString() {
        return "MutableTrip{" +
                "tripId=" + tripId +
                ", headSign='" + headSign + '\'' +
                ", service=" + HasId.asId(service) +
                ", route=" + HasId.asId(route) +
                ", stopCalls=" + stopCalls +
                ", actualMode=" + actualMode +
                ", filtered=" + filtered +
                '}';
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public String getHeadsign() {
        return headSign;
    }

    @Override
    public Route getRoute() {
        return route;
    }

    @Override
    public TransportMode getTransportMode() {
        return actualMode;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.TRIP_ID;
    }

    public void setFiltered(boolean flag) {
        this.filtered = flag;
    }

    /***
     * Set if one or more stops has been filtered out, for example due to geographical bounds
     * @return filtered or not
     */
    @Override
    public boolean isFiltered() {
        return filtered;
    }

    @Override
    public boolean intoNextDay() {
        return stopCalls.intoNextDay();
    }

    @Override
    public TramTime departTime() {
        if (stopCalls.isEmpty()) {
            throw new RuntimeException("Cannot get stopCalls for " + this);
        }
        return stopCalls.getFirstStop(true).getDepartureTime();
    }

    @Override
    public TramTime arrivalTime() {
        if (stopCalls.isEmpty()) {
            throw new RuntimeException("Cannot get stopCalls for " + this);
        }
        return stopCalls.getLastStop(true).getArrivalTime();
    }

    @Override
    public boolean hasStops() {
        boolean noStops = stopCalls.isEmpty();
        if (noStops) {
            if (filtered) {
                // this can happen when all stop calls for a trip have been filtered out by geo-bounds
                logger.warn(format("Filtered Trip %s has no stops", tripId));
            } else {
                // this ought not to happen
                logger.error(format("Unfiltered Trip %s has no stops",tripId));
            }
        }
        return !noStops;
    }

    @Override
    public boolean callsAt(final IdFor<Station> stationId) {
        return stopCalls.callsAt(stationId);
    }

    @Override
    public boolean callsAt(final InterchangeStation interchangeStation) {
        return stopCalls.callsAt(interchangeStation);
    }

    @Override
    public boolean serviceOperatesOn(final TramDate date) {
        return service.getCalendar().operatesOn(date);
    }

    @Override
    public IdFor<Station> firstStation() {
        return stopCalls.getFirstStop(true).getStationId();
    }

    /***
     * Test support only
     * @return id of last station on the trip
     */
    @Deprecated
    @Override
    public IdFor<Station> lastStation() {
        final StopCall lastStop = stopCalls.getLastStop(true);
        if (!lastStop.callsAtStation()) {
            // todo
            throw new RuntimeException("Undefined behaviour, last stop is not called at for " + lastStop);
        }
        return lastStop.getStationId();
    }

    /***
     * @param first first station id
     * @param second second start id
     * @return true iff second is after first for this trip
     */
    @Override
    public boolean isAfter(final IdFor<Station> first, final IdFor<Station> second) {
        final int seqFirst = stopCalls.getStopFor(first).getGetSequenceNumber();
        final int seqSecond = stopCalls.getStopFor(second).getGetSequenceNumber();
        return seqSecond > seqFirst;
    }

}
