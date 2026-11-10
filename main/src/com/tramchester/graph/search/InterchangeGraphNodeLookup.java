package com.tramchester.graph.search;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphNodeId;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.graph.reference.GraphLabels;
import com.tramchester.repository.InterchangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.reference.GraphLabel.INTERCHANGE;
import static com.tramchester.graph.reference.GraphLabel.INTERCHANGE_NOT_SAME_MODE;

public class InterchangeGraphNodeLookup implements InterchangeNodeCheck {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeGraphNodeLookup.class);

    private final InterchangeRepository interchangeRepository;
    private final ImmutableEnumSet<TransportMode> consolidatedModes;
    private final Set<GraphNodeId> allowedForModes;
    private final boolean allowAll;

    public InterchangeGraphNodeLookup(final GraphTransaction txn, final InterchangeRepository interchangeRepository,
                                      final ImmutableEnumSet<TransportMode> requestedModes) {
        this.consolidatedModes = TransportMode.consolidateReplacements(requestedModes);
        this.interchangeRepository = interchangeRepository;

        if (consolidatedModes.size() > 1) {
            logger.info("Multi mode request, no interchange restrictions " + this.consolidatedModes);
            this.allowedForModes = Collections.emptySet();
            this.allowAll = true;
        } else {
            logger.info("Single mode request, enforce single mode restrictions for " + this.consolidatedModes);
            this.allowedForModes = findFor(txn);
            this.allowAll = false;
        }
    }

    private Set<GraphNodeId> findFor(final GraphTransaction txn) {
        return txn.findNodes(INTERCHANGE_NOT_SAME_MODE).
                filter(this::checkForModes).
                map(GraphNode::getId).
                collect(Collectors.toSet());
    }

    private boolean checkForModes(final GraphNode node) {
        final IdFor<Station> stationId = node.getStationId();
        final InterchangeStation interchangeStation = interchangeRepository.getInterchangeById(stationId);

        return interchangeStation.isAvailableSameMode(consolidatedModes);
    }

    @Override
    public boolean checkInterchangeAvailableFor(final GraphNode node) {
        if (allowAll) {
            return true;
        }

        final GraphLabels labels = node.getLabels();

        if (!labels.contains(INTERCHANGE)) {
            String message = "Not an interchange " + node;
            logger.error(message);
            throw new RuntimeException(message);
        }


        if (labels.contains(INTERCHANGE_NOT_SAME_MODE)) {
            return allowedForModes.contains(node.getId());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Not a same mode restricted interchange " + node.getId());
        }

        return true;
    }
}
