package com.tramchester.unit.liveData;

import com.tramchester.cloud.SNSPublisher;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataSNSPublisher;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LiveDataSNSPublisherTest extends EasyMockSupport {
    private LiveDataSNSPublisher publisher;
    private SNSPublisher snsPublisher;
    private TfgmTramLiveDataConfig liveConfig;
    private LiveDataFetcher liveDataFetcher;

    @BeforeEach
    void beforeEachTest() {
        liveConfig = createMock(TfgmTramLiveDataConfig.class);
        TramchesterConfig config = new LocalConfig(liveConfig);
        snsPublisher = createStrictMock(SNSPublisher.class);
        liveDataFetcher = createStrictMock(LiveDataFetcher.class);
        publisher = new LiveDataSNSPublisher(config, snsPublisher, liveDataFetcher);
    }

    @Test
    void shouldSendSNSForReceivedIfEnable() {

        EasyMock.expect(liveConfig.getSNSTopic()).andStubReturn("aTopic");

        liveDataFetcher.subscribe(publisher);
        EasyMock.expectLastCall();

        snsPublisher.send("someTextToSend");
        EasyMock.expectLastCall();

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();
    }

    @Test
    void shouldNotSendSNSForReceivedIfDisabled() {

        EasyMock.expect(liveConfig.getSNSTopic()).andStubReturn("");

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();
    }

    private static class LocalConfig extends TestConfig {
        private final TfgmTramLiveDataConfig liveDataConfig;

        private LocalConfig(TfgmTramLiveDataConfig liveDataConfig) {
            this.liveDataConfig = liveDataConfig;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public TfgmTramLiveDataConfig getLiveDataConfig() {
            return liveDataConfig;
        }
    }
}


