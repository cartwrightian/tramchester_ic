package com.tramchester.unit.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.LiveDataObserver;
import com.tramchester.livedata.tfgm.*;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.Bury;

public class LiveDataMarshallerTest extends EasyMockSupport {

    private LiveDataParser mapper;
    private LiveDataMarshaller repository;
    private ProvidesNow providesNow;
    private LocalDateTime lastUpdate;
    private LiveDataObserver subscriber;

    @BeforeEach
    void beforeEachTestRuns() {
        LiveDataFetcher fetcher = createMock(LiveDataHTTPFetcher.class);
        mapper = createMock(LiveDataParser.class);
        providesNow = createMock(ProvidesNow.class);

        repository = new LiveDataMarshaller(fetcher, mapper, providesNow);

        subscriber = createMock(LiveDataObserver.class);

        lastUpdate = TestEnv.LocalNow();

        repository.addSubscriber(subscriber);
    }

    @Test
    void shouldUpdateSubscriber() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        info.add(createDepartureInfoWithDueTram(lastUpdate, "yyy", "platformIdA",
                "some message", Altrincham.fake()));
        info.add(createDepartureInfoWithDueTram(lastUpdate, "303", "platformIdB",
                "some message", Altrincham.fake()));

        EasyMock.expect(providesNow.getNowHourMins()).andStubReturn(TramTime.ofHourMins(lastUpdate.toLocalTime()));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        EasyMock.expect(subscriber.seenUpdate(info)).andReturn(true);

        replayAll();
        repository.rawData("someData");
        verifyAll();
    }

    @Test
    void shouldUpdateSubscriberIgnoringStaleData() {
        List<TramStationDepartureInfo> info = new LinkedList<>();

        Station station = Altrincham.fake();
        TramStationDepartureInfo departureInfo = createDepartureInfoWithDueTram(lastUpdate, "yyy", "platformIdA",
                "some message", station);

        info.add(departureInfo);
        info.add(createDepartureInfoWithDueTram(lastUpdate.plusMinutes(25), "303", "platformIdB",
                "some message", station));
        info.add(createDepartureInfoWithDueTram(lastUpdate.minusMinutes(25), "444", "platformIdC",
                "some message", station));

        EasyMock.expect(providesNow.getNowHourMins()).andStubReturn(TramTime.ofHourMins(lastUpdate.toLocalTime()));
        EasyMock.expect(providesNow.getDate()).andStubReturn(lastUpdate.toLocalDate());

        EasyMock.expect(mapper.parse("someData")).andReturn(info);

        List<TramStationDepartureInfo> expected = Collections.singletonList(departureInfo);

        EasyMock.expect(subscriber.seenUpdate(expected)).andReturn(true);

        replayAll();
        repository.rawData("someData");
        verifyAll();
    }

    public static TramStationDepartureInfo createDepartureInfoWithDueTram(LocalDateTime lastUpdate,
                                                                          String displayId, String platformId, String message,
                                                                          Station station) {
        Platform platform = TestEnv.createPlatformFor(station, platformId);

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId, Lines.Airport,
                LineDirection.Incoming, station, message, lastUpdate, platform);
        UpcomingDeparture dueTram = new UpcomingDeparture(lastUpdate.toLocalDate(), station, Bury.fake(),
                "Due", TramTime.ofHourMins(lastUpdate.toLocalTime()).plusMinutes(42), "Single",
                TestEnv.MetAgency(), TransportMode.Tram);
        departureInfo.addDueTram(dueTram);
        return departureInfo;
    }
}
