package com.tramchester.config;

import java.net.URI;

public interface TfgmTramLiveDataConfig {
    String getDataUrl();

    default URI getDataURI() {
        return URI.create(getDataUrl());
    }

    String getDataSubscriptionKey();

    Long getRefreshPeriodSeconds();

    // for alerting
    int getMaxNumberStationsWithoutMessages();

    int getMaxNumberStationsWithoutData();

    // for S3 data archiving
    String getS3Prefix();

    String getS3Bucket();

    default boolean isS3Enabled() {
        return !getS3Bucket().isEmpty();
    }


    /**
     * For sns publish, use the helper method on config to get full topic
     * @return JUST the topic prefix
     */
    String getSnsTopicPublishPrefix();

    default boolean httpsSource() {
        return getDataURI().getScheme().equals("https");
    }

    default boolean snsSource() {
        return getDataURI().getScheme().equals("sns");
    }
}
