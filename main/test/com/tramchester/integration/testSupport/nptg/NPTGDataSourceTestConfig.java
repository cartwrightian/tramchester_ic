package com.tramchester.integration.testSupport.nptg;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.nptg.NPTGDataLoader;

import java.nio.file.Path;
import java.time.Duration;


 //name: nptg
 // dataURL: https://beta-naptan.dft.gov.uk/Download/National/csv

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
        return "https://beta-naptan.dft.gov.uk/Download/National/csv";
        //return "https://beta-naptan.dft.gov.uk/Download/File/Localities.csv";
    }

    @Override
    public Duration getDefaultExpiry() {
        return Duration.ofDays(14);
    }

    @Override
    public String getDownloadFilename() {
        return NPTGDataLoader.LOCALITIES_CSV;
        //return "nptgcsv.zip";
    }

    @Override
    public String getName() {
        return "nptg";
    }

    @Override
    public boolean getIsS3() {
        return false;
    }

    @Override
    public String getModTimeCheckFilename() {
        return "";
    }
}
