package com.tramchester.testSupport.tfgm;

import com.tramchester.config.ConfigReference;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestEnv;

import java.nio.file.Path;
import java.time.Duration;

public class TFGMRemoteDataSourceConfig extends RemoteDataSourceConfig {
    private final Path downloadPath;

    public static TFGMRemoteDataSourceConfig createFor(Path downloadPath) {
        return new TFGMRemoteDataSourceConfig(downloadPath);
    }

    @Override
    public ConfigReference<Boolean> getSkip() {
        return new ConfigReference<>(false);
    }

    private TFGMRemoteDataSourceConfig(Path downloadPath) {
        this.downloadPath = downloadPath;
    }

    @Override
    public Path getDataPath() {
        return downloadPath.resolve("TfGMgtfs");
    }

    @Override
    public Path getDownloadPath() {
        return downloadPath;
    }

    @Override
    public String getDataCheckUrl() {
        return TestEnv.TFGM_TIMETABLE_URL;
    }

    @Override
    public String getDataUrl() {
        return TestEnv.TFGM_TIMETABLE_URL;
    }

    @Override
    public boolean checkOnlyIfExpired() {
        return false;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(1);
    }

    @Override
    public String getDownloadFilename() {
        return "tfgm_data.zip";
    }

    @Override
    public String getName() {
        return "tfgm";
    }

    @Override
    public DataSourceID getDataSourceId() {
        return DataSourceID.tfgm;
    }

    @Override
    public String getModTimeCheckFilename() {
        return "";
    }
}
