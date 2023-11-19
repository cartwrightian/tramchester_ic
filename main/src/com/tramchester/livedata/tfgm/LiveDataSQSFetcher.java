package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SQSSubscriber;
import com.tramchester.cloud.SQSSubscriberFactory;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/***
 * Subscribes a queue to the SNS topic where live data is published
 * Need a queue per environment, with limited lifetime so old messages are discarded, which depends on
 * configured poll rate for live data
 */

@LazySingleton
public class LiveDataSQSFetcher extends LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataSQSFetcher.class);
    public static final String LIVEDATA_QUEUE_PREFIX = "tramchester_live_data_";

    private final SQSSubscriberFactory sqsSubscriberFactory;
    private final TramchesterConfig config;
    private SQSSubscriber sqsSubscriber;

    @Inject
    public LiveDataSQSFetcher(SQSSubscriberFactory sqsSubscriberFactory, TramchesterConfig config) {
        this.sqsSubscriberFactory = sqsSubscriberFactory;
        this.config = config;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        String queueName = LIVEDATA_QUEUE_PREFIX + config.getEnvironmentName();
        String topicName = config.getLiveDataSNSTopic();
        long retentionPeriodSeconds = 60; // shortest period allowed, which might produce stale data,
        // but should never be more than 2 or 3 messages
        this.sqsSubscriber = sqsSubscriberFactory.getFor(queueName, topicName, retentionPeriodSeconds);
    }

    @Override
    String getData() {
        String text =  sqsSubscriber.receiveMessage();
        logger.info("Received message of size " + text.length());
        return text;
    }
}
