package com.tramchester.integration.testSupport.nptg;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.nptg.NPTGXMLDataLoader;

import java.nio.file.Path;
import java.time.Duration;


 // name: nptg
 // https://beta-naptan.dft.gov.uk/

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

    // https://beta-naptan.dft.gov.uk/Download/Nptg/xml
    @Override
    public String getDataUrl() {
        return "https://beta-naptan.dft.gov.uk/Download/Nptg/xml";
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
        return NPTGXMLDataLoader.LOCALITIES_XML;
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
