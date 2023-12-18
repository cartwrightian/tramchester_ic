package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TfgmTramLiveDataAppConfig implements TfgmTramLiveDataConfig {

    private final String dataUrl;
    private final String dataSubscriptionKey;
    private final String s3Bucket;
    private final Long refreshPeriodSeconds;
    private final Integer maxNumberStationsWithoutMessages;
    private final Integer maxNumberStationsWithoutData;
    private final String s3Prefix;
    private final String snsTopicPrefix;

    public TfgmTramLiveDataAppConfig(@JsonProperty(value = "dataUrl", required = true) String dataUrl,
                                     @JsonProperty(value = "dataSubscriptionKey", required = true) String dataSubscriptionKey,
                                     @JsonProperty(value = "s3Bucket", required = true) String s3Bucket,
                                     @JsonProperty(value = "refreshPeriodSeconds", required = true) Long refreshPeriodSeconds,
                                     @JsonProperty(value = "maxNumberStationsWithoutMessages", required = true) Integer maxNumberStationsWithoutMessages,
                                     @JsonProperty(value = "maxNumberStationsWithoutData", required = true) Integer maxNumberStationsWithoutData,
                                     @JsonProperty(value = "s3Prefix", required = true) String s3Prefix,
                                     @JsonProperty(value = "snsTopicPrefix", required = false) String snsTopicPrefix) {
        this.dataUrl = dataUrl;
        this.dataSubscriptionKey = dataSubscriptionKey;
        this.s3Bucket = s3Bucket;
        this.refreshPeriodSeconds = refreshPeriodSeconds;
        this.maxNumberStationsWithoutMessages = maxNumberStationsWithoutMessages;
        this.maxNumberStationsWithoutData = maxNumberStationsWithoutData;
        this.s3Prefix = s3Prefix;
        this.snsTopicPrefix = snsTopicPrefix;
    }

    @Override
    public String getDataUrl() {
        return dataUrl;
    }

    @Override
    public String getDataSubscriptionKey() { return dataSubscriptionKey; }

    @Override
    public String getS3Bucket() {
        return s3Bucket.toLowerCase();
    }

    @Override
    public String getSnsTopicPublishPrefix() {
        if (snsTopicPrefix ==null) {
            return "";
        }
        return snsTopicPrefix;
    }

    @Override
    public Long getRefreshPeriodSeconds() { return refreshPeriodSeconds; }

    @Override
    public int getMaxNumberStationsWithoutMessages() {
        return maxNumberStationsWithoutMessages;
    }

    @Override
    public int getMaxNumberStationsWithoutData() {
        return maxNumberStationsWithoutData;
    }

    @Override
    public String getS3Prefix() {
        return s3Prefix;
    }

}
