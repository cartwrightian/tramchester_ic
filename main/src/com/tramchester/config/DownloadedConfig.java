package com.tramchester.config;

import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;
import java.time.Duration;

public interface DownloadedConfig {
    DataSourceID getDataSourceId();

    // default expiry when cannot check mod time via http(s)
    Duration getDefaultExpiry();

    Path getDownloadPath();
    String getDownloadFilename();
    String getModTimeCheckFilename();
    boolean hasModCheckFilename();

    // url to check mod time against to see if newer data available
    String getDataCheckUrl();
    // url where data is located
    String getDataUrl();

    boolean isMandatory();

    // only check for updated data if file has expired, used for open rail data so not repeatedly requesting auth
    // token to do the check when we know only updated weekly
    boolean checkOnlyIfExpired();
}
