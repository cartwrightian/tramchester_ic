package com.tramchester.integration.testSupport.naptan;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;

import java.nio.file.Path;
import java.time.Duration;


public class NaptanRemoteDataSourceTestConfig extends RemoteDataSourceConfig {
    public static final String NAPTAN_BASE_URL = "https://naptan.api.dft.gov.uk/v1/access-nodes"; // ?dataFormat=csv

    private final Path dataPath;

    public NaptanRemoteDataSourceTestConfig(Path dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
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
        // ?dataFormat=csv
        //return String.format("%s?dataFormat=%s", NAPTAN_BASE_URL, format);
        return "https://beta-naptan.dft.gov.uk/Download/National/xml";
    }

    @Override
    public boolean checkOnlyIfExpired() {
        return false;
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(14);
    }

    @Override
    public String getDownloadFilename() {
        return "NaPTAN.xml";
    }

    @Override
    public String getName() {
        return DataSourceID.naptanxml.name();
    }

    @Override
    public String getModTimeCheckFilename() {
        return "";
    }
}
