package com.tramchester.unit.repository;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.TransportDataContainer;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.TestEnv.Modes.BusesOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportDataContainerTest {

    @Test
    void shouldHaveCorrectModTimeForTransportMode() {
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();
        ZonedDateTime baseTime = providesLocalNow.getZoneDateTimeUTC();

        ZonedDateTime plusOneHour = baseTime.plusHours(1);
        ZonedDateTime minusOneHour = baseTime.minusHours(1);

        TransportDataContainer container = new TransportDataContainer(providesLocalNow, "local");

        DataSourceInfo dataSourceA = new DataSourceInfo(DataSourceID.tfgm, "v1", plusOneHour, TramsOnly);
        DataSourceInfo dataSourceB = new DataSourceInfo(DataSourceID.unknown, "v1", minusOneHour, BusesOnly);
        DataSourceInfo dataSourceC = new DataSourceInfo(DataSourceID.openRailData, "v1", baseTime, TramsOnly);

        container.addDataSourceInfo(dataSourceA);
        container.addDataSourceInfo(dataSourceB);
        container.addDataSourceInfo(dataSourceC);

        assertEquals(plusOneHour, container.getNewestModTimeFor(Tram));
        assertEquals(minusOneHour, container.getNewestModTimeFor(Bus));

    }
}
