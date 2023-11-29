package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;
import java.time.Duration;

public class RailRemoteDataSourceConfig extends RemoteDataSourceConfig {

    // http://data.atoc.org/how-to
    // https://data.atoc.org/member-area

    public static final String VERSION = "928";

    private static final String CURRENT_PREFIX = "ttis"+VERSION;

    private static final String RAIL_LATEST_ZIP = String.format("s3://tramchesternewdist/railData/%s.zip", CURRENT_PREFIX);

    private final String dataPath;

    public RailRemoteDataSourceConfig(String dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return Path.of(dataPath);
    }

    @Override
    public Path getDownloadPath() {
        return getDataPath();
    }

    @Override
    public String getDataCheckUrl() {
        return "";
    }

    @Override
    public String getDataUrl() {
        return RAIL_LATEST_ZIP;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(1);
    }

    @Override
    public String getDownloadFilename() {
        return "rail_data.zip";
    }

    @Override
    public String getName() {
        return "rail";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.rail;
    }

    @Override
    public boolean getIsS3() {
        return true;
    }

    public String getFilePrefix() {
        return CURRENT_PREFIX.replace("ttis", "ttisf");
    }

    @Override
    public String getModTimeCheckFilename() {
        return "";
    }
}
