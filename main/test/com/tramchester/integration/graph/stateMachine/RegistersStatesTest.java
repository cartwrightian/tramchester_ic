package com.tramchester.integration.graph.stateMachine;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationCollection;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.search.stateMachine.RegistersStates;
import com.tramchester.graph.search.stateMachine.TowardsDestination;
import com.tramchester.graph.search.stateMachine.states.StateBuilderParameters;
import com.tramchester.graph.search.stateMachine.states.TraversalStateFactory;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

class RegistersStatesTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private TraversalStateFactory factory;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachOfTheTestsRun() {
        NodeContentsRepository nodeContents = componentContainer.get(NodeContentsRepository.class);
        LocationCollection destinationIds = new LocationSet<>();
        TowardsDestination towardsDestination = new TowardsDestination(destinationIds);
        factory = new TraversalStateFactory(new StateBuilderParameters(TestEnv.testDay(), TramTime.of(8,0),
                towardsDestination, nodeContents, config, EnumSet.of(TransportMode.Tram)));
    }

    @Test
    void shouldCreateStateTransitionDiagram() throws FileNotFoundException {

        Set<RegistersStates.FromTo> transitions = factory.getTransitions();

        Path filePath = Path.of("stateTransitions.dot");
        OutputStream fileStream = new FileOutputStream(filePath.toFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        printStream.print("digraph G {\n");
        transitions.forEach(transition -> printStream.format("\"%s\"->\"%s\";\n",
                transition.getFrom().name(), transition.getTo().name()));
        printStream.print("}");

        printStream.close();
    }

}