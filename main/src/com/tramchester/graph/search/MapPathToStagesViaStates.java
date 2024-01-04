package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.ConnectingStage;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.facade.*;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.states.NotStartedState;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TripRepository;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.tramchester.graph.GraphPropertyKey.STOP_SEQ_NUM;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_CHILD;
import static com.tramchester.graph.TransportRelationshipTypes.GROUPED_TO_PARENT;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class MapPathToStagesViaStates implements PathToStages {

    private static final Logger logger = LoggerFactory.getLogger(MapPathToStagesViaStates.class);

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final TraversalStateFactory stateFactory;
    private final StationGroupsRepository stationGroupsRepository;
    private final NodeContentsRepository nodeContentsRepository;
    private final TripRepository tripRepository;
    private final SortsPositions sortsPosition;

    @Inject
    public MapPathToStagesViaStates(StationRepository stationRepository, PlatformRepository platformRepository,
                                    TraversalStateFactory stateFactory, StationGroupsRepository stationGroupsRepository,
                                    NodeContentsRepository nodeContentsRepository,
                                    TripRepository tripRepository, SortsPositions sortsPosition) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.stateFactory = stateFactory;
        this.stationGroupsRepository = stationGroupsRepository;
        this.nodeContentsRepository = nodeContentsRepository;
        this.tripRepository = tripRepository;
        this.sortsPosition = sortsPosition;

    }

    @Override
    public List<TransportStage<?, ?>> mapDirect(RouteCalculator.TimedPath timedPath, JourneyRequest journeyRequest,
                                                LowestCostsForDestRoutes lowestCostForRoutes, LocationSet endStations,
                                                GraphTransaction txn) {
        final Path path = timedPath.path();
        final TramTime queryTime = timedPath.queryTime();
        logger.info(format("Mapping path length %s to transport stages for %s at %s with %s changes",
                path.length(), journeyRequest, queryTime, timedPath.numChanges()));

        final LatLong destinationLatLon = sortsPosition.midPointFrom(endStations);

        final TraversalOps traversalOps = new TraversalOps(txn, nodeContentsRepository, tripRepository, sortsPosition, endStations,
                destinationLatLon, lowestCostForRoutes, journeyRequest.getDate());

        final MapStatesToStages mapStatesToStages = new MapStatesToStages(stationRepository, platformRepository, tripRepository, queryTime);

        final TraversalState initial = new NotStartedState(traversalOps, stateFactory, journeyRequest.getRequestedModes());

        PathMapper pathMapper = new PathMapper(path, txn);

        pathMapper.process(initial, new PathMapper.ForGraphNode() {
            @Override
            public TraversalState getNextStateFrom(final TraversalState previous, final GraphNode node, final Duration currentCost) {
                final EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(node);
                final boolean alreadyOnDiversion = false;
                final TraversalState next = previous.nextState(labels, node, mapStatesToStages, currentCost, alreadyOnDiversion);

                logger.debug("At state " + previous.getClass().getSimpleName() + " next is " + next.getClass().getSimpleName());

                return next;
            }
        }, new PathMapper.ForGraphRelationship() {
            @Override
            public Duration getCostFor(final TraversalState current, final GraphRelationship relationship) {
                final Duration lastRelationshipCost = nodeContentsRepository.getCost(relationship);

                logger.debug("Seen " + relationship.getType().name() + " with cost " + lastRelationshipCost);

                if (Durations.greaterThan(lastRelationshipCost, Duration.ZERO)) {
                    Duration total = current.getTotalDuration().plus(lastRelationshipCost);
                    mapStatesToStages.updateTotalCost(total);
                }
                if (relationship.hasProperty(STOP_SEQ_NUM)) {
                    mapStatesToStages.passStop(relationship);
                }
                return lastRelationshipCost;
            }
        });

        TraversalState finalState = pathMapper.getFinalState();

        final ImmutableGraphNode startOfPath = txn.fromStart(path);
        final ImmutableGraphNode endOfPath = txn.fromEnd(path);

        finalState.toDestination(finalState, endOfPath, Duration.ZERO, mapStatesToStages);

        final List<TransportStage<?, ?>> stages = mapStatesToStages.getStages();
        if (stages.isEmpty()) {
            if (path.length()==2) {
                if (startOfPath.hasRelationship(OUTGOING, GROUPED_TO_PARENT) && (endOfPath.hasRelationship(INCOMING, GROUPED_TO_CHILD))) {
                    stages.addAll(addViaCompositeStation(startOfPath, endOfPath, journeyRequest));
                }
            } else {
                logger.warn("Did not map any stages for path length:" + path.length() + " path:" + timedPath + " request: " + journeyRequest);
            }
        }
        return stages;
    }

    private List<TransportStage<StationGroup, StationGroup>> addViaCompositeStation(GraphNode startNode, GraphNode endNode, JourneyRequest journeyRequest) {
        logger.info("Add ConnectingStage Journey via single composite node");

        final List<TransportStage<StationGroup, StationGroup>> toAdd = new ArrayList<>();

        IdFor<NPTGLocality> startId = startNode.getAreaId();
        IdFor<NPTGLocality> endId = endNode.getAreaId();

        StationGroup start = stationGroupsRepository.getStationGroup(startId);
        StationGroup end = stationGroupsRepository.getStationGroup(endId);

        ConnectingStage<StationGroup, StationGroup> connectingStage =
                new ConnectingStage<>(start, end, Duration.ZERO, journeyRequest.getOriginalTime());
        toAdd.add(connectingStage);

        return toAdd;
    }


}
