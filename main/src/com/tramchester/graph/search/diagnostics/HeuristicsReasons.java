package com.tramchester.graph.search.diagnostics;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ValidHeuristicReason;

import static java.lang.String.format;

public class HeuristicsReasons {

    private static class SameTrip extends HeuristicsReasonWithID<Trip> {
        private SameTrip(final IdFor<Trip> tripId, final HowIGotHere path) {
            super(ReasonCode.SameTrip, path, tripId);
        }
    }

    private static class AlreadySeenStation extends HeuristicsReasonWithID<Station> {
        protected AlreadySeenStation(final IdFor<Station> stationId, final HowIGotHere path) {
            super(ReasonCode.AlreadySeenStation, path, stationId);
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

    private static class TooManyChanges extends HeuristicsReasonWithCount {
        protected TooManyChanges(final HowIGotHere path, final int number) {
            super(ReasonCode.TooManyChanges, path, number);
        }
    }

//    private static class TooManyWalkingConnections extends HeuristicsReasonWithCount {
//        protected TooManyWalkingConnections(final HowIGotHere path, final int count) {
//            super(ReasonCode.TooManyWalkingConnections, path, count);
//        }
//    }

//    private static class TooManyNeighbourConnections extends HeuristicsReasonWithCount {
//        protected TooManyNeighbourConnections(final HowIGotHere path, final int count) {
//            super(ReasonCode.TooManyNeighbourConnections, path, count);
//        }
//    }

    private static class StationClosed extends HeuristicsReasonWithID<Station> {
        protected StationClosed(final HowIGotHere howIGotHere, final IdFor<Station> closed) {
            super(ReasonCode.StationClosed, howIGotHere, closed);
        }
    }

    private static class ServiceDoesNotOperateOnTime extends DoesNotOperateOnTime {

        private final IdFor<Service> serviceId;

        protected ServiceDoesNotOperateOnTime(final ReasonCode reasonCode, final TramTime elapsedTime, final HowIGotHere path, final IdFor<Service> serviceId) {
            super(reasonCode, elapsedTime, path);
            this.serviceId = serviceId;
        }

        @Override
        public String textForGraph() {
            return format("ServiceDoesNotOperateOnTime:%s%s%s", serviceId, System.lineSeparator(), elapsedTime);
        }
    }

    ///////////////////////////////////
    /// convenience methods

    public static HeuristicsReason IsValid(final ReasonCode code, final HowIGotHere path) {
        return new ValidHeuristicReason( code, path);
    }

    public static HeuristicsReason Continue(final HowIGotHere path) {
        return new ValidHeuristicReason(ReasonCode.Continue, path);
    }

    public static HeuristicsReason DoesNotRunOnQueryDate(final HowIGotHere path, final IdFor<Service> nodeServiceId) {
        return new DoesNotRunOnQueryDate(path, nodeServiceId);
    }

    public static HeuristicsReason ServiceNotRunningAtTime(final HowIGotHere path, final IdFor<Service> serviceId, final TramTime time) {
        return new ServiceDoesNotOperateOnTime(ReasonCode.ServiceNotRunningAtTime, time, path, serviceId);
    }

    public static HeuristicsReason StationNotReachable(final HowIGotHere path, final ReasonCode code) {
        return new SimpleHeuristicReason(code, path);
    }

    public static HeuristicsReason DoesNotOperateOnTime(final TramTime currentElapsed, final HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.DoesNotOperateOnTime, currentElapsed, path);
    }

    public static HeuristicsReason TooManyChanges(final HowIGotHere path, final int number) {
        return new TooManyChanges(path, number);
    }

    public static HeuristicsReason TooManyWalkingConnections(final HowIGotHere path, final int count) {
        return new HeuristicsReasonWithCount(ReasonCode.TooManyWalkingConnections, path, count);
    }

