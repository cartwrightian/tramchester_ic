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

        EasyMock.expect(liveConfig.getSnsTopicPrefix()).andStubReturn("aTopic");
        EasyMock.expect(liveConfig.snsSource()).andStubReturn(false);

        liveDataFetcher.subscribe(publisher);
        EasyMock.expectLastCall();

        snsPublisher.send("aTopicDev", "someTextToSend");
        EasyMock.expectLastCall();

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();
    }

    @Test
    void shouldNotSendSNSForReceivedIfDisabledNoTopic() {

        EasyMock.expect(liveConfig.getSnsTopicPrefix()).andStubReturn("");

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();
    }

    @Test
    void shouldNotSendSNSForReceivedIfTopicButSNSSource() {

        EasyMock.expect(liveConfig.getSnsTopicPrefix()).andStubReturn("validTopic");
        EasyMock.expect(liveConfig.snsSource()).andStubReturn(true);

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


