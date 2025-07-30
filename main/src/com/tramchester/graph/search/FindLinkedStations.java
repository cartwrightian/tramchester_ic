package com.tramchester.graph.search;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class FindLinkedStations {
    private static final Logger logger = LoggerFactory.getLogger(FindLinkedStations.class);

    private final GraphDatabase graphDatabase;
    private final StationRepository stationRepository;
    private final Geography geography;

    @Inject
    public FindLinkedStations(GraphDatabase graphDatabase,
                              @SuppressWarnings("unused") StationsAndLinksGraphBuilder.Ready readyToken,
                              StationRepository stationRepository, Geography geography) {
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
        this.geography = geography;
    }

    // supports visualisation of the transport network
    public Set<StationToStationConnection> findLinkedFor(final TransportMode mode) {
        logger.info(format("Find links for %s", mode));

        String modesProps = GraphPropertyKey.TRANSPORT_MODES.getText();
        String stationLabel = GraphLabel.forMode(mode).name();

        Map<String, Object> params = new HashMap<>();
        params.put("mode", mode.getNumber());

        String query = format("MATCH (a:%s)-[r:LINKED]->(b) " +
                        "WHERE $mode in r.%s " +
                        "RETURN r",
                stationLabel, modesProps);

        logger.info("Query: '" + query + '"');

        Set<StationToStationConnection> links = new HashSet<>();
        try (MutableGraphTransaction txn = graphDatabase.beginTimedTxMutable(logger, "query for links " + mode)) {
            // TODO
            final MutableGraphTransactionNeo4J neo4J = (MutableGraphTransactionNeo4J) txn;
            final Result result = neo4J.execute(query, params);
            while (result.hasNext()) {
                final GraphRelationship relationship = neo4J.getQueryColumnAsRelationship(result.next(), "r");
                links.add(createLink(relationship));
            }
            // TODO why is this commented out...??
            //result.close();
        }

        logger.info("Found " + links.size() + " links");
        return links;
    }

    public IdSet<Station> atLeastNLinkedStations(final TransportMode mode, final int threshhold) {
        logger.info(format("Find at least N outbound for %s N=%s", mode, threshhold));
        Map<String, Object> params = new HashMap<>();

        final String modeLabel = GraphLabel.forMode(mode).name();

        params.put("threshhold", threshhold);

        final String query = format("MATCH (a:STATION)-[link:LINKED]->(:STATION) " +
                        "WHERE a:%s " +
                        "WITH a, count(link) as numLinks " +
                        "WHERE numLinks>=$threshhold " +
                        "RETURN a.station_id as stationId",
                modeLabel);

        return doQuery(mode, params, query);
    }

    @NotNull
    private IdSet<Station> doQuery(final TransportMode mode, Map<String, Object> params, final String query) {
        logger.info("Query: '" + query + '"');

        final IdSet<Station> stationIds = new IdSet<>();

        // TODO could be immutable
        try (MutableGraphTransaction txn = graphDatabase.beginTimedTxMutable(logger, "linked for " + mode) ) {
            // TODO
            final MutableGraphTransactionNeo4J neo4J = (MutableGraphTransactionNeo4J) txn;
            final Result result = neo4J.execute(query, params);
            while (result.hasNext()) {
                final Map<String, Object> row = result.next();
                final String text = (String) row.get("stationId");
                stationIds.add(Station.createId(text));
            }
            result.close();
        }

        logger.info("Found " + stationIds.size() + " matches");
        return stationIds;
    }

    private StationToStationConnection createLink(final GraphRelationship relationship) {

        final IdFor<Station> startId = relationship.getStartStationId();
        final IdFor<Station> endId = relationship.getEndStationId();

        final Station start = stationRepository.getStationById(startId);
        final Station end = stationRepository.getStationById(endId);

        final EnumSet<TransportMode> modes = relationship.getTransportModes();

        return StationToStationConnection.createForWalk(start, end, modes, StationToStationConnection.LinkType.Linked, geography);
    }

}
