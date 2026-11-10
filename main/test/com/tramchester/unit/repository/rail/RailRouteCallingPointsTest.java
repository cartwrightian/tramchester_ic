package com.tramchester.unit.repository.rail;

import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.repository.rail.RailRouteIdsTest.LONGEST_VIA_STOKE;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

public class RailRouteCallingPointsTest {

    private IdFor<Agency> agencyId;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        agencyId = Agency.createId("NT");
    }

    @Test
    void shouldSortBySizeCallingPoints() {
        // shorter first
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport, StokeOnTrent);

        int result = routeA.compareTo(routeB);
        assertTrue(result>0, Integer.toString(result));
    }

    @Test
    void shouldRespectModeWhenCheckingIfContained() {
        List<IdFor<Station>> idList = getStationIds(ManchesterPiccadilly, Stockport, LondonEuston);
        RailRouteCallingPoints callingA = new RailRouteCallingPoints(agencyId, idList, Train);
        RailRouteCallingPoints callingB = new RailRouteCallingPoints(agencyId, idList, RailReplacementBus);

        assertFalse(callingA.contains(callingB));
        assertFalse(callingB.contains(callingA));
    }

    @Test
    void shouldHaveContainsForAgencyCallingPoints() {

        TransportMode mode = Train;

        List<IdFor<Station>> idListA = LONGEST_VIA_STOKE.stream().map(Station::createId).collect(Collectors.toList());

        RailRouteCallingPoints callingA = new RailRouteCallingPoints(agencyId, idListA, mode);

        assertTrue(callingA.contains(callingA));

        List<IdFor<Station>> idListB = getStationIds(ManchesterPiccadilly, Stockport, Crewe, LondonEuston);
        RailRouteCallingPoints callingB = new RailRouteCallingPoints(agencyId, idListB, mode);

        assertFalse(callingA.contains(callingB));
        assertFalse(callingB.contains(callingA));

        List<IdFor<Station>> idListC = getStationIds(ManchesterPiccadilly, Stockport, LondonEuston);
        RailRouteCallingPoints callingC = new RailRouteCallingPoints(agencyId, idListC, mode);

        assertTrue(callingA.contains(callingC));
        assertFalse(callingC.contains(callingA));
        assertFalse(callingC.contains(callingB));

        List<IdFor<Station>> idListD = getStationIds(ManchesterPiccadilly, Stockport, Macclesfield, StokeOnTrent, LondonEuston);
        RailRouteCallingPoints callingD = new RailRouteCallingPoints(agencyId, idListD, mode);

        assertTrue(callingA.contains(callingD));
        assertTrue(callingD.contains(callingC));
        assertFalse(callingD.contains(callingB));

    }

    @Test
    void shouldSortByModeCallingPoints() {
        // shorter first
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train,
                ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, RailReplacementBus,
                ManchesterPiccadilly, Stockport);

        int result = routeA.compareTo(routeB);
        assertTrue(result<0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsReversed() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport, Altrincham);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Macclesfield);

        int result = routeA.compareTo(routeB);
        assertTrue(result<0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsThenNames() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Macclesfield);

        int result = routeA.compareTo(routeB);
        assertTrue(result>0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsThenNamesReversed() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Macclesfield);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport);

        int result = routeA.compareTo(routeB);
        assertTrue(result<0, Integer.toString(result));
    }

    @Test
    void shouldSortBySizeCallingPointsThenNamesSame() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport);

        int result = routeA.compareTo(routeB);
        assertEquals(0,result, Integer.toString(result));
    }

    @Test
    void shouldHaveExpectedOrderLongestFirst() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport, StokeOnTrent);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport, StokeOnTrent, LondonEuston);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, Train, ManchesterPiccadilly, StokeOnTrent);

        List<RailRouteCallingPoints> list = Arrays.asList(routeA, routeB, routeC);

        List<RailRouteCallingPoints> result = list.stream().sorted(RailRouteCallingPoints::compareTo).toList();

        assertEquals(routeB, result.get(0));
        assertEquals(routeA, result.get(1));
        assertEquals(routeC, result.get(2));

    }

    @Test
    void shouldHaveExpectedOrderSameLength() {
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train, ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, Train, ManchesterPiccadilly, Macclesfield);
        RailRouteCallingPoints routeC = formRailRoute(agencyId, Train, ManchesterPiccadilly, LondonEuston);

        List<RailRouteCallingPoints> list = Arrays.asList(routeA, routeB, routeC);

        List<RailRouteCallingPoints> result = list.stream().sorted(RailRouteCallingPoints::compareTo).toList();

        assertEquals(routeC, result.get(0));
        assertEquals(routeB, result.get(1));
        assertEquals(routeA, result.get(2));

    }

    @Test
    void shouldNotBeEqualsIfModeDifferent() {
        // shorter first
        RailRouteCallingPoints routeA = formRailRoute(agencyId, Train,
                ManchesterPiccadilly, Stockport);
        RailRouteCallingPoints routeB = formRailRoute(agencyId, RailReplacementBus,
                ManchesterPiccadilly, Stockport);

        assertNotEquals(routeA, routeB);
    }

    private RailRouteCallingPoints formRailRoute(IdFor<Agency> agencyId, TransportMode mode, RailStationIds... stationIds) {
        return new RailRouteCallingPoints(agencyId, idsFor(stationIds), mode);
    }

    private List<IdFor<Station>> idsFor(RailStationIds... stationIds) {
        return Arrays.stream(stationIds).map(RailStationIds::getId).collect(Collectors.toList());
    }

    private List<IdFor<Station>> getStationIds(RailStationIds... railStationIds) {
        return idsFor(railStationIds);
    }
}
