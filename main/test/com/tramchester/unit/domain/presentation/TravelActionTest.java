package com.tramchester.unit.domain.presentation;

import com.tramchester.domain.presentation.TravelAction;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.presentation.TravelAction.*;
import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TravelActionTest {

    @Test
    void shouldBoard() {
        assertEquals("Board Tram", TravelAction.decideActionText(Board, Tram, None, NotSet));
        assertEquals("Board Train", TravelAction.decideActionText(Board, Train, None, NotSet));
    }

    @Test
    void shouldChangeOrBoard() {
        assertEquals("Change Tram", TravelAction.decideActionText(Change, Tram, None, Tram));
        assertEquals("Change Train", TravelAction.decideActionText(Change, Train, None, Train));

        assertEquals("Board Tram", TravelAction.decideActionText(Change, Tram, ConnectTo, Walk));
        assertEquals("Board Tram", TravelAction.decideActionText(Change, Tram, WalkTo, Tram));
    }

    @Test
    void shouldBoardOrChangeAfterChangeDependingOnPreviousMode() {
        assertEquals("Board Tram", TravelAction.decideActionText(Change, Tram, Change, TramReplacementBus));

        assertEquals("Change Tram", TravelAction.decideActionText(Change, Tram, Change, Tram));

    }
}
