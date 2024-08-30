package com.tramchester.unit.mappers;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.tfgm.Lines;
import com.tramchester.livedata.tfgm.TramDepartureFactory;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepositoryPublic;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.tramchester.testSupport.reference.TramStations.ManAirport;
import static org.junit.jupiter.api.Assertions.*;

public class TramDepartureFactoryTest extends EasyMockSupport {
    private TramDepartureFactory tramDepartureFactory;
    private StationRepositoryPublic stationRepository;
    private PlatformRepository platformRepository;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        AgencyRepository agencyRepository = createStrictMock(AgencyRepository.class);
        stationRepository = createStrictMock(StationRepositoryPublic.class);
        platformRepository = createStrictMock(PlatformRepository.class);
        tramDepartureFactory = new TramDepartureFactory(agencyRepository, stationRepository, platformRepository);
    }

    @Test
    void shouldReturnNullIfStationIsMissing() {

        EasyMock.expect(stationRepository.hasStationId(Station.createId("wrongActo"))).andReturn(false);

        replayAll();
        TramStationDepartureInfo result = tramDepartureFactory.createStationDeparture(BigDecimal.valueOf(42), Lines.Altrincham, LineDirection.Outgoing,
                "wrongActo", "platform message", TestEnv.LocalNow());
        verifyAll();

        assertNull(result);
    }

    @Test
    void shouldHaveCreateWithPlatform() {
        LocalDateTime updateTime = TestEnv.LocalNow();

        Station airport = ManAirport.fakeWithPlatform("2", ManAirport.getLatLong(), DataSourceID.unknown,
                NPTGLocality.InvalidId());

        Platform airportPlatform = TestEnv.findOnlyPlatform(airport);

        IdFor<Station> stationId = airport.getId();

        EasyMock.expect(stationRepository.hasStationId(stationId)).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(stationId)).andReturn(airport);
        PlatformId platformId = PlatformId.createId(stationId, "2");
        EasyMock.expect(platformRepository.hasPlatformId(platformId)).andReturn(true);
        EasyMock.expect(platformRepository.getPlatformById(platformId)).andReturn(airportPlatform);

        replayAll();
        TramStationDepartureInfo result = tramDepartureFactory.createStationDeparture(BigDecimal.valueOf(42), Lines.Altrincham, LineDirection.Outgoing,
                TramStations.ManAirport.getRawId()+"2", "platform message", updateTime);
        verifyAll();

        assertNotNull(result);
        assertEquals("42", result.getDisplayId());
        assertEquals(stationId, result.getStation().getId());
        assertEquals(Lines.Altrincham, result.getLine());
        assertEquals(LineDirection.Outgoing, result.getDirection());
        assertEquals(updateTime , result.getLastUpdate());
        assertEquals("platform message", result.getMessage());
        assertFalse(result.hasDueTrams());
        assertTrue(result.hasStationPlatform());
        assertEquals(airportPlatform, result.getStationPlatform());
    }

    @Test
    void shouldHaveNotPlatformIfNotFoundInRepo() {
        EasyMock.expect(stationRepository.hasStationId(ManAirport.getId())).andReturn(true);
        EasyMock.expect(stationRepository.getStationById(ManAirport.getId())).andReturn(ManAirport.fake());
        PlatformId platformId = PlatformId.createId(ManAirport.getId(), "9");
        EasyMock.expect(platformRepository.hasPlatformId(platformId)).andReturn(false);

        replayAll();
        TramStationDepartureInfo result = tramDepartureFactory.createStationDeparture(BigDecimal.valueOf(42), Lines.Altrincham, LineDirection.Outgoing,
                TramStations.ManAirport.getRawId()+"9", "platform message", TestEnv.LocalNow());
        verifyAll();

        assertNotNull(result);
        assertEquals("42", result.getDisplayId());
        assertFalse(result.hasStationPlatform());
    }
}
