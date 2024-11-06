package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SQSSubscriber;
import com.tramchester.cloud.SQSSubscriberFactory;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.net.URI;

import static java.lang.String.format;

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
        // queue per env, the queue is subscribed to the sns topic
        String queueName = LIVEDATA_QUEUE_PREFIX + config.getEnvironmentName();
        TfgmTramLiveDataConfig liveDataConfig = config.getLiveDataConfig();
        URI snsURI = liveDataConfig.getDataURI();

        if (liveDataConfig.snsSource()) {
            long retentionPeriodSeconds = 60; // shortest period allowed, which might produce stale data,
            // but should never be more than 2 or 3 messages
            final String snsTopic = getTopicFrom(snsURI);
            logger.info(format("Attempt to subscribe queue %s to sns topic %s from uri %s", queueName, snsTopic, snsURI));
            sqsSubscriber = sqsSubscriberFactory.getFor(queueName, snsTopic, retentionPeriodSeconds);
            if (sqsSubscriber == null) {
                logger.error("Failed to get subscriber");
            }
            logger.info("started");
        } else {
            logger.error("Did not see sns source for live data, data url was " + snsURI);
        }
    }

    public String getTopicFrom(final URI uri) {
        final String scheme = uri.getScheme();
        final String text = uri.toString();

        return text.replace(scheme,"").replace("://","");
    }

    // NOTE For testing - wire up for periodic fetching is in Main, it will not happen on it's own
    @Override
    String getData() {
        if (sqsSubscriber==null) {
            logger.warn("No subscriber present");
            return "";
        }

        final String text = sqsSubscriber.receiveMessage();
        logger.info("Received message of size " + text.length());
        return text;
    }

    @Override
    boolean isEnabled() {
        return sqsSubscriber!=null;
    }
}
