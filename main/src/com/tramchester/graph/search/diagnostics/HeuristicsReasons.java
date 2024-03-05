package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.search.ValidHeuristicReason;

import java.time.Duration;

public class HeuristicsReasons {


    private static class SameTrip extends HeuristicsReasonWithID<Trip> {
        private SameTrip(final IdFor<Trip> tripId, final HowIGotHere path) {
            super(ReasonCode.SameTrip, path, tripId, false);
        }
    }

    private static class RouteNotAvailableOnQueryDate extends HeuristicsReasonWithID<Route> {
        protected RouteNotAvailableOnQueryDate(final HowIGotHere path, final IdFor<Route> routeId) {
            super(ReasonCode.RouteNotOnQueryDate, path, routeId, false);
        }
    }

    private static class DoesNotRunOnQueryDate extends HeuristicsReasonWithID<Service> {
        protected DoesNotRunOnQueryDate(final HowIGotHere path, final IdFor<Service> nodeServiceId) {
            super(ReasonCode.NotOnQueryDate, path, nodeServiceId, false);
        }
    }

    public static HeuristicsReason AlreadySeenTime(HowIGotHere howIGotHere, GraphNodeId nextNodeId) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.AlreadySeenTime, howIGotHere, nextNodeId, false, GraphNodeId::toString);
    }

    private static class StationClosed extends HeuristicsReasonWithID<Station> {
        protected StationClosed(final HowIGotHere howIGotHere, final IdFor<Station> closed) {
            super(ReasonCode.StationClosed, howIGotHere, closed, false);
        }
    }

    ///////////////////////////////////
    /// convenience methods


    public static HeuristicsReason DoesNotRunOnQueryDate(final HowIGotHere path, final IdFor<Service> nodeServiceId) {
        return new DoesNotRunOnQueryDate(path, nodeServiceId);
    }

    public static HeuristicsReason ServiceNotRunningAtTime(final HowIGotHere path, final IdFor<Service> serviceId, final TramTime time) {
        return new HeuristicReasonWithAttributes<>(ReasonCode.ServiceNotRunningAtTime, path, serviceId, time, false,
                Object::toString, TramTime::toPattern);
    }

    public static HeuristicsReason StationNotReachable(final HowIGotHere path, final ReasonCode code) {
        return new HeuristicReasonWithValidity(code, path, false);
    }

    public static HeuristicsReason DoesNotOperateOnTime(final TramTime currentElapsed, final HowIGotHere path) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.DoesNotOperateOnTime, path, currentElapsed, false, TramTime::toPattern);
    }

    public static HeuristicsReason TooManyChanges(final HowIGotHere path, final int number) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.TooManyChanges, path, number, false, Object::toString);
    }

    public static HeuristicsReason TooManyWalkingConnections(final HowIGotHere path, final int count) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.TooManyWalkingConnections, path, count, false, Object::toString);
    }

    public static HeuristicsReason TooManyNeighbourConnections(final HowIGotHere path, final int count) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.TooManyNeighbourConnections, path, count, false, Object::toString);
    }

    public static HeuristicsReason TookTooLong(final TramTime currentElapsed, final HowIGotHere path) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.TookTooLong, path, currentElapsed, false, TramTime::toPattern);
    }

    public static HeuristicsReason DoesNotOperateAtHour(final TramTime currentElapsed, final HowIGotHere path, int hourAtNode) {
        return new HeuristicReasonWithAttributes<>(ReasonCode.NotAtHour, path, currentElapsed, hourAtNode, false,
                TramTime::toPattern, Object::toString);
    }

    public static HeuristicsReason AlreadyDeparted(final TramTime tramTime, final HowIGotHere path) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.AlreadyDeparted, path, tramTime, false, TramTime::toPattern);
    }

    public static HeuristicsReason DestinationUnavailableAtTime(final TramTime tramTime, final HowIGotHere path) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.DestinationUnavailableAtTime, path, tramTime, false, TramTime::toPattern);
    }

    public static HeuristicsReason Cached(final HeuristicsReason contained, final HowIGotHere path) {
        return new CachedHeuristicReason(contained, path);
    }

    public static HeuristicsReason HigherCost(final HowIGotHere howIGotHere, Duration duration) {
        return new HeuristicReasonWithAttribute<>(ReasonCode.HigherCost, howIGotHere, duration, false, Duration::toString);
    }

    public static HeuristicsReason ArrivedMoreChanges(HowIGotHere howIGotHere, int numberChanges, Duration duration) {
        return new HeuristicReasonWithAttributes<>(ReasonCode.ArrivedMoreChanges, howIGotHere, duration, numberChanges, false,
                Duration::toString, Object::toString);
    }


    public static HeuristicsReason ArrivedLater(HowIGotHere howIGotHere, Duration duration, int numberChanges) {
        return new HeuristicReasonWithAttributes<>(ReasonCode.ArrivedLater, howIGotHere, duration, numberChanges, false,
                Duration::toString, Object::toString);
    }

    public static HeuristicsReason PathToLong(final HowIGotHere path) {
        return new HeuristicReasonWithValidity(ReasonCode.PathTooLong, path, false);
    }

    public static HeuristicsReason ReturnedToStart(final HowIGotHere path) {
        return new HeuristicReasonWithValidity(ReasonCode.ReturnedToStart, path, false);
    }

    public static HeuristicsReason StationClosed(final HowIGotHere howIGotHere, IdFor<Station> closed) {
        return new StationClosed(howIGotHere, closed);
    }

    public static HeuristicsReason TransportModeWrong(final HowIGotHere howIGotHere) {
        return new HeuristicReasonWithValidity(ReasonCode.TransportModeWrong, howIGotHere, false);
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
