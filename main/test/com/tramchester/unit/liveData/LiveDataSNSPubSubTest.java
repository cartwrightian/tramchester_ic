package com.tramchester.unit.liveData;

import com.tramchester.cloud.SNSPublisherSubscriber;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LiveDataSNSPubSubTest extends EasyMockSupport {
    private LiveDataSNSPublisher publisher;
    private SNSPublisherSubscriber snsPublisher;
    private TfgmTramLiveDataConfig liveConfig;
    private LiveDataFetcher liveDataFetcher;

    @BeforeEach
    void beforeEachTest() {
        liveConfig = createMock(TfgmTramLiveDataConfig.class);
        TramchesterConfig config = new LocalConfig(liveConfig);
        snsPublisher = createStrictMock(SNSPublisherSubscriber.class);
        liveDataFetcher = createStrictMock(LiveDataFetcher.class);

        publisher = new LiveDataSNSPublisher(config, snsPublisher, liveDataFetcher);
    }

    @Test
    void shouldSendSNSForReceivedIfEnable() {

        EasyMock.expect(liveConfig.getSnsTopicPublishPrefix()).andStubReturn("aTopic");
        EasyMock.expect(liveConfig.snsSource()).andStubReturn(false);

        liveDataFetcher.subscribe(publisher);
        EasyMock.expectLastCall();

        EasyMock.expect(snsPublisher.send("aTopicDev", "someTextToSend")).andReturn(true);

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();

        assertTrue(publisher.getLastSentOk());
    }

    @Test
    void shouldCaptureFailure() {

        EasyMock.expect(liveConfig.getSnsTopicPublishPrefix()).andStubReturn("aTopic");
        EasyMock.expect(liveConfig.snsSource()).andStubReturn(false);

        liveDataFetcher.subscribe(publisher);
        EasyMock.expectLastCall();

        EasyMock.expect(snsPublisher.send("aTopicDev", "someTextToSend")).andReturn(false);

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();

        assertFalse(publisher.getLastSentOk());
    }

    @Test
    void shouldNotSendSNSForReceivedIfDisabledNoTopic() {

        EasyMock.expect(liveConfig.getSnsTopicPublishPrefix()).andStubReturn("");

        replayAll();
        publisher.start();
        publisher.rawData("someTextToSend");
        verifyAll();
    }

    @Test
    void shouldNotSendSNSForReceivedIfTopicButSNSSource() {

        EasyMock.expect(liveConfig.getSnsTopicPublishPrefix()).andStubReturn("validTopic");
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


