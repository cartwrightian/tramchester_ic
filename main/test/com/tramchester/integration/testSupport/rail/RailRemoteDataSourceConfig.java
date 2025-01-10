package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;
import java.time.Duration;

public class RailRemoteDataSourceConfig extends RemoteDataSourceConfig {

    // https://www.railforums.co.uk/threads/data-atoc-org-portal-closing.278726/

    // no longer available
    // http://data.atoc.org/how-to
    // https://data.atoc.org/member-area

//    public static final String VERSION = "344";


//    private static final String CURRENT_PREFIX = PREFIX +VERSION;

    private static final String RAIL_LATEST_ZIP = "https://opendata.nationalrail.co.uk/api/staticfeeds/3.0/timetable";
            //String.format("s3://tramchesternewdist/railData/%s.zip", CURRENT_PREFIX);

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
        return RAIL_LATEST_ZIP;
    }

    @Override
    public String getDataUrl() {
        return RAIL_LATEST_ZIP;
    }

    @Override
    public boolean checkOnlyIfExpired() {
        return true;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(6);
    }

    @Override
    public String getDownloadFilename() {
        return "rail_data.zip";
    }

    @Override
    public String getName() {
        return "openRailData";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.openRailData;
    }

//    public String getFilePrefix(final String version) {
//        return PREFIX+version;
//    }

    @Override
    public String getModTimeCheckFilename() {
        return "";
    }
}
