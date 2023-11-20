package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SNSPublisher;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@LazySingleton
public class LiveDataSNSPublisher implements LiveDataFetcher.ReceivesRawData {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataSNSPublisher.class);

    private final TramchesterConfig config;
    private final SNSPublisher snsPublisher;
    private final LiveDataFetcher liveDataFetcher;
    private String snsTopic;

    @Inject
    public LiveDataSNSPublisher(TramchesterConfig config, SNSPublisher snsPublisher, LiveDataFetcher liveDataFetcher) {
        this.config = config;
        this.snsPublisher = snsPublisher;
        this.liveDataFetcher = liveDataFetcher;
    }

    @PostConstruct
    public void start() {
        if (isEnabled()) {
            snsTopic = config.getLiveDataSNSTopic();
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
        snsTopic = config.getLiveDataSNSTopic();
        if (snsTopic.isEmpty()) {
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
        logger.info("Publishing live data to SNS on topic " + snsTopic);
        snsPublisher.send(snsTopic, text);
    }

}
