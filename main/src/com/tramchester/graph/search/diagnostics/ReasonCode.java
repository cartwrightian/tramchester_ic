package com.tramchester.graph.search.diagnostics;

import org.neo4j.graphdb.traversal.Evaluation;

public enum ReasonCode {

    ServiceDateOk, ServiceTimeOk, NumChangesOK, TimeOk, HourOk, Reachable, ReachableNoCheck, DurationOk,
    WalkOk, StationOpen, Continue, NumConnectionsOk, NumWalkingConnectionsOk, NeighbourConnectionsOk,
    ReachableSameRoute, TransportModeOk,

    NotOnQueryDate,
    RouteNotOnQueryDate,
    DoesNotOperateOnTime,
    ExchangeNotReachable,
    ServiceNotRunningAtTime,
    TookTooLong,
    NotAtHour,
    AlreadyDeparted,
    DestinationUnavailableAtTime,
    PathTooLong,
    AlreadySeenRouteStation,
    AlreadySeenTime,
    TransportModeWrong,
    HigherCost,
    SameTrip,

    ArrivedMoreChanges,
    ArrivedLater,

    ReturnedToStart,
    TooManyChanges,
    TooManyWalkingConnections,
    TooManyNeighbourConnections,
    StationClosed,
//    TimedOut,
    TooManyRouteChangesRequired,
    TooManyInterchangesRequired,

    SearchStopped, // search was stopped for some reason i.e. time out

//    CachedUNKNOWN,
    CachedNotAtHour,
    CachedDoesNotOperateOnTime,
    CachedTooManyRouteChangesRequired,
    CachedRouteNotOnQueryDate,
    CachedNotOnQueryDate,
    CachedTooManyInterchangesRequired,
    PreviousCacheMiss,

    // stats for overall journey
    OnTram,
    OnBus,
    OnTrain,
    OnShip,
    OnSubway,
    OnWalk,
    NotOnVehicle,
    SeenGroup,

    Arrived;

    private static Evaluation decideEvaluationAction(ReasonCode code) {
        return switch (code) {
            case ServiceDateOk, ServiceTimeOk, NumChangesOK, NumConnectionsOk, TimeOk, HourOk, Reachable, ReachableNoCheck,
                    DurationOk, WalkOk, StationOpen, Continue, ReachableSameRoute, TransportModeOk
                    -> Evaluation.INCLUDE_AND_CONTINUE;
            case Arrived
                    -> Evaluation.INCLUDE_AND_PRUNE;
            case HigherCost, ReturnedToStart, PathTooLong, TooManyChanges, TooManyWalkingConnections,
                    TookTooLong, ServiceNotRunningAtTime, NotAtHour, DoesNotOperateOnTime, NotOnQueryDate,
                    AlreadyDeparted, StationClosed, TooManyNeighbourConnections, RouteNotOnQueryDate,
                    ExchangeNotReachable, TooManyRouteChangesRequired, TooManyInterchangesRequired, AlreadySeenRouteStation,
                    TransportModeWrong, SameTrip, DestinationUnavailableAtTime, AlreadySeenTime,
                    ArrivedMoreChanges, ArrivedLater, SearchStopped
                    -> Evaluation.EXCLUDE_AND_PRUNE;
            case OnTram, OnBus, OnTrain, NotOnVehicle, PreviousCacheMiss, NumWalkingConnectionsOk,
                    NeighbourConnectionsOk, OnShip, OnSubway, OnWalk, CachedNotAtHour,
                    CachedDoesNotOperateOnTime, CachedTooManyRouteChangesRequired, CachedRouteNotOnQueryDate,
                    CachedNotOnQueryDate, CachedTooManyInterchangesRequired, SeenGroup
                    -> throw new RuntimeException("Unexpected reason-code during evaluation: " + code.name());
        };
    }

    public Evaluation getEvaluationAction() {
        return decideEvaluationAction(this);
    }
}
