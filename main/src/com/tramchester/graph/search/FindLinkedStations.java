package com.tramchester.graph.search;

import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.facade.GraphRelationship;
import com.tramchester.graph.facade.neo4j.TimedTransaction;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class FindLinkedStations {
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberLinks.class);

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
        // TODO could be immutable
        try (TimedTransaction txn = graphDatabase.beginTimedTxMutable(logger, "query for links " + mode)) {
            final Result result = txn.execute(query, params);
            while (result.hasNext()) {
                final GraphRelationship relationship = txn.getQueryColumnAsRelationship(result.next(), "r");
                links.add(createLink(relationship));
            }
            //result.close();
        }

        logger.info("Found " + links.size() + " links");
        return links;
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
