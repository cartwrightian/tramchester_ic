package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.facade.neo4j.ImmutableGraphNode;
import com.tramchester.graph.facade.neo4j.ImmutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {

    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TripRepository tripRepository;
    private final TramchesterConfig config;

    @Inject
    public MapPathToStagesViaStates(StationRepository stationRepository, PlatformRepository platformRepository,
                                    TripRepository tripRepository, TramchesterConfig config) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.tripRepository = tripRepository;

        this.config = config;
    }

    @Override
    public List<TransportStage<?, ?>> mapDirect(final RouteCalculator.TimedPath timedPath, final JourneyRequest journeyRequest,
                                                final TowardsDestination towardsDestination, final ImmutableGraphTransactionNeo4J txn, boolean fullLogging) {
        final GraphPath path = timedPath.path();
        final TramTime queryTime = timedPath.queryTime();
        if (fullLogging) {
            logger.info(format("Mapping path length %s to transport stages for %s at %s with %s changes",
                    path.length(), journeyRequest, queryTime, timedPath.numChanges()));
        }

        final StateBuilderParameters builderParameters = new StateBuilderParameters(journeyRequest.getDate(), timedPath.queryTime(),
                towardsDestination, config, journeyRequest.getRequestedModes());

        final TraversalStateFactory stateFactory = new TraversalStateFactory(builderParameters);

        final MapStatesToStages mapStatesToStages = new MapStatesToStages(stationRepository, platformRepository, tripRepository, queryTime);

        final ImmutableGraphNode startOfPath = path.getStartNode(txn); // txn.fromStart(path);

        final TraversalState initial = new NotStartedState(stateFactory, startOfPath.getId(), txn);

        final PathMapper pathMapper = new PathMapper(path, txn);

        pathMapper.process(initial, new PathMapper.ForGraphNode() {
            @Override
            public TraversalState getNextStateFrom(final TraversalState previous, final GraphNode node, final Duration currentCost) {
                final EnumSet<GraphLabel> labels = node.getLabels();
                final TraversalState next = previous.nextState(labels, node, mapStatesToStages, currentCost);

                logger.debug("At state " + previous.getClass().getSimpleName() + " next is " + next.getClass().getSimpleName());

                return next;
            }
        }, new PathMapper.ForGraphRelationship() {
            @Override
            public Duration getCostFor(final TraversalState current, final GraphRelationship relationship) {
                final Duration lastRelationshipCost = relationship.getCost(); //nodeContentsRepository.getCost(relationship);

                logger.debug("Seen " + relationship.getType().name() + " with cost " + lastRelationshipCost);

                if (Durations.greaterThan(lastRelationshipCost, Duration.ZERO)) {
                    final Duration total = current.getTotalDuration().plus(lastRelationshipCost);
                    mapStatesToStages.updateTotalCost(total);
                }
                if (relationship.hasProperty(STOP_SEQ_NUM)) {
                    mapStatesToStages.passStop(relationship);
                }
                if (relationship.isType(DIVERSION)) {
                    final IdFor<Station> stationId = relationship.getStartStationId();
                    mapStatesToStages.beginDiversion(stationId);
                }
                return lastRelationshipCost;
            }
        });

        final TraversalState finalState = pathMapper.getFinalState();

        final ImmutableGraphNode endOfPath = path.getEndNode(txn); // txn.fromEnd(path);

        finalState.toDestination(finalState, endOfPath, Duration.ZERO, mapStatesToStages);

        final List<TransportStage<?, ?>> stages = mapStatesToStages.getStages();
        if (stages.isEmpty()) {
            if (path.length()==2) {
                /// child -> parent, end <- parent => startOfPath is station, endOfPath is station
                if (startOfPath.hasRelationship(GraphDirection.Outgoing, GROUPED_TO_PARENT) &&
                        (endOfPath.hasRelationship(GraphDirection.Incoming, GROUPED_TO_CHILD))) {
                    stages.add(getDirectConnectionFor(startOfPath, endOfPath, journeyRequest));
                }
            } else {
                logger.warn("Did not map any stages for path length:" + path.length() + " path:" + timedPath + " request: " + journeyRequest);
            }
        }
        return stages;
    }

    private  ConnectingStage<Station, Station> getDirectConnectionFor(final GraphNode startNode, final GraphNode endNode,
                                                                                  final JourneyRequest journeyRequest) {
        final IdFor<Station> startId = startNode.getStationId();
        final IdFor<Station> endId = endNode.getStationId();

        final Station start = stationRepository.getStationById(startId);
        final Station end = stationRepository.getStationById(endId);

        // todo duration should be walking costs?
        return new ConnectingStage<>(start, end, Duration.ZERO, journeyRequest.getOriginalTime());
    }


}
