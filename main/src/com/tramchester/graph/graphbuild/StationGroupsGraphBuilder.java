package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TimedTransaction;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.MutableGraphNode;
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
import java.util.Set;

/***
 * Add nodes and relationships for composite stations to the existing graph
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
        config.getTransportModes().forEach(this::addCompositeNodesAndLinks);
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

    private void addCompositeNodesAndLinks(TransportMode mode) {
        Set<StationGroup> allComposite = stationGroupsRepository.getStationGroupsFor(mode);

        if (allComposite.isEmpty()) {
            logger.info("No composite stations to add for " + mode);
            return;
        }

        final String logMessage = "adding " + allComposite.size() + " composite stations for " + mode;

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, logMessage)) {
            MutableGraphTransaction txn = timedTransaction.transaction();
            allComposite.stream().filter(graphFilter::shouldInclude).
                filter(this::shouldInclude).
                forEach(compositeStation -> {
                    MutableGraphNode stationNode = createGroupedStationNodes(txn, compositeStation);
                    linkStations(txn, stationNode, compositeStation);
            });
            timedTransaction.commit();
        }
    }

    private boolean shouldInclude(StationGroup station) {
        return graphFilter.shouldIncludeRoutes(station.getPickupRoutes()) ||
                graphFilter.shouldIncludeRoutes(station.getDropoffRoutes());
    }

    private MutableGraphNode createGroupedStationNodes(MutableGraphTransaction txn, StationGroup stationGroup) {
        MutableGraphNode groupNode = createGraphNode(txn, GraphLabel.GROUPED);
        IdFor<NaptanArea> areaId = stationGroup.getAreaId();
        groupNode.setAreaId(areaId);
        groupNode.set(stationGroup);
        return groupNode;
    }

    private void linkStations(MutableGraphTransaction txn, MutableGraphNode parentNode, StationGroup stationGroup) {
        Set<Station> contained = stationGroup.getContained();

        contained.stream().
                filter(graphFilter::shouldInclude).
                forEach(station -> {
                    final Duration walkingCost = geography.getWalkingDuration(stationGroup, station);
                    MutableGraphNode childNode = stationAndPlatformNodeCache.getStation(txn, station.getId());
                    if (childNode==null) {
                        throw new RuntimeException("cannot find node for " + station);
                    }

                    addGroupRelationshipTowardsChild(txn, parentNode, childNode, walkingCost);
                    addGroupRelationshipTowardsParent(txn, childNode, parentNode, walkingCost);
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
