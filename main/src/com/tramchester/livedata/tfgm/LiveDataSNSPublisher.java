package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SNSPublisherSubscriber;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import static java.lang.String.format;

@LazySingleton
public class LiveDataSNSPublisher implements LiveDataFetcher.ReceivesRawData {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataSNSPublisher.class);

    private final TramchesterConfig config;
    private final SNSPublisherSubscriber snsPublisher;
    private final LiveDataFetcher liveDataFetcher;
    private String snsTopic;
    private boolean lastSendOk;

    @Inject
    public LiveDataSNSPublisher(TramchesterConfig config, SNSPublisherSubscriber snsPublisher, LiveDataFetcher liveDataFetcher) {
        this.config = config;
        this.snsPublisher = snsPublisher;
        this.liveDataFetcher = liveDataFetcher;
    }

    @PostConstruct
    public void start() {
        if (isEnabled()) {
            snsTopic = config.getLiveDataSNSPublishTopic();
            logger.info("Enabled, subscribing to live data, will publish on topic " + snsTopic);
            liveDataFetcher.subscribe(this);
        } else {
            logger.info("Not enabled");
        }
    }

    public boolean isEnabled() {
        if (!config.liveTfgmTramDataEnabled()) {
            return false;
        }
        TfgmTramLiveDataConfig liveDataConfig = config.getLiveDataConfig();
        String topic = config.getLiveDataSNSPublishTopic();
        if (topic.isEmpty()) {
            return false;
        }
        return !liveDataConfig.snsSource(); // don't want a loop....
    }

    @Override
    public void rawData(String text) {
        if (!isEnabled()) {
            logger.error("Should not be called, disabled");
            return;
        }

        logger.debug("Publishing live data to SNS on topic " + snsTopic);
        lastSendOk = snsPublisher.send(snsTopic, text);
        if (!lastSendOk) {
            logger.error(format("Failed to publish live data on topic %s, check logs", snsTopic));
        }
    }

    public boolean getLastSentOk() {
        return lastSendOk;
    }
}
