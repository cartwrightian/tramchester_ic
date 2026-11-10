package com.tramchester.graph.search.stateMachine;

import com.tramchester.domain.JourneyRequest;
import com.tramchester.graph.core.GraphNode;
import com.tramchester.graph.core.GraphTransaction;
import com.tramchester.repository.InterchangeRepository;

import static com.tramchester.graph.reference.GraphLabel.INTERCHANGE_NOT_SAME_MODE;

public class InterchangeGraphLookup {

    public InterchangeGraphLookup() {
        // TODO
    }

    public InterchangeGraphLookup(GraphTransaction txn, InterchangeRepository interchangeRepository, JourneyRequest journeyRequest) {

    }

    public boolean checkInterchangeAvailableFor(final GraphNode node) {
        if (node.getLabels().contains(INTERCHANGE_NOT_SAME_MODE)) {
            return validateModes(node);
        }
        return true;
    }

    private boolean validateModes(final GraphNode node) {
        // TODO WIP
        return true;
    }
}
