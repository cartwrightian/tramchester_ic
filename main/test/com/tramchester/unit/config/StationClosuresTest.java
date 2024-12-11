package com.tramchester.unit.config;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationClosuresTest {

    @Test
    void shouldNotHaveClosuresInThePast() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);

        List<GTFSSourceConfig> dataSources = testConfig.getGTFSDataSource();
        assertEquals(1, dataSources.size());

        GTFSSourceConfig tfgmDataSource = dataSources.getFirst();
        assertEquals(DataSourceID.tfgm, tfgmDataSource.getDataSourceId());

        List<StationClosures> closures = tfgmDataSource.getStationClosures();

        TramDate today = TramDate.from(TestEnv.LocalNow());

        List<StationClosures> outdated = closures.stream().filter(closure -> closure.getDateRange().getEndDate().isBefore(today))
                .toList();

        assertTrue(outdated.isEmpty(), outdated.toString());

    }
}
