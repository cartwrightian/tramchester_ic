package com.tramchester.unit.repository;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.TransportDataContainer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportDataContainerTest {

    @Test
    void shouldHaveCorrectModTimeForTransportMode() {
        ProvidesLocalNow providesLocalNow = new ProvidesLocalNow();
        LocalDateTime baseTime = providesLocalNow.getDateTime();

        LocalDateTime plusOneHour = baseTime.plusHours(1);
        LocalDateTime minusOneHour = baseTime.minusHours(1);

        TransportDataContainer container = new TransportDataContainer(providesLocalNow, "local");

        DataSourceInfo dataSourceA = new DataSourceInfo(DataSourceID.tfgm, "v1", plusOneHour, EnumSet.of(Tram));
        DataSourceInfo dataSourceB = new DataSourceInfo(DataSourceID.unknown, "v1", minusOneHour, EnumSet.of(Bus));
        DataSourceInfo dataSourceC = new DataSourceInfo(DataSourceID.rail, "v1", baseTime, EnumSet.of(Tram));

        container.addDataSourceInfo(dataSourceA);
        container.addDataSourceInfo(dataSourceB);
        container.addDataSourceInfo(dataSourceC);

        assertEquals(plusOneHour, container.getNewestModTimeFor(Tram));
        assertEquals(minusOneHour, container.getNewestModTimeFor(Bus));

    }
}
