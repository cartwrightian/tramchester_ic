package com.tramchester.integration.testSupport;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.time.Duration;

public class DatabaseRemoteDataSourceConfig extends RemoteDataSourceConfig {
    private final Path datapath;

    public DatabaseRemoteDataSourceConfig(Path datapath) {
        this.datapath = datapath;
    }

    @Override
    public Path getDataPath() {
        return datapath;
    }

    @Override
    public String getDataCheckUrl() {
        return TestEnv.DATABASE_REMOTE_URL;
    }

    @Override
    public String getDataUrl() {
        return TestEnv.DATABASE_REMOTE_URL;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(1);
    }

    @Override
    public String getDownloadFilename() {
        return "database.zip";
    }

    @Override
    public String getName() {
        return DataSourceID.database.name();
    }

    @Override
    public boolean getIsS3() {
        return true;
    }

    @Override
    public String getModTimeCheckFilename() {
        return "tramchester.db";
    }
}