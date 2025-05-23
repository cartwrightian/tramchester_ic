package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.factory.LocationDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class LocationDTOTest {

    private LocationDTOFactory factory;

    @BeforeEach
    void beforeEachTestRuns() {
        factory = new LocationDTOFactory();
    }

    @Test
    void shouldCreateDTOAsExpected() {

        MutableStation testStation = StationHelper.forTestMutable("9400ZZMAALT", "Altrincham area", "Altrincham",
                nearAltrincham, DataSourceID.tfgm, false);

        testStation.addRouteDropOff(TestEnv.getTramTestRoute(Route.createBasicRouteId("routeIdA"), "routeNameA"));
        testStation.addRoutePickUp(TestEnv.getTramTestRoute(Route.createBasicRouteId("routeIdB"), "routeNameB"));

        testStation.addPlatform(MutablePlatform.buildForTFGMTram("1", testStation, new LatLong(1.2, 1),
                DataSourceID.unknown, NPTGLocality.InvalidId()));
        testStation.addPlatform(MutablePlatform.buildForTFGMTram("2", testStation, new LatLong(1.1, 1),
                DataSourceID.unknown, NPTGLocality.InvalidId()));

        LocationDTO dto = factory.createLocationDTO(testStation); //new LocationDTO(testStation);

        assertEquals(IdForDTO.createFor(testStation), dto.getId());
        assertEquals(testStation.getTransportModes(), dto.getTransportModes());
        assertEquals(testStation.getName(), dto.getName());
        assertEquals(testStation.getLatLong(), dto.getLatLong());
        assertTrue(dto.isTram());

        assertEquals(2, dto.getRoutes().size());
        Set<String> routeNames = dto.getRoutes().stream().map(RouteRefDTO::getRouteName).collect(Collectors.toSet());
        assertTrue(routeNames.contains("routeNameA"));
        assertTrue(routeNames.contains("routeNameB"));

        assertTrue(dto.hasPlatforms());
        assertEquals(2, dto.getPlatforms().size());

        Optional<PlatformDTO> findPlatformOne = getPlatformById(dto, TramStations.Altrincham.getRawId() + "1");
        assertTrue(findPlatformOne.isPresent(), "expected platform not found in " + dto);
        PlatformDTO platformDTOA = findPlatformOne.get();
        assertEquals("Altrincham platform 1", platformDTOA.getName());
        assertEquals("1", platformDTOA.getPlatformNumber());

        Optional<PlatformDTO> findPlatformTwo = getPlatformById(dto, TramStations.Altrincham.getRawId() + "2");
        assertTrue(findPlatformTwo.isPresent());
        PlatformDTO platformDTOB = findPlatformTwo.get();
        assertEquals("Altrincham platform 2", platformDTOB.getName());
        assertEquals("2", platformDTOB.getPlatformNumber());

    }

    @NotNull
    private Optional<PlatformDTO> getPlatformById(LocationDTO dto, String platformId) {
        return dto.getPlatforms().stream().
                filter(platformDTO -> platformDTO.getId().getActualId().equals(platformId)).findFirst();
    }

}
