package com.tramchester.graph.search;

import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.TimedTransaction;
import com.tramchester.graph.facade.GraphNode;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.GraphTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class FindStationLinks {
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberLinks.class);

    private final GraphDatabase graphDatabase;
    private final StationRepository stationRepository;
    private final Geography geography;

    @Inject
    public FindStationLinks(GraphDatabase graphDatabase,
                            @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready readyToken,
                            StationRepository stationRepository, Geography geography) {
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
        this.geography = geography;
    }

    // supports visualisation of the transport network
    public Set<StationLink> findLinkedFor(TransportMode mode) {
        logger.info(format("Find links for %s", mode));
        Map<String, Object> params = new HashMap<>();
        String stationLabel = GraphLabel.forMode(mode).name();
        String modesProps = GraphPropertyKey.TRANSPORT_MODES.getText();

        params.put("mode", mode.getNumber());

        String query = format("MATCH (a:%s)-[r:LINKED]->(b) " +
                        "WHERE $mode in r.%s " +
                        "RETURN r",
                stationLabel, modesProps);

        logger.info("Query: '" + query + '"');

        Set<StationLink> links = new HashSet<>();
        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "query for links " + mode)) {
            GraphTransaction txn = timedTransaction.transaction();
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                GraphRelationship relationship = txn.getQueryColumnAsRelationship(result.next(), "r");
                links.add(createLink(txn, relationship));
            }
            result.close();
        }

        logger.info("Found " + links.size() + " links");
        return links;
    }

    private StationLink createLink(GraphTransaction txn, GraphRelationship relationship) {

        GraphNode startNode = relationship.getStartNode(txn);
        GraphNode endNode = relationship.getEndNode(txn);

        IdFor<Station> startId = startNode.getStationId();
        IdFor<Station> endId = endNode.getStationId();

        Station start = stationRepository.getStationById(startId);
        Station end = stationRepository.getStationById(endId);

        Set<TransportMode> modes = relationship.getTransportModes();

        return StationLink.create(start, end, modes, geography);
    }

}
