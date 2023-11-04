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
    public Path getDownloadPath() {
        return getDataPath();
    }

    @Override
    public String getDataCheckUrl() {
        return TestEnv.getDatabaseRemoteURL();
    }

    @Override
    public String getDataUrl() {
        return TestEnv.getDatabaseRemoteURL();
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(0);
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

    @Override
    public boolean isMandatory() {
        // will be recreated locally if missing
        return false;
    }

    @Override
    public boolean getSkipUpload() {
        // sep upload mechanism for DB files
        return false;
    }
}
