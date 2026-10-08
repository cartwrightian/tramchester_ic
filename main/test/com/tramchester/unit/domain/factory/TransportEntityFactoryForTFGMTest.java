package com.tramchester.unit.domain.factory;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.tramchester.domain.factory.TransportEntityFactoryForTFGM.getStationIdFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransportEntityFactoryForTFGMTest extends EasyMockSupport {

    private NaptanRepository naptanRepository;
    private TransportEntityFactoryForTFGM entityFactory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        naptanRepository = createMock(NaptanRepository.class);
        EasyMock.expect(naptanRepository.isEnabled()).andStubReturn(false);
        entityFactory = new TransportEntityFactoryForTFGM(naptanRepository);
    }

    @Test
    void testShouldFormIdByRemovingPlatformForTramStopIfRequired() {
        assertEquals(Station.createId("9400ZZid"), getStationIdFor("9400ZZid1"));

        assertEquals(Station.createId("9400XXid1"), getStationIdFor("9400XXid1"));

    }

    @Test
    void shouldRemoveStationFromPlatformNumberIfPresent() {
        IdFor<Station> stationId = TramStations.ImperialWarMuseum.getId();
        final String actoCode = "9400ZZMAIWM1";
        PlatformId platformId = entityFactory.getPlatformIdFrom(actoCode, stationId);

        assertEquals("1", platformId.getNumber());
    }

    @Test
    void shouldNotRemoveStationFromPlatformNumberIfNoMatch() {
        IdFor<Station> stationId = TramStations.ImperialWarMuseum.getId();
        String platformNumber = "42";
        PlatformId platformId = entityFactory.getPlatformIdFrom(platformNumber, stationId);

        assertEquals("42", platformId.getNumber());
    }

    @Test
    void shouldRemoveNumericPlatformId() {
        IdFor<Station> result = getStationIdFor("9400ZZMASTP1");
        assertEquals(TramStations.StPetersSquare.getId(), result);
    }

    @Test
    void shouldDealWithSaleWithoutPlatform() {
        IdFor<Station> result = getStationIdFor("9400ZZMASLE");
        assertEquals(TramStations.Sale.getId(), result);
    }

    @Test
    void shouldHandlePlatformNumbers() {
        TramStations sale = TramStations.Sale;
        StopData stopData = new StopData("12345", sale.getRawId()+"1", sale.getLatLong().getLat(),
                sale.getLatLong().getLon(), sale.getName());

        replayAll();
        entityFactory.formStationId(stopData);
        Optional<MutablePlatform> result = entityFactory.maybeCreatePlatform(stopData, sale.fake());
        verifyAll();
        assertTrue(result.isPresent());

        Platform platformId = result.get();
        assertEquals("1", platformId.getPlatformNumber());
    }

    @Test
    void shouldHandleMissingPlatformNumbers() {
        TramStations sale = TramStations.Sale;
        String underlyingId = "12345";
        StopData stopData = new StopData(underlyingId, sale.getRawId(), sale.getLatLong().getLat(),
                sale.getLatLong().getLon(), sale.getName());
        StopTimeData stopTimeData = new StopTimeData("tripXYZ", "09:31", "09:35",
                underlyingId, 4, GTFSPickupDropoffType.Regular.name(), GTFSPickupDropoffType.Regular.name());

        replayAll();
        final IdFor<Station> createdId = entityFactory.formStationId(stopData);
        assertEquals(sale.getId(), createdId);

        Optional<MutablePlatform> result = entityFactory.maybeCreatePlatform(stopData, sale.fake());
        PlatformId subsequentCreate = entityFactory.getPlatformId(stopTimeData, sale.fake());
        verifyAll();

        assertTrue(result.isPresent());

        Platform platform = result.get();
        assertEquals("unknown", platform.getPlatformNumber());

        assertEquals(platform.getId(), subsequentCreate);
    }
}
