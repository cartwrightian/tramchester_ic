package com.tramchester.unit.graph.calculation;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.StationToStationConnection;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindLinkedStations;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.UnitTestOfGraphConfig;
import com.tramchester.testSupport.reference.TramTransportDataForTestFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphQueriesTests {

    private static ComponentContainer componentContainer;
    private static UnitTestOfGraphConfig config;
    private TramTransportDataForTestFactory.TramTransportDataForTest transportData;

    @BeforeAll
    static void onceBeforeAllTestRuns() throws IOException {
        config = new UnitTestOfGraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                overrideProvider(TramTransportDataForTestFactory.class).
                create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsRun() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = (TramTransportDataForTestFactory.TramTransportDataForTest) componentContainer.get(TransportData.class);
    }

    @Test
    void shouldHaveCorrectLinksBetweenStations() {
        FindLinkedStations findStationLinks = componentContainer.get(FindLinkedStations.class);

        Set<StationToStationConnection> links = findStationLinks.findLinkedFor(Tram);

        assertEquals(6, links.size());

        EnumSet<TransportMode> modes = EnumSet.of(Tram);
        assertTrue(matches(links, modes, transportData.getFirst(), transportData.getSecond()));
        assertTrue(matches(links, modes, transportData.getSecond(), transportData.getInterchange()));
        assertTrue(matches(links, modes, transportData.getInterchange(), transportData.getFourthStation()));
        assertTrue(matches(links, modes, transportData.getInterchange(), transportData.getFifthStation()));
        assertTrue(matches(links, modes, transportData.getInterchange(), transportData.getLast()));
        assertTrue(matches(links, modes, transportData.getFirstDupName(), transportData.getFirstDup2Name()));

    }

    private boolean matches(Set<StationToStationConnection> links, EnumSet<TransportMode> modes, Station begin, Station end) {
        return links.stream().
                filter(link -> link.getLinkType()== StationToStationConnection.LinkType.Linked).
                filter(link -> link.getLinkingModes().equals(modes)).
                filter(link -> link.getBegin().equals(begin)).
                anyMatch(link -> link.getEnd().equals(end));
    }


}
