package com.tramchester.unit.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.LiveDataMarshaller;
import com.tramchester.livedata.tfgm.OverheadDisplayLines;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TramDepartureRepositoryTest extends EasyMockSupport {

    private ProvidesNow providesNow;
    private TramDepartureRepository departureRepository;
    private LocalDateTime lastUpdate;
    private Station station;
    private Platform platform;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;
    private LocalDate date;
    private TramTime expectedDue;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = createMock(ProvidesNow.class);
        LiveDataMarshaller updater = EasyMock.createMock(LiveDataMarshaller.class);
        departureRepository = new TramDepartureRepository(updater, providesNow, new CacheMetrics(TestEnv.NoopRegisterMetrics()));

        LocalDate today = TestEnv.LocalNow().toLocalDate();
        date = today;
        lastUpdate = LocalDateTime.of(today, LocalTime.of(15,42));

        station = Shudehill.fakeWithPlatform(1, TramDate.of(date));
        platform = TestEnv.findOnlyPlatform(station);

        expectedDue = TramTime.ofHourMins(lastUpdate.toLocalTime().plusMinutes(42));
    }

    @Test
    void shouldCountStationsWithDueTrams() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        // first station, has due tram
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, Bury.fake(), "Due",
                expectedDue, "Single", agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId1", platform,
                "message 1", station, dueTram);

        // second station, has due tram
        Station secondStation = Altrincham.fakeWithPlatform(1, TramDate.of(date));
        Platform platfromForSecondStation = TestEnv.findOnlyPlatform(secondStation);

        UpcomingDeparture dueTramOther = new UpcomingDeparture(date, secondStation, ManAirport.fake(), "Due",
                TramTime.ofHourMins(lastUpdate.toLocalTime()).plus(Duration.ofMinutes(12)), "Double", agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId2", platfromForSecondStation,
                "message 2", secondStation, dueTramOther);

        // third, no due trams
        Station thirdStation = TraffordCentre.fakeWithPlatform(2, TramDate.of(date));
        Platform platfromForThirdStation = TestEnv.findOnlyPlatform(thirdStation);

        TramStationDepartureInfo thirdStationInfo = new TramStationDepartureInfo("displayId3", OverheadDisplayLines.Airport,
        LineDirection.Incoming, thirdStation, "message 3", lastUpdate, platfromForThirdStation);
        infos.add(thirdStationInfo);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.seenUpdate(infos);
        verifyAll();

        assertEquals(3, departureRepository.upToDateEntries());
        assertEquals(3, departureRepository.getNumStationsWithData(lastUpdate));
        assertEquals(2, departureRepository.getNumStationsWithTrams(lastUpdate));
    }

    @Test
    void shouldGetDepartureInformationForSingleStation() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, destination, "Due",
                expectedDue, "Single", agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform,
                "some message", station, dueTram);

        Station otherStation = Altrincham.fakeWithPlatform(1, TramDate.of(date));
        Platform otherPlatform = TestEnv.findOnlyPlatform(otherStation);

                Station destinationManAirport = ManAirport.fake();
        UpcomingDeparture dueTramOther = new UpcomingDeparture(date, otherStation, destinationManAirport, "Due",
                TramTime.ofHourMins(lastUpdate.toLocalTime()).plus(Duration.ofMinutes(12)), "Double", agency, mode);
        addStationInfoWithDueTram(infos, lastUpdate, "displayXXX", otherPlatform,
                "some message", otherStation, dueTramOther);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.seenUpdate(infos);
        verifyAll();

        assertEquals(2, departureRepository.upToDateEntries());
        assertEquals(2, departureRepository.getNumStationsWithData(lastUpdate));

        List<UpcomingDeparture> results = departureRepository.forStation(station);

        assertEquals(1, results.size());
        UpcomingDeparture result = results.getFirst();
        assertEquals("Due", result.getStatus());
        //assertMinutesEquals(42, result.getWait());
        assertEquals(lastUpdate.plusMinutes(42).toLocalTime(), result.getWhen().asLocalTime());
        assertEquals("Single", result.getCarriages());
        assertEquals(destination.getId(), result.getDestinationId());

        List<UpcomingDeparture> resultOther = departureRepository.forStation(otherStation);
        assertEquals(1, resultOther.size());
        assertEquals(destinationManAirport.getId(), resultOther.getFirst().getDestinationId());
    }

    @Test
    void shouldGetDepartureInformationForSingleStationDueTramOnly() {
        List<TramStationDepartureInfo> infos = new ArrayList<>();

        Station destination = Bury.fake();
        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, destination, "Due",
                expectedDue, "Single", agency, mode);
        dueTram.setPlatform(platform);

        addStationInfoWithDueTram(infos, lastUpdate, "displayId", platform,
                "some message", station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.seenUpdate(infos);
        verifyAll();

        assertEquals(1, departureRepository.upToDateEntries());
        assertEquals(1, departureRepository.getNumStationsWithData(lastUpdate));
        assertEquals(1, departureRepository.getNumStationsWithTrams(lastUpdate));


        List<UpcomingDeparture> allTramsForStation = departureRepository.forStation(station);

        List<UpcomingDeparture> allTramsForPlatform = allTramsForStation.stream().
                filter(departure -> departure.getPlatform().equals(platform))
                        .toList();

        assertFalse(allTramsForPlatform.isEmpty());

    }

    @Test
    void shouldGetDueTramsWithinTimeWindows() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        UpcomingDeparture dueTram = new UpcomingDeparture(date, station, station, "Due",
                expectedDue, "Single", agency, mode);
        addStationInfoWithDueTram(info, lastUpdate, "displayId", platform, "some message",
                station, dueTram);

        EasyMock.expect(providesNow.getDateTime()).andStubReturn(lastUpdate);

        replayAll();
        departureRepository.seenUpdate(info);
        List<UpcomingDeparture> dueTramsNow = departureRepository.forStation(station);
        List<UpcomingDeparture> dueTramsEarlier = departureRepository.forStation(station);
        List<UpcomingDeparture> dueTramsLater = departureRepository.forStation(station);
        verifyAll();

        assertEquals(1, dueTramsNow.size());
        assertEquals(1, dueTramsEarlier.size());
        assertEquals(1, dueTramsLater.size());

        assertEquals(1, departureRepository.getNumStationsWithData(lastUpdate.minusMinutes(5)));
        assertEquals(1, departureRepository.getNumStationsWithData(lastUpdate.plusMinutes(5)));
    }

    static void addStationInfoWithDueTram(List<TramStationDepartureInfo> info, LocalDateTime lastUpdate,
                                          String displayId, Platform platform,
                                          String message,
                                          Station location, UpcomingDeparture upcomingDeparture) {
        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId, OverheadDisplayLines.Eccles,
                LineDirection.Incoming, location, message, lastUpdate, platform);
        departureInfo.addDueTram(upcomingDeparture);

        info.add(departureInfo);

    }
}
