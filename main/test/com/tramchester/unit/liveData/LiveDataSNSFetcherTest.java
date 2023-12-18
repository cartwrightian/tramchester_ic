package com.tramchester.unit.liveData;

import com.tramchester.cloud.SQSSubscriber;
import com.tramchester.cloud.SQSSubscriberFactory;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataSQSFetcher;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LiveDataSNSFetcherTest extends EasyMockSupport {

    public static final String TEST_TOPIC = "testTopic";
    private LiveDataSQSFetcher fetcher;
    private SQSSubscriberFactory sqsSubscriberFactory;
    private TramchesterConfig config;
    private final Long refreshPeriod = 60L; // 60 is the min

    @BeforeEach
    void onceBeforeEachTest() {
        TfgmTramLiveDataConfig liveDataConfig = createMock(TfgmTramLiveDataConfig.class);
        config = TestEnv.GET(liveDataConfig);

        EasyMock.expect(liveDataConfig.getDataURI()).andStubReturn(URI.create("sns://"+TEST_TOPIC + config.getEnvironmentName()));
        EasyMock.expect(liveDataConfig.snsSource()).andStubReturn(true);

        sqsSubscriberFactory = createMock(SQSSubscriberFactory.class);

        fetcher = new LiveDataSQSFetcher(sqsSubscriberFactory, config);

        EasyMock.expect(liveDataConfig.getSnsTopicPublishPrefix()).andStubReturn(TEST_TOPIC);
        EasyMock.expect(liveDataConfig.getRefreshPeriodSeconds()).andStubReturn(refreshPeriod);
    }

    @Test
    void testShouldReceiveDataFromSQS() {

        SQSSubscriber sqsSubscriber = createMock(SQSSubscriber.class);

        String queueName = "tramchester_live_data_" + config.getEnvironmentName();
        EasyMock.expect(sqsSubscriberFactory.getFor(queueName, TEST_TOPIC + config.getEnvironmentName(),
                refreshPeriod)).andReturn(sqsSubscriber);

        String text = "someTextFromAMessage";
        EasyMock.expect(sqsSubscriber.receiveMessage()).andReturn(text);

        LiveDataFetcher.ReceivesRawData receiver = createMock(LiveDataFetcher.ReceivesRawData.class);

        receiver.rawData(text);
        EasyMock.expectLastCall();

        replayAll();
        fetcher.start();
        fetcher.subscribe(receiver);
        fetcher.fetch();
        verifyAll();
    }

    @Test
    void shouldGetTopicFromURI() {
        String nameA = fetcher.getTopicFrom(URI.create("sns://someTopic_with_name"));
        assertEquals("someTopic_with_name", nameA);

        String nameB = fetcher.getTopicFrom(URI.create("SNS://someTopic_with_other_name"));
        assertEquals("someTopic_with_other_name", nameB);
    }
}
