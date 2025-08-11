package com.tramchester.graph.core.neo4j;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.core.GraphRelationship;
import com.tramchester.graph.reference.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class FindLinkedStationsNeo4J extends FindLinkedStations {
    private static final Logger logger = LoggerFactory.getLogger(FindLinkedStationsNeo4J.class);

    private final GraphDatabaseNeo4J graphDatabase;
//    private final StationRepository stationRepository;
//    private final Geography geography;

    @Inject
    public FindLinkedStationsNeo4J(GraphDatabaseNeo4J graphDatabase,
                                   @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready readyToken,
                                   StationRepository stationRepository, Geography geography) {
        super(stationRepository, geography);
        this.graphDatabase = graphDatabase;
    }

    // supports visualisation of the transport network
    @Override
    public Set<StationToStationConnection> findLinkedFor(final TransportMode mode) {
        logger.info(format("Find links for %s", mode));

        final String modesProps = GraphPropertyKey.TRANSPORT_MODES.getText();
        final String stationLabel = GraphLabel.forMode(mode).name();

        final Map<String, Object> params = new HashMap<>();
        params.put("mode", mode.getNumber());

        final String query = format("MATCH (a:%s)-[r:LINKED]->(b) " +
                        "WHERE $mode in r.%s " +
                        "RETURN r",
                stationLabel, modesProps);

        final Set<StationToStationConnection> links = doQueryWithMapping(query, params, new ProcessResult<StationToStationConnection>() {
            @Override
            public StationToStationConnection map(MutableGraphTransactionNeo4J txn, Map<String, Object> row) {
                final Relationship relationship = (Relationship) row.get("r");
                final GraphRelationship graphRelationship = txn.wrapRelationship(relationship);
                return createConnection(graphRelationship);
            }
        });

        logger.info("Found " + links.size() + " links");
        return links;
    }

    @Override
    public IdSet<Station> atLeastNLinkedStations(final TransportMode mode, final int threshhold) {
        logger.info(format("Find at least N outbound for %s N=%s", mode, threshhold));

        final String modeLabel = GraphLabel.forMode(mode).name();

        Map<String, Object> params = new HashMap<>();
        params.put("threshhold", threshhold);

        final String query = format("MATCH (a:STATION)-[link:LINKED]->(:STATION) " +
                        "WHERE a:%s " +
                        "WITH a, count(link) as numLinks " +
                        "WHERE numLinks>=$threshhold " +
                        "RETURN a.station_id as stationId",
                modeLabel);

        Set<IdFor<Station>> stationIds = doQueryWithMapping(query, params, new ProcessResult<>() {
            @Override
            public IdFor<Station> map(MutableGraphTransactionNeo4J txn, Map<String, Object> row) {
                final String text = (String) row.get("stationId");
                return Station.createId(text);
            }
        });
        logger.info("Found " + stationIds.size() + " matches");

        return new IdSet<>(stationIds);
    }

    private <R> Set<R> doQueryWithMapping(String query, Map<String, Object> params, ProcessResult<R> mapper) {
        Set<R> results = new HashSet<>();
        logger.info("Query: '" + query + '"');
        try (MutableGraphTransactionNeo4J txn = graphDatabase.beginTimedTxMutableNeo4J(logger, "doQuery") ) {
            final Result result = txn.execute(query, params);
            while (result.hasNext()) {
                final Map<String, Object> row = result.next();
                final R item = mapper.map(txn, row);
                results.add(item);
            }
            result.close();
        }
        return results;
    }



    private interface ProcessResult<R> {
        R map(MutableGraphTransactionNeo4J txn, Map<String, Object> row);
    }

}
