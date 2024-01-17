package com.tramchester.integration.resources.journeyPlanning;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.App;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.ConfigDTO;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.JourneyQueryDTO;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.KnowLocality;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.testSupport.reference.BusStations.*;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerBusTest {

    private static final IntegrationBusTestConfig configuration = new IntegrationBusTestConfig();
    private static final IntegrationAppExtension appExt = new IntegrationAppExtension(App.class, configuration);

    private TramDate when;
    private JourneyResourceTestFacade journeyResourceTestFacade;
    private StationGroup stockportCentralStops;
    private StationGroup nearShudehillCentralStops;
    private ObjectMapper mapper;
    private StationGroupsRepository stationGroupRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        journeyResourceTestFacade = new JourneyResourceTestFacade(appExt);

        App app = appExt.getTestSupport().getApplication();
        GuiceContainerDependencies dependencies = app.getDependencies();

        CentralStops centralStops = new CentralStops(dependencies);

        stockportCentralStops = centralStops.Stockport();
        nearShudehillCentralStops = centralStops.Shudehill();

        mapper = new ObjectMapper();

        stationGroupRepository = dependencies.get(StationGroupsRepository.class);
    }

    @Test
    void shouldGetTransportModes() {
        Response response = APIClient.getApiResponse(appExt, "version/modes");
        assertEquals(200, response.getStatus());

        ConfigDTO result = response.readEntity(new GenericType<>() {});

        List<TransportMode> results = result.getModes();
        assertFalse(results.isEmpty());

        List<TransportMode> expected = Collections.singletonList(TransportMode.Bus);
        assertEquals(expected, results);
    }

    @Test
    void shouldBusJourneyWestEast() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO query1 = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtAltrinchamInterchange,
                stockportCentralStops, false, 2);

        JourneyPlanRepresentation planA = journeyResourceTestFacade.getJourneyPlan(query1);
        List<JourneyDTO> foundA = getValidJourneysAfter(queryTime, planA);
        Assertions.assertFalse(foundA.isEmpty());

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, stockportCentralStops,
                StopAtAltrinchamInterchange, false, 2);

        JourneyPlanRepresentation planB = journeyResourceTestFacade.getJourneyPlan(query);
        List<JourneyDTO> foundB = getValidJourneysAfter(queryTime, planB);
        Assertions.assertFalse(foundB.isEmpty());
    }

    @Test
    void shouldPlanBusJourneySouthern() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO shudehillToStockport = journeyResourceTestFacade.getQueryDTO(when, queryTime, nearShudehillCentralStops,
                stockportCentralStops, false, 2);

        JourneyPlanRepresentation southNorth = journeyResourceTestFacade.getJourneyPlan(shudehillToStockport);
        List<JourneyDTO> southNorthFound = getValidJourneysAfter(queryTime, southNorth);
        Assertions.assertFalse(southNorthFound.isEmpty());

        JourneyQueryDTO stockportToShudehill = journeyResourceTestFacade.getQueryDTO(when, queryTime,
                stockportCentralStops, StopAtShudehillInterchange, false, 3);

        JourneyPlanRepresentation northSouth = journeyResourceTestFacade.getJourneyPlan(stockportToShudehill);
        List<JourneyDTO> foundWith3Changes = getValidJourneysAfter(queryTime, northSouth);
        Assertions.assertFalse(foundWith3Changes.isEmpty());
    }

    @Test
    void shouldPlanBusJourneyNorthern() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO query1 = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtShudehillInterchange,
                BuryInterchange, false, 2);

        JourneyPlanRepresentation planA = journeyResourceTestFacade.getJourneyPlan(query1);
        List<JourneyDTO> foundA = getValidJourneysAfter(queryTime, planA);
        Assertions.assertFalse(foundA.isEmpty());


        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, BuryInterchange,
                StopAtShudehillInterchange, false, 2);

        JourneyPlanRepresentation planB = journeyResourceTestFacade.getJourneyPlan(query);
        List<JourneyDTO> foundB = getValidJourneysAfter(queryTime, planB);
        Assertions.assertFalse(foundB.isEmpty());
    }

    @Test
    void shouldPlanBusJourneyNoLoops() {
        TramTime queryTime = TramTime.of(8,56);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtAltrinchamInterchange, ManchesterAirportStation,
                false, 2);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());

        found.forEach(result -> {
            Set<String> stageIds= new HashSet<>();
            result.getStages().forEach(stage -> {
                String id = stage.getActionStation().getId().getActualId();
                Assertions.assertFalse(stageIds.contains(id), "duplicate stations id found during " +result);
                stageIds.add(id);
            });
        });
    }

    @Test
    void shouldSetRecentCookieForAJourneyStations() throws IOException {
        LocalDateTime timeStamp = TestEnv.LocalNow();

        TramTime queryTime = TramTime.of(8,56);

        BusStations start = StopAtAltrinchamInterchange;
        BusStations end = ManchesterAirportStation;

        Timestamped expectedStart = new Timestamped(start.getIdForDTO(), timeStamp, LocationType.Station);
        Timestamped expectedEnd = new Timestamped(end.getIdForDTO(), timeStamp, LocationType.Station);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, start, end,
                false, 2);

        Response result = journeyResourceTestFacade.getResponse(false, Collections.emptyList(), query);

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        Assertions.assertEquals(2,recentJourneys.getTimeStamps().size());

        assertTrue(recentJourneys.getTimeStamps().contains(expectedStart), recentJourneys.toString());
        assertTrue(recentJourneys.getTimeStamps().contains(expectedEnd), recentJourneys.toString());
    }

    @Test
    void shouldSetRecentCookieForAJourneyStationGroup() throws IOException {
        LocalDateTime timeStamp = TestEnv.LocalNow();

        TramTime queryTime = TramTime.of(8,56);

        StationGroup start = KnowLocality.Altrincham.from(stationGroupRepository);
        StationGroup end = KnowLocality.ManchesterAirport.from(stationGroupRepository);

        Timestamped expectedStart = new Timestamped(start, timeStamp);
        Timestamped expectedEnd = new Timestamped(end, timeStamp);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, start, end,
                false, 2);

        Response result = journeyResourceTestFacade.getResponse(false, Collections.emptyList(), query);

        RecentJourneys recentJourneys = getRecentJourneysFromCookie(result);

        Assertions.assertEquals(2,recentJourneys.getTimeStamps().size());

        assertTrue(recentJourneys.getTimeStamps().contains(expectedStart), recentJourneys.toString());
        assertTrue(recentJourneys.getTimeStamps().contains(expectedEnd), recentJourneys.toString());
    }

    private RecentJourneys getRecentJourneysFromCookie(Response response) throws IOException {
        Map<String, NewCookie> cookies = response.getCookies();
        NewCookie recent = cookies.get("tramchesterRecent");
        String value = recent.toCookie().getValue();
        return RecentJourneys.decodeCookie(mapper, value);
    }

    @Test
    void shouldPlanSimpleBusJourneyFromLocation() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO query = JourneyQueryDTO.create(when, queryTime, nearAltrincham.location(), stockportCentralStops, false, 2);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Test
    void shouldPlanDirectWalkToBusStopFromLocation() {
        TramTime queryTime = TramTime.of(8,15);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, nearAltrincham.location(), StopAtAltrinchamInterchange, false, 3);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Test
    void shouldPlanSimpleJourneyArriveByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, stockportCentralStops, StopAtAltrinchamInterchange, true, 3);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        // TODO 20 mins gap? Estimation is too optimistic for Buses?
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            Assertions.assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            Duration duration = Duration.between(journeyDTO.getExpectedArrivalTime(), queryTime.toDate(when));
            if (duration.getSeconds() < 20*60) {
                found.add(journeyDTO);
            }
        });
        Assertions.assertFalse(found.isEmpty());
    }

    private List<JourneyDTO> getValidJourneysAfter(TramTime queryTime, JourneyPlanRepresentation plan) {
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            LocalDateTime firstDepartureTime = journeyDTO.getFirstDepartureTime();
            Assertions.assertTrue(firstDepartureTime.isAfter(queryTime.toDate(when))
                    || firstDepartureTime.equals(queryTime.toDate(when)), firstDepartureTime.toString());
            if (journeyDTO.getExpectedArrivalTime().isAfter(queryTime.toDate(when))) {
                found.add(journeyDTO);
            }
        });
        return found;
    }

}
