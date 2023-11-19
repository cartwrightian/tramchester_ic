package com.tramchester.livedata.tfgm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyLiveDataFetcher extends LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(DummyLiveDataFetcher.class);
    @Override
    String getData() {
        logger.error("getData() called for dummy implementation, check for live data being enabled in the caller!");
        return "";
    }
}
