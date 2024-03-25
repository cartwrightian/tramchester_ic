package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TimedTransaction;
import com.tramchester.graph.facade.GraphNodeId;
import com.tramchester.graph.facade.MutableGraphNode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.caching.GraphBuilderCache;
import com.tramchester.graph.graphbuild.caching.StationAndPlatformNodeCache;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationGroupsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * Add nodes and relationships for group stations to the existing graph
 */
@LazySingleton
public class StationGroupsGraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(StationGroupsGraphBuilder.class);

    private final GraphDatabase graphDatabase;
    private final StationGroupsRepository stationGroupsRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;
    private final StationAndPlatformNodeCache stationAndPlatformNodeCache;
    private final Geography geography;
    private final Duration maxWalkingDuration;

    // NOTE: cannot use graphquery here as creates a circular dependency on this class

    @Inject
    public StationGroupsGraphBuilder(GraphDatabase graphDatabase, StationGroupsRepository stationGroupsRepository,
                                     TramchesterConfig config, GraphFilter graphFilter,
                                     @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready stationsAndLinksAreBuilt,
                                     GraphBuilderCache builderCache, Geography geography) {
        super(graphDatabase);
        this.graphDatabase = graphDatabase;
        this.stationGroupsRepository = stationGroupsRepository;
        this.config = config;
        this.graphFilter = graphFilter;
        this.stationAndPlatformNodeCache = builderCache;
        this.geography = geography;
        maxWalkingDuration = config.getWalkingDuration();
    }

    @PostConstruct
    private void start() {
        logger.info("starting");
        addCompositesStationsToDB();
        logger.info("started");
    }

    private void addCompositesStationsToDB() {
        if (!stationGroupsRepository.isEnabled()) {
            logger.warn("Disabled, StationGroupsRepository is not enabled");
            return;
        }
        try(MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            if (hasDBFlag(txn)) {
                logger.info("Already present in DB");
                return;
            }
        }
        config.getTransportModes().forEach(this::addGroupedStationsNodesAndLinks);
        try(MutableGraphTransaction txn = graphDatabase.beginTxMutable()) {
            addDBFlag(txn);
            txn.commit();
        }
        reportStats();
    }

    // force construction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    private void addGroupedStationsNodesAndLinks(final TransportMode mode) {
        final Set<StationGroup> groupsForMode = stationGroupsRepository.getStationGroupsFor(mode);

        if (groupsForMode.isEmpty()) {
            logger.info("No grouped stations to add for " + mode);
            return;
        }

        final String logMessage = "Adding " + groupsForMode.size() + " station groups for " + mode;

        final Map<IdFor<StationGroup>, GraphNodeId> nodeForGroups = new HashMap<>();

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, logMessage)) {
            final MutableGraphTransaction txn = timedTransaction.transaction();
            groupsForMode.stream().filter(graphFilter::shouldInclude).
                filter(this::shouldInclude).
                forEach(group -> {
                    final MutableGraphNode groupNode = createGroupedStationNodes(txn, group);
                    nodeForGroups.put(group.getId(), groupNode.getId());
                    linkStationsWithGroup(txn, groupNode, group);
            });
            timedTransaction.commit();
        }

        logger.info("Added " + nodeForGroups.size() + " station groups");

        final AtomicInteger parentChildLinks = new AtomicInteger(0);
        try(TimedTransaction addParentTxn = new TimedTransaction(graphDatabase, logger, "add parents for groups")) {
            final MutableGraphTransaction txn = addParentTxn.transaction();
            groupsForMode.stream().filter(graphFilter::shouldInclude).
                    filter(this::shouldInclude).
                    filter(StationGroup::hasParent).
                    filter(group -> nodeForGroups.containsKey(group.getParentId())).
                    forEach(group -> {
                        final GraphNodeId currentNodeId = nodeForGroups.get(group.getId());
                        final GraphNodeId parentNodeId = nodeForGroups.get(group.getParentId());
                        createGroupToParentRelationship(txn, txn.getNodeByIdMutable(currentNodeId), txn.getNodeByIdMutable(parentNodeId), group);
                        parentChildLinks.getAndIncrement();
                    });
            addParentTxn.commit();
        }

        logger.info("Added " + parentChildLinks.get() + " links between parent/child station groups");

        nodeForGroups.clear();

    }

    private void createGroupToParentRelationship(final MutableGraphTransaction txn, final MutableGraphNode childNode,
                                                 final MutableGraphNode parentNode, final StationGroup childGroup) {
        // parent group <-> child group
        final Location<?> parentGroup = stationGroupsRepository.getStationGroup(childGroup.getParentId());
        final Duration walkingCost = geography.getWalkingDuration(childGroup, parentGroup);

        if (walkingCost.compareTo(maxWalkingDuration)<=0) {
            // todo seems some group's parents are a long way off
            addRelationshipsBetweenGroupAndParentGroup(txn, childNode, parentNode, walkingCost);
        }
    }

    private boolean shouldInclude(StationGroup station) {
        return graphFilter.shouldIncludeRoutes(station.getPickupRoutes()) ||
                graphFilter.shouldIncludeRoutes(station.getDropoffRoutes());
    }

    private MutableGraphNode createGroupedStationNodes(final MutableGraphTransaction txn, final StationGroup stationGroup) {
        final MutableGraphNode groupNode = createGraphNode(txn, GraphLabel.GROUPED);
        final IdFor<NPTGLocality> areaId = stationGroup.getLocalityId();
        groupNode.setAreaId(areaId);
        groupNode.set(stationGroup);
        return groupNode;
    }

    private void linkStationsWithGroup(final MutableGraphTransaction txn, final MutableGraphNode groupNode, final StationGroup stationGroup) {
        final LocationSet<Station> contained = stationGroup.getAllContained();

        contained.stream().
                filter(graphFilter::shouldInclude).
                forEach(station -> {
                    final Duration walkingCost = geography.getWalkingDuration(stationGroup, station);

                    final MutableGraphNode stationNode = stationAndPlatformNodeCache.getStation(txn, station.getId());
                    if (stationNode==null) {
                        throw new RuntimeException("cannot find node for " + station);
                    }

                    // note: Relationship code finds station group id from End Node
                    addGroupRelationshipTowardsContained(txn, groupNode, stationNode, walkingCost);
                    addContainedRelationshipTowardsGroup(txn, stationNode, groupNode, walkingCost);
        });
    }

    private boolean hasDBFlag(MutableGraphTransaction txn) {
        return txn.hasAnyMatching(GraphLabel.COMPOSITES_ADDED);
    }

    private void addDBFlag(MutableGraphTransaction txn) {
        txn.createNode(GraphLabel.COMPOSITES_ADDED);
    }

    public static class Ready {
        private Ready() {
        }
    }

}
