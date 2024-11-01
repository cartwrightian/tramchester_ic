package com.tramchester.livedata.tfgm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// NOTE : see LiveDataModule for control of which instance gets created
public abstract class LiveDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataFetcher.class);

    private final List<ReceivesRawData> subscribers;

    protected LiveDataFetcher() {
        this.subscribers = new ArrayList<>();
    }

    public void fetch() {
        logger.debug("Fetch data");

        final String data = getData();
        if (data.isEmpty()) {
            logger.warn("No data received");
            return;
        }

        if (subscribers.isEmpty()) {
            logger.warn("No subscribers!");
        }
        subscribers.forEach(subscriber -> subscriber.rawData(data));
    }

    abstract String getData();
    abstract boolean isEnabled();

    protected void stop() {
        subscribers.clear();
    }

    public void subscribe(final ReceivesRawData receivesRawData) {
        if (isEnabled()) {
            logger.info("Register " + receivesRawData.getClass());
            subscribers.add(receivesRawData);
        } else {
            // todo runtime exception?
            logger.error("NOT ENABLED, ignoring subscription request");
        }
    }

    public interface ReceivesRawData {
        void rawData(String text);
    }
}
