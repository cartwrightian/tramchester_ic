package com.tramchester.graph.search.stateMachine;

import java.io.Serial;

public class NextStateNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NextStateNotFoundException(RegistersStates.FromTo key) {
        super("No next state, attempted transition was " + key.toString());
    }
}
