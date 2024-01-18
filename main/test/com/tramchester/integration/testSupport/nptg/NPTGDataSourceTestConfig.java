package com.tramchester.integration.testSupport.nptg;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.nptg.NPTGDataLoader;

import java.nio.file.Path;
import java.time.Duration;


 // name: nptg
 // https://beta-naptan.dft.gov.uk/Download/Nptg

public class NPTGDataSourceTestConfig extends RemoteDataSourceConfig {
    @Override
    public Path getDataPath() {
        return Path.of("data", "nptg");
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
        return "https://beta-naptan.dft.gov.uk/Download/Nptg/csv";
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(14);
    }

    @Override
    public String getDownloadFilename() {
        return NPTGDataLoader.LOCALITIES_CSV;
    }

    @Override
    public String getName() {
        return "nptg";
    }

    @Override
    public String getModTimeCheckFilename() {
        return "";
    }
}
