package com.tramchester.integration.livedata;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.OverheadDisplayLines;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataHTTPFetcher;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.livedata.repository.StationByName;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.LiveDataMessagesTest;
import com.tramchester.testSupport.testTags.LiveDataInfraTest;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class LiveDataHTTPFetcherTest {

    private static ComponentContainer componentContainer;
    private static LiveDataHTTPFetcher fetcher;
    private static String payload;
    private static IntegrationTramTestConfig configuration;

    private TransportData transportData;
    private LiveDataParser parser;
    private StationByName stationByName;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        configuration = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // don't want to fetch every time
        fetcher = componentContainer.get(LiveDataHTTPFetcher.class);
        payload = fetcher.getData();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        parser = componentContainer.get(LiveDataParser.class);
        stationByName = componentContainer.get(StationByName.class);
    }

    @Test
    void shouldHaveTFGMKeyInConfig() {
        assertNotNull(configuration.getLiveDataConfig().getDataSubscriptionKey(), "missing tfgm live data key");
    }

    @Test
    @LiveDataInfraTest
    void shouldFetchSomethingFromTFGM() {
        assertNotNull(payload);
        assertFalse(payload.isEmpty());
    }

    @Test
    @LiveDataMessagesTest
    void shouldFetchValidDataFromTFGMAPI() {
        List<TramStationDepartureInfo> departureInfos = parser.parse(payload);

        assertFalse(departureInfos.isEmpty());

        Optional<TramStationDepartureInfo> hasMsgs = departureInfos.stream().
                filter(info -> !info.getMessage().isEmpty()).findAny();

        assertTrue(hasMsgs.isPresent(), "display with msgs");

        TramStationDepartureInfo display = hasMsgs.get();

        // this assert will fail if run at certain times of day....
        // assertTrue(aDisplay.getDueTrams().size()>0);
        assertNotEquals(OverheadDisplayLines.UnknownLine, display.getLine());
        LocalDateTime when = display.getLastUpdate();
        Assertions.assertEquals(TestEnv.LocalNow().getDayOfMonth(),when.getDayOfMonth());
    }

    @Test
    @LiveDataInfraTest
    void shouldHaveCrosscheckOnLiveDateDestinations() {
        List<TramStationDepartureInfo> departureInfos = parser.parse(payload);

        assertFalse(departureInfos.isEmpty());

        IdSet<Station> destinations = departureInfos.stream().flatMap(entry -> entry.getDueTrams().stream()).
                map(UpcomingDeparture::getDestinationId).collect(IdSet.idCollector());

        IdSet<Station> expected = transportData.getStations().stream().collect(IdSet.collector());

        IdSet<Station> mismatch = destinations.stream().filter(destinationId -> !expected.contains(destinationId)).
                collect(IdSet.idCollector());

        assertTrue(mismatch.isEmpty(), mismatch.toString());
    }

    @Test
    @LiveDataInfraTest
    @Disabled("Part of spike on character set encoding issue for live api")
    void checkCharacterEncodingOnResponse()  {
        String rawJSON = fetcher.getData();

        //JSONParser jsonParser = new JSONParser();
        JsonObject parsed = Jsoner.deserialize(rawJSON, new JsonObject());
        assertTrue(parsed.containsKey("value"));
        JsonArray infoList = (JsonArray) parsed.get("value");

        List<String> destinations = new ArrayList<>();
        for (Object item : infoList) {
            JsonObject jsonObject = (JsonObject) item;
            for (int i = 0; i < 4; i++) {
                String place = jsonObject.get(format("Dest%d", i)).toString();
                if (!place.isEmpty()) {
                    destinations.add(place);
                }
            }
        }
        assertFalse(destinations.isEmpty());
    }

    @Test
    @LiveDataInfraTest
    void shouldMapAllLinesCorrectly() {
        List<TramStationDepartureInfo> departureInfos = parser.parse(payload);

        Set<OverheadDisplayLines> uniqueLines = departureInfos.stream().map(TramStationDepartureInfo::getLine).collect(Collectors.toSet());

        assertFalse(uniqueLines.isEmpty());

        assertFalse(uniqueLines.contains(OverheadDisplayLines.UnknownLine));

        assertEquals(8, uniqueLines.size());
    }

    @Test
    void shouldHaveRealStationNamesForMappings() {
        List<LiveDataParser.LiveDataNamesMapping> mappings = Arrays.asList(LiveDataParser.LiveDataNamesMapping.values());
        mappings.forEach(mapping -> assertTrue(stationByName.getTramStationByName(mapping.getToo()).isPresent(), mapping.name()));
    }


}
