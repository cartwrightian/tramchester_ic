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
    //boolean getIsS3();

    // url to check mod time against to see if newer data available
    String getDataCheckUrl();
    // url where data is located
    String getDataUrl();

    boolean isMandatory();
}
