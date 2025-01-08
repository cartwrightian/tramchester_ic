package com.tramchester.graph;

import com.google.inject.Inject;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.graphbuild.StationGroupsGraphBuilder;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@LazySingleton
public class FindStationsByNumberLinks {
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberLinks.class);

    private final GraphDatabase graphDatabase;

    // NOTE: beware circular dependencies here, interchange discover depends on this which is in turn used during
    // graph building
    @Inject
    public FindStationsByNumberLinks(GraphDatabase graphDatabase,
                                     @SuppressWarnings("unused") StationGroupsGraphBuilder.Ready readyToken) {
        this.graphDatabase = graphDatabase;
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

        IdSet<Station> stationIds = new IdSet<>();

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "linked for " + mode) ) {
            MutableGraphTransaction txn = timedTransaction.transaction();
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String text = (String) row.get("stationId");
                stationIds.add(Station.createId(text));
            }
            result.close();
        }

        logger.info("Found " + stationIds.size() + " matches");
        return stationIds;
    }

}
