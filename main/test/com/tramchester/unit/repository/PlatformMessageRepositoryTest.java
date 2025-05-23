package com.tramchester.unit.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.OverheadDisplayLines;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformMessageRepositoryTest  extends EasyMockSupport {

    private ProvidesNow providesNow;
    private PlatformMessageRepository repository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;
    private TramchesterConfig config;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        config = createMock(TramchesterConfig.class);

        LiveDataMarshaller updater = EasyMock.createMock(LiveDataMarshaller.class);

        repository = new PlatformMessageRepository(updater, providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()), config);

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        station = TramStations.Shudehill.fakeWithPlatform(1, TramDate.of(today));
        platform = TestEnv.findOnlyPlatform(station);
    }

    @Test
    void shouldUpdateMessageCacheAndFetch() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("yyy", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "some message", lastUpdate, platform);


        Station altrincham = Altrincham.fake();
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("yyy", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, altrincham,
                "some different message", lastUpdate, TestEnv.createPlatformFor(altrincham, "someOther"));
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(2, repository.numberOfEntries());

        TramDate date = TramDate.from(lastUpdate);
        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), date, updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());

        Optional<PlatformMessage> noMessage = repository.messagesFor(PlatformId.createId(station, "XXXX"), date, updateTime);
        assertTrue(noMessage.isEmpty());

        Optional<PlatformMessage> otherMessage = repository.messagesFor(PlatformId.createId(altrincham, "someOther"), date, updateTime);
        assertTrue(otherMessage.isPresent());
        assertEquals("some different message", otherMessage.get().getMessage());

        List<PlatformMessage> stationMessages = repository.messagesFor(station, date, updateTime);
        assertEquals(1, stationMessages.size());
        assertEquals("some message", stationMessages.get(0).getMessage());

//        final Platform platform = MutablePlatform.buildForTFGMTram("XXXX", "platform name", Ashton.getLatLong(), DataSourceID.unknown, IdFor.invalid());
        Station otherStation = TramStations.Ashton.fakeWithPlatform(1, date);

        List<PlatformMessage> noStationMsg = repository.messagesFor(otherStation, date, updateTime);
        assertTrue(noStationMsg.isEmpty());
    }

    @Test
    void shouldIgnoreAPIProvidedEmptyMessage() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "<no message>", lastUpdate, platform);
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(0, repository.numberOfEntries());
    }

    @Test
    void shouldIgnorEmptyMessage() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andReturn(true);
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "", lastUpdate, platform);
        infos.add(departureInfo);

        //EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(0, repository.numberOfEntries());
    }

    @Test
    void shouldGiveNoMessagesIfNoRefresh() {
        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate),
                TramTime.ofHourMins(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOutOfDateRefresh() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "some msg", lastUpdate, platform);
        infos.add(departureInfo);

//        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate().minusDays(1));
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate).minusDays(1));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate),
                TramTime.ofHourMins(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGiveNoMessagesIfOldRefresh() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();
        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo("yyy", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "some msg", lastUpdate.minusMinutes(30), platform);
        infos.add(departureInfo);

        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        // no refresh
        Optional<PlatformMessage> result = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate),
                TramTime.ofHourMins(lastUpdate.toLocalTime()));
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIgnoreDuplicateUpdatesForPlatforms() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("123", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "some message", lastUpdate, platform);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("456", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "some other message", lastUpdate, platform);

        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));

        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());
    }

    @Test
    void shouldIgnoreEmptyDuplicateUpdatesForPlatforms() {
        List<TramStationDepartureInfo> infos = new LinkedList<>();
        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(true);

        TramStationDepartureInfo departureInfoA = new TramStationDepartureInfo("123", OverheadDisplayLines.Eccles,
                LineDirection.Incoming, station, "some message", lastUpdate, platform);
        TramStationDepartureInfo departureInfoB = new TramStationDepartureInfo("456", OverheadDisplayLines.Airport,
                LineDirection.Incoming, station, "", lastUpdate, platform);
        infos.add(departureInfoA);
        infos.add(departureInfoB);

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
//        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        repository.updateCache(infos);
        verifyAll();

        assertEquals(1, repository.numberOfEntries());

        Optional<PlatformMessage> platformMessage = repository.messagesFor(platform.getId(), TramDate.from(lastUpdate), updateTime);
        assertTrue(platformMessage.isPresent());
        assertEquals("some message", platformMessage.get().getMessage());
    }

    @Test
    void noNothingIfLiveDataIsDisabled() {

        TramTime time = TramTime.of(14, 34);

        EasyMock.expect(config.liveTfgmTramDataEnabled()).andStubReturn(false);
        EasyMock.expect(providesNow.getTramDate()).andReturn(TramDate.from(lastUpdate));


        replayAll();
        TramDate date = providesNow.getTramDate();

        List<PlatformMessage> messageForStation = repository.messagesFor(station, date, time);
        Optional<PlatformMessage> messageForPlatform = repository.messagesFor(PlatformId.createId(station, "platformId"), date, time);
        Set<Station> stationsWithMessages = repository.getStationsWithMessages(LocalTime.now());
        int numberOfEntries = repository.numberOfEntries();
        int numberStationsWithEntries = repository.numberStationsWithMessages(LocalDateTime.now());
        verifyAll();

        assertTrue(messageForStation.isEmpty());
        assertTrue(messageForPlatform.isEmpty());
        assertTrue(stationsWithMessages.isEmpty());
        assertEquals(0, numberOfEntries);
        assertEquals(0, numberStationsWithEntries);
    }

}
