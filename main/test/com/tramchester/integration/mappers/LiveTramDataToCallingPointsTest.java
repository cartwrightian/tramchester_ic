package com.tramchester.integration.mappers;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.repository.LiveDataObserver;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.mappers.LiveTramDataToCallingPoints;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

@Disabled("SPIKE WIP")
public class LiveTramDataToCallingPointsTest {

    private static GuiceContainerDependencies componentContainer;
    private LiveTramDataToCallingPoints toCallingPoints;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        toCallingPoints = componentContainer.get(LiveTramDataToCallingPoints.class);
    }

    @Test
    void spikeLiveDataToAmbiguousStationPairs() {
        final LiveDataFetcher fetcher = componentContainer.get(LiveDataFetcher.class);
        final LiveDataMarshaller marshaller = componentContainer.get(LiveDataMarshaller.class);
        final CountDownLatch latch = new CountDownLatch(1);

        marshaller.addSubscriber(new LiveDataObserver() {
            @Override
            public boolean seenUpdate(List<TramStationDepartureInfo> update) {
                toCallingPoints.map(update);
                latch.countDown();
                return true;
            }
        });

        try {
            fetcher.fetch();
            latch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e);
        }

    }


}
