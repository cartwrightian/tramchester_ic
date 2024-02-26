package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.Towards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraversalStateFactory {
    private static final Logger logger = LoggerFactory.getLogger(TraversalStateFactory.class);

    private final RegistersStates registersStates;
    private final TramchesterConfig config;
    private final NodeContentsRepository nodeContents;
    private boolean running;

    public TraversalStateFactory(RegistersStates registersStates, NodeContentsRepository nodeContents, TramchesterConfig config) {
        this.registersStates = registersStates;
        this.nodeContents = nodeContents;
        this.config = config;
        running = false;
    }

    public void start() {
        logger.info("starting");
        final boolean interchangesOnly = config.getChangeAtInterchangeOnly();
        final boolean depthFirst = config.getDepthFirst();

        final FindStateAfterRouteStation findStateAfterRouteStation = new FindStateAfterRouteStation();

        //    NotStartedState not currently via a builder

        registersStates.addBuilder(new RouteStationStateOnTrip.Builder(interchangesOnly, nodeContents));
        registersStates.addBuilder(new RouteStationStateEndTrip.Builder(interchangesOnly));
        registersStates.addBuilder(new HourState.Builder(depthFirst));
        registersStates.addBuilder(new JustBoardedState.Builder(interchangesOnly));
        registersStates.addBuilder(new NoPlatformStationState.Builder(findStateAfterRouteStation));
        registersStates.addBuilder(new PlatformStationState.Builder());
        registersStates.addBuilder(new WalkingState.Builder());
        registersStates.addBuilder(new ServiceState.Builder(depthFirst));
        registersStates.addBuilder(new PlatformState.Builder(findStateAfterRouteStation));
        registersStates.addBuilder(new MinuteState.Builder(interchangesOnly, nodeContents));
        registersStates.addBuilder(new DestinationState.Builder());
        registersStates.addBuilder(new GroupedStationState.Builder());

        running = true;
        logger.info("started");
    }

    public void stop() {
        logger.info("stopping");
        running = false;
        registersStates.clear();
        logger.info("stopped");
    }


    private <S extends TraversalState, T extends Towards<S>> T  getFor(TraversalStateType from, TraversalStateType to) {
        if (!running) {
            // help to diagnose / pinpoint issues with timeout causing shutdown from integration tests
            throw new RuntimeException("Not running");
        }
        return registersStates.getBuilderFor(from,to);
    }

    public HourState.Builder getTowardsHour(final TraversalStateType from) {
        return getFor(from, TraversalStateType.HourState);
    }

    public PlatformStationState.Builder getTowardsStation(final TraversalStateType from) {
        return getFor(from, TraversalStateType.PlatformStationState);
    }

    public DestinationState.Builder getTowardsDestination(final TraversalStateType from) {
        return getFor(from, TraversalStateType.DestinationState);
    }

    public MinuteState.Builder getTowardsMinute(final TraversalStateType from) {
        return getFor(from, TraversalStateType.MinuteState);
    }

    public GroupedStationState.Builder getTowardsGroup(final TraversalStateType from) {
        return getFor(from, TraversalStateType.GroupedStationState);
    }

    public NoPlatformStationState.Builder getTowardsNoPlatformStation(final TraversalStateType from) {
        return getFor(from, TraversalStateType.NoPlatformStationState);
    }

    public ServiceState.Builder getTowardsService(final TraversalStateType from) {
        return getFor(from, TraversalStateType.ServiceState);
    }

    public PlatformState.Builder getTowardsPlatform(final TraversalStateType from) {
        return getFor(from, TraversalStateType.PlatformState);
    }

    public WalkingState.Builder getTowardsWalk(final TraversalStateType from) {
        return getFor(from, TraversalStateType.WalkingState);
    }

    public JustBoardedState.Builder getTowardsJustBoarded(final TraversalStateType from) {
        return getFor(from, TraversalStateType.JustBoardedState);
    }

    public RouteStationStateOnTrip.Builder getTowardsRouteStationOnTrip(final TraversalStateType from) {
        return getFor(from, TraversalStateType.RouteStationStateOnTrip);
    }

    public RouteStationStateEndTrip.Builder getTowardsRouteStationEndTrip(final TraversalStateType from) {
        return getFor(from, TraversalStateType.RouteStationStateEndTrip);
    }
}
