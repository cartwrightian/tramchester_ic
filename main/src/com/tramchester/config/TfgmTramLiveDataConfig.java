package com.tramchester.config;

public interface TfgmTramLiveDataConfig {
    String getDataUrl();

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
    String getSnsTopicPrefix();
}
