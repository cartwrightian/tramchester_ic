package com.tramchester.unit.domain.presentation;

import com.tramchester.domain.presentation.TravelAction;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.presentation.TravelAction.*;
import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TravelActionTest {

    @Test
    void shouldBoard() {
        assertEquals("Board Tram", TravelAction.decideActionText(Board, Tram, None));
        assertEquals("Board Train", TravelAction.decideActionText(Board, Train, None));
    }

    @Test
    void shouldChange() {
        assertEquals("Change Tram", TravelAction.decideActionText(Change, Tram, None));
        assertEquals("Change Train", TravelAction.decideActionText(Change, Train, None));

        assertEquals("Board Tram", TravelAction.decideActionText(Change, Tram, ConnectTo));
        assertEquals("Board Tram", TravelAction.decideActionText(Change, Tram, WalkTo));


    }
}
