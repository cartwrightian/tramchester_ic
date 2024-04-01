package com.tramchester.integration;

import com.netflix.governator.guice.Grapher;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

class CreatesCodeDependencyGraphTest {

    private static ComponentContainer container;

    @BeforeAll
    static void beforeAnyTestRun() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        container = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
    }

    @Disabled("Need to move to direct use of GraphvizGrapher as governator still has javax.inject dependencies")
    @Test
    void shouldCreateDotFile() throws Exception {
        Grapher grapher = container.get(Grapher.class);
        grapher.toFile(new File("dependencies.dot"));
    }
}
