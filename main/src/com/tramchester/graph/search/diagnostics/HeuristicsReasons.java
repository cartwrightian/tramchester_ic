package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.ValidHeuristicReason;

public class HeuristicsReasons {

    private static class SameTrip extends HeuristicsReasonWithID<Trip> {
        private SameTrip(final IdFor<Trip> tripId, final HowIGotHere path) {
            super(ReasonCode.SameTrip, path, tripId);
        }
    }

    private static class RouteNotAvailableOnQueryDate extends HeuristicsReasonWithID<Route> {
        protected RouteNotAvailableOnQueryDate(final HowIGotHere path, final IdFor<Route> routeId) {
            super(ReasonCode.RouteNotOnQueryDate, path, routeId);
        }
    }

    private static class DoesNotRunOnQueryDate extends HeuristicsReasonWithID<Service> {
        protected DoesNotRunOnQueryDate(final HowIGotHere path, final IdFor<Service> nodeServiceId) {
            super(ReasonCode.NotOnQueryDate, path, nodeServiceId);
        }
    }

    public static HeuristicsReason AlreadySeenTime(HowIGotHere howIGotHere, GraphNodeId nextNodeId) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.AlreadySeenTime, howIGotHere, nextNodeId);
    }

    private static class StationClosed extends HeuristicsReasonWithID<Station> {
        protected StationClosed(final HowIGotHere howIGotHere, final IdFor<Station> closed) {
            super(ReasonCode.StationClosed, howIGotHere, closed);
        }
    }

    private static class ServiceDoesNotOperateOnTime<T> extends InvalidHeutisticReasonWithAttribute<T> {

        private final IdFor<Service> serviceId;

        protected ServiceDoesNotOperateOnTime(final ReasonCode reasonCode, final T elapsedTime, final HowIGotHere path, final IdFor<Service> serviceId) {
            super(reasonCode, path, elapsedTime);
            this.serviceId = serviceId;
        }

        @Override
        public String textForGraph() {
            return super.textForGraph() + " " + serviceId;
        }
    }

    ///////////////////////////////////
    /// convenience methods


    public static HeuristicsReason DoesNotRunOnQueryDate(final HowIGotHere path, final IdFor<Service> nodeServiceId) {
        return new DoesNotRunOnQueryDate(path, nodeServiceId);
    }

    public static HeuristicsReason ServiceNotRunningAtTime(final HowIGotHere path, final IdFor<Service> serviceId, final TramTime time) {
        return new ServiceDoesNotOperateOnTime<>(ReasonCode.ServiceNotRunningAtTime, time, path, serviceId);
    }

    public static HeuristicsReason StationNotReachable(final HowIGotHere path, final ReasonCode code) {
        return new InvalidHeuristicReason(code, path);
    }

    public static HeuristicsReason DoesNotOperateOnTime(final TramTime currentElapsed, final HowIGotHere path) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.DoesNotOperateOnTime, path, currentElapsed);
    }

    public static HeuristicsReason TooManyChanges(final HowIGotHere path, final int number) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.TooManyChanges, path, number);
    }

    public static HeuristicsReason TooManyWalkingConnections(final HowIGotHere path, final int count) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.TooManyWalkingConnections, path, count);
    }

    public static HeuristicsReason TooManyNeighbourConnections(final HowIGotHere path, final int count) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.TooManyNeighbourConnections, path, count);
    }

    public static HeuristicsReason TookTooLong(final TramTime currentElapsed, final HowIGotHere path) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.TookTooLong, path, currentElapsed);
    }

    public static HeuristicsReason DoesNotOperateAtHour(final TramTime currentElapsed, final HowIGotHere path) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.NotAtHour, path, currentElapsed);
    }

    public static HeuristicsReason AlreadyDeparted(final TramTime tramTime, final HowIGotHere path) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.AlreadyDeparted, path, tramTime);
    }

    public static HeuristicsReason DestinationUnavailableAtTime(final TramTime tramTime, final HowIGotHere path) {
        return new InvalidHeutisticReasonWithAttribute<>(ReasonCode.DestinationUnavailableAtTime, path, tramTime);
    }

    public static HeuristicsReason Cached(final HeuristicsReason contained, final HowIGotHere path) {
        return new CachedHeuristicReason(contained, path);
    }

    public static HeuristicsReason HigherCost(final HowIGotHere howIGotHere) {
        return new InvalidHeuristicReason(ReasonCode.HigherCost, howIGotHere);
    }

    public static HeuristicsReason PathToLong(final HowIGotHere path) {
        return new InvalidHeuristicReason(ReasonCode.PathTooLong, path);
    }

    public static HeuristicsReason ReturnedToStart(final HowIGotHere path) {
        return new InvalidHeuristicReason(ReasonCode.ReturnedToStart, path);
    }

    public static HeuristicsReason StationClosed(final HowIGotHere howIGotHere, IdFor<Station> closed) {
        return new StationClosed(howIGotHere, closed);
    }

    public static HeuristicsReason TransportModeWrong(final HowIGotHere howIGotHere) {
        return new InvalidHeuristicReason(ReasonCode.TransportModeWrong, howIGotHere);
    }

    public static HeuristicsReason RouteNotToday(final HowIGotHere howIGotHere, final IdFor<Route> id) {
        return new HeuristicsReasons.RouteNotAvailableOnQueryDate(howIGotHere, id);
    }

    public static HeuristicsReason CacheMiss(final HowIGotHere howIGotHere) {
        return new ValidHeuristicReason(ReasonCode.PreviousCacheMiss, howIGotHere);
    }

    public static HeuristicsReason SameTrip(final IdFor<Trip> tripId, final HowIGotHere howIGotHere) {
        return new SameTrip(tripId, howIGotHere);
    }

}
