package com.tramchester.integration.livedata;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.livedata.tfgm.LiveDataHTTPFetcher;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.livedata.repository.StationByName;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.LiveDataMessagesCategory;
import com.tramchester.testSupport.testTags.LiveDataTestCategory;
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
    @LiveDataTestCategory
    void shouldFetchSomethingFromTFGM() {
        assertNotNull(payload);
        assertFalse(payload.isEmpty());
    }

    @Test
    @LiveDataMessagesCategory
    void shouldFetchValidDataFromTFGMAPI() {
        List<TramStationDepartureInfo> departureInfos = parser.parse(payload);

        assertFalse(departureInfos.isEmpty());

        Optional<TramStationDepartureInfo> hasMsgs = departureInfos.stream().
                filter(info -> !info.getMessage().isEmpty()).findAny();

        assertTrue(hasMsgs.isPresent(), "display with msgs");

        TramStationDepartureInfo display = hasMsgs.get();

        // this assert will fail if run at certain times of day....
        // assertTrue(aDisplay.getDueTrams().size()>0);
        assertNotEquals(Lines.UnknownLine, display.getLine());
        LocalDateTime when = display.getLastUpdate();
        Assertions.assertEquals(TestEnv.LocalNow().getDayOfMonth(),when.getDayOfMonth());
    }

    @Test
    @LiveDataTestCategory
    void shouldHaveCrosscheckOnLiveDateDestinations() {
        List<TramStationDepartureInfo> departureInfos = parser.parse(payload);

        assertFalse(departureInfos.isEmpty());

        Set<Station> destinations = departureInfos.stream().flatMap(entry -> entry.getDueTrams().stream()).
                map(UpcomingDeparture::getDestination).collect(Collectors.toSet());

        Set<String> stationNames = transportData.getStations().stream().map(Station::getName).collect(Collectors.toSet());

        Set<Station> mismatch = destinations.stream().filter(destination -> !stationNames.contains(destination.getName())).
                collect(Collectors.toSet());

        assertTrue(mismatch.isEmpty(), mismatch.toString());
    }

    @Test
    @LiveDataTestCategory
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
    @LiveDataTestCategory
    void shouldMapAllLinesCorrectly() {
        List<TramStationDepartureInfo> departureInfos = parser.parse(payload);

        Set<Lines> uniqueLines = departureInfos.stream().map(TramStationDepartureInfo::getLine).collect(Collectors.toSet());

        assertFalse(uniqueLines.contains(Lines.UnknownLine));

        assertEquals(8, uniqueLines.size());
    }

    @Test
    void shouldHaveRealStationNamesForMappings() {
        List<LiveDataParser.LiveDataNamesMapping> mappings = Arrays.asList(LiveDataParser.LiveDataNamesMapping.values());
        mappings.forEach(mapping -> assertTrue(stationByName.getTramStationByName(mapping.getToo()).isPresent(), mapping.name()));
    }


}
