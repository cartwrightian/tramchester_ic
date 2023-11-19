package com.tramchester.modules;

import com.google.inject.AbstractModule;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.livedata.tfgm.DummyLiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataFetcher;
import com.tramchester.livedata.tfgm.LiveDataHTTPFetcher;
import com.tramchester.livedata.tfgm.LiveDataSQSFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveDataModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataModule.class);

    private final TramchesterConfig config;

    public LiveDataModule(TramchesterConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        TfgmTramLiveDataConfig liveConfig = config.getLiveDataConfig();
        if  (liveConfig==null) {
            logger.warn("tfgm live data disabled");
            bind(LiveDataFetcher.class).to(DummyLiveDataFetcher.class);
            return;
        }
        String url = liveConfig.getDataUrl();
        if (url.toLowerCase().startsWith("https://")) {
            bind(LiveDataFetcher.class).to(LiveDataHTTPFetcher.class);
        } else if (url.equalsIgnoreCase("sns://")) {
            bind(LiveDataFetcher.class).to(LiveDataSQSFetcher.class);
        } else {
            throw new RuntimeException("Unknown scheme for " + url);
        }

    }
}
