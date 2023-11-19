package com.tramchester.integration.livedata;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.cloud.SNSPublisher;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataSNSPublisher;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class LiveDataSNSPublisherTest {
    LiveDataSNSPublisher publisher;
    SNSPublisher snsPublisher;
    private TramchesterConfig configuration;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        configuration = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
        GuiceContainerDependencies componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        publisher = componentContainer.get(LiveDataSNSPublisher.class);
    }

    @Test
    void todo() {
        fail("todo");
    }
}