    public static HeuristicsReason TooManyNeighbourConnections(final HowIGotHere path, final int count) {
        return new HeuristicsReasonWithCount(ReasonCode.TooManyNeighbourConnections, path, count);
    }

    public static HeuristicsReason TookTooLong(final TramTime currentElapsed, final HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.TookTooLong, currentElapsed, path);
    }

    public static HeuristicsReason DoesNotOperateAtHour(final TramTime currentElapsed, final HowIGotHere path) {
        return new DoesNotOperateAtHour(ReasonCode.NotAtHour, currentElapsed, path);
    }

    public static HeuristicsReason AlreadyDeparted(final TramTime currentElapsed, final HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.AlreadyDeparted, currentElapsed, path);
    }

    public static HeuristicsReason DestinationUnavailableAtTime(final TramTime currentElapsed, final HowIGotHere path) {
        return new DoesNotOperateOnTime(ReasonCode.DestinationUnavailableAtTime, currentElapsed, path);
    }

    public static HeuristicsReason Cached(final ReasonCode code, final TramTime currentElapsed, final HowIGotHere path) {

        return switch (code) {
            case NotAtHour -> new DoesNotOperateAtHour(ReasonCode.CachedNotAtHour, currentElapsed, path);
            case DoesNotOperateOnTime -> new DoesNotOperateOnTime(ReasonCode.CachedDoesNotOperateOnTime, currentElapsed, path);
            case TooManyRouteChangesRequired -> new DoesNotOperateOnTime(ReasonCode.CachedTooManyRouteChangesRequired, currentElapsed, path);
            case RouteNotOnQueryDate -> new DoesNotOperateOnTime(ReasonCode.CachedRouteNotOnQueryDate, currentElapsed, path);
            case NotOnQueryDate -> new DoesNotOperateOnTime(ReasonCode.CachedNotOnQueryDate, currentElapsed, path);
            case TooManyInterchangesRequired -> new DoesNotOperateOnTime(ReasonCode.CachedTooManyInterchangesRequired, currentElapsed, path);
            default -> new DoesNotOperateOnTime(ReasonCode.CachedUNKNOWN, currentElapsed, path);
        };
    }

    public static HeuristicsReason HigherCost(final HowIGotHere howIGotHere) {
        return new SimpleHeuristicReason(ReasonCode.HigherCost, howIGotHere);
    }

    public static HeuristicsReason PathToLong(final HowIGotHere path) {
        return new SimpleHeuristicReason(ReasonCode.PathTooLong, path);
    }

    public static HeuristicsReason ReturnedToStart(final HowIGotHere path) {
        return new SimpleHeuristicReason(ReasonCode.ReturnedToStart, path);
    }

    public static HeuristicsReason StationClosed(final HowIGotHere howIGotHere, IdFor<Station> closed) {
        return new StationClosed(howIGotHere, closed);
    }

//    public static HeuristicsReason TimedOut(final HowIGotHere howIGotHere) {
//        return new TimedOut(howIGotHere);
//    }


    public static HeuristicsReason TransportModeWrong(final HowIGotHere howIGotHere) {
        return new SimpleHeuristicReason(ReasonCode.TransportModeWrong, howIGotHere);
    }

    public static HeuristicsReason RouteNotToday(final HowIGotHere howIGotHere, final IdFor<Route> id) {
        return new HeuristicsReasons.RouteNotAvailableOnQueryDate(howIGotHere, id);
    }

    public static HeuristicsReason CacheMiss(final HowIGotHere howIGotHere) {
        return new SimpleHeuristicReason(ReasonCode.PreviousCacheMiss, howIGotHere);
    }

    public static HeuristicsReason AlreadySeenStation(final IdFor<Station> stationId, final HowIGotHere howIGotHere) {
        return new AlreadySeenStation(stationId, howIGotHere);
    }

    public static HeuristicsReason SameTrip(final IdFor<Trip> tripId, final HowIGotHere howIGotHere) {
        return new SameTrip(tripId, howIGotHere);
    }

}
