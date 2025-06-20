package com.tramchester.integration.livedata;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataHTTPFetcher;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.LiveDataDueTramsTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LiveDataDueTramsTest
public class LiveDataParserTest {

    private static GuiceContainerDependencies componentContainer;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {

        // NOTE: Actual load of live data is disabled here as right now just checking static mappings

        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldHaveRealStationNamesForDestinationMapping() {
        Set<String> validNames = stationRepository.getStations().stream().
                map(Location::getName).collect(Collectors.toSet());

        Set<LiveDataParser.LiveDataNamesMapping> missing = Arrays.stream(LiveDataParser.LiveDataNamesMapping.values()).
                filter(mapping -> !validNames.contains(mapping.getToo())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), missing.toString());
    }

    @Test
    void shouldParseCurrentData() {

        LocalDateTime now = TestEnv.LocalNow();
        LocalDate today = now.toLocalDate();
        LiveDataHTTPFetcher fetcher = componentContainer.get(LiveDataHTTPFetcher.class);
        LiveDataParser parser = componentContainer.get(LiveDataParser.class);

        String payload = fetcher.getData();

        List<TramStationDepartureInfo> results = parser.parse(payload);

        assertFalse(results.isEmpty());

        Set<TramStationDepartureInfo> allDue = results.stream().filter(TramStationDepartureInfo::hasDueTrams).collect(Collectors.toSet());

        assertFalse(allDue.isEmpty());

        @NotNull Set<TramStationDepartureInfo> dueToday = allDue.stream().
                filter(info -> info.getLastUpdate().toLocalDate().equals(today)).
                collect(Collectors.toSet());

        assertFalse(dueToday.isEmpty());

        TramTime nowTime = TramTime.ofHourMins(now.toLocalTime());

        List<LiveDataMarshaller.Timely> timely = dueToday.stream().
                map(info -> LiveDataMarshaller.isTimely(info, today, nowTime)).
                toList();

        assertTrue(timely.contains(LiveDataMarshaller.Timely.OnTime), "No timely live data in " + payload);

    }
}
