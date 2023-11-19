package com.tramchester.testSupport;

import com.tramchester.config.TfgmTramLiveDataConfig;

public class TestTramLiveDataConfig implements TfgmTramLiveDataConfig {

    @Override
    public int getMaxNumberStationsWithoutMessages() {
        return 10;
    }

    @Override
    public int getMaxNumberStationsWithoutData() {
        return 5;
    }

    @Override
    public String getS3Prefix() {
        return "test";
    }

    @Override
    public String getDataUrl() {
        return "https://api.tfgm.com/odata/Metrolinks";
    }

    @Override
    public String getDataSubscriptionKey() {
        return System.getenv("TFGMAPIKEY");
    }

    // upload disabled for now
    @Override
    public String getS3Bucket() { return ""; }

    @Override
    public String getSNSTopic() {
        return "";
    }

    @Override
    public Long getRefreshPeriodSeconds() { return 20L; }


}
