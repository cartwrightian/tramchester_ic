package com.tramchester.livedata.tfgm;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SNSPublisher;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@LazySingleton
public class LiveDataSNSPublisher implements LiveDataFetcher.ReceivesRawData {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataSNSPublisher.class);

    private final TramchesterConfig config;
    private final SNSPublisher snsPublisher;
    private final LiveDataFetcher liveDataFetcher;

    public LiveDataSNSPublisher(TramchesterConfig config, SNSPublisher snsPublisher, LiveDataFetcher liveDataFetcher) {
        this.config = config;
        this.snsPublisher = snsPublisher;
        this.liveDataFetcher = liveDataFetcher;
    }

    @PostConstruct
    public void start() {
        if (isEnabled()) {
            logger.info("Enabled, subscribing to live data");
            liveDataFetcher.subscribe(this);
        } else {
            logger.info("Not enabled");
        }
    }

    public boolean isEnabled() {
        if (config.liveTfgmTramDataEnabled()) {
            TfgmTramLiveDataConfig liveDataConfig = config.getLiveDataConfig();
            String topic = liveDataConfig.getSNSTopic();
            if (topic==null) {
                return false;
            }
            return !topic.isEmpty();
        }
        return false;
    }

    @Override
    public void rawData(String text) {
        if (!isEnabled()) {
            logger.error("Should not be called, disabled");
            return;
        }
        snsPublisher.send(text);
    }

}
