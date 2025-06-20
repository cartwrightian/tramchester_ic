package com.tramchester.unit.mappers;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.StationByName;
import com.tramchester.livedata.tfgm.LiveDataParser;
import com.tramchester.livedata.tfgm.OverheadDisplayLines;
import com.tramchester.livedata.tfgm.TramDepartureFactory;
import com.tramchester.livedata.tfgm.TramStationDepartureInfo;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class LiveDataParserTest extends EasyMockSupport {

    private LiveDataParser parser;
    private StationByName stationByName;
    private Platform mediaCityPlatform;
    private TramDepartureFactory departureFactory;

    @BeforeEach
    void beforeEachTestRuns() {
        stationByName = createStrictMock(StationByName.class);

        Station mediaCity = MediaCityUK.fakeWithPlatform(2, TestEnv.testDay());

        mediaCityPlatform = TestEnv.findOnlyPlatform(mediaCity);
        departureFactory = createMock(TramDepartureFactory.class);

        parser = new LiveDataParser(stationByName, departureFactory);

    }

    private void expectationByName(TramStations station) {
        EasyMock.expect(stationByName.getTramStationByName(station.getName())).andStubReturn(Optional.of(station.fake()));
    }

    @Test
    void shouldMapTimesCorrectlyDuringEarlyHours() {
        String header = "{\n \"@odata.context\":\"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks\",\"value\":[\n";
        String footer = "]\n }\n";

        StringBuilder message = new StringBuilder();
        message.append(header);
        for (int i = 1; i < 12; i++) {
            if (i>1) {
                message.append(",\n");
            }
            String line = String.format("""
                    {
                    "Id":%s,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK",
                    "AtcoCode":"9400ZZMAMCU2","Direction":"Incoming","Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due",
                    "Wait0":"1","Dest1":"","Carriages1":"","Status1":"","Wait1":"","Dest2":"",
                    "Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"",
                    "MessageBoard":"Test.","Wait3":"","LastUpdated":"2017-11-29T%02d:45:00Z"
                        }""", i, i);
            message.append(line);
            LocalDateTime expectedDateTime = LocalDateTime.of(2017, 11, 29, i, 45);
            TramStationDepartureInfo dep = setExpectationsForDeparture(i, OverheadDisplayLines.Eccles, LineDirection.Incoming, "Test.",
                    MediaCityUK, expectedDateTime, "2", mediaCityPlatform);
            expectDueTram(dep, Piccadilly, 1, "Due", "Single", mediaCityPlatform);
        }
        message.append(footer);

        expectationByName(Piccadilly);

        replayAll();
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(message.toString());
        assertEquals(11, info.size());
        for (int i = 1; i < 12; i++) {
            LocalDateTime expected = LocalDateTime.of(2017, 11, 29, i, 45);
            assertEquals(expected, info.get(i-1).getLastUpdate(), expected.toString());
        }
        parser.stop();
        verifyAll();
    }

    @Test
    void shouldCreateDueTram() {
        String exampleData = """
            {
              "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                {
                  "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming","Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due","Wait0":"1","Dest1":"","Carriages1":"","Status1":"","Wait1":"","Dest2":"","Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                }]
             }
            """;

        String msg = "message A";

        expectationByName(Piccadilly);
        expectationByName(Deansgate);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);
        TramStationDepartureInfo depA = setExpectationsForDeparture(1, OverheadDisplayLines.Eccles, LineDirection.Incoming, msg, MediaCityUK, dateTimeA, "2", mediaCityPlatform);

        expectDueTram(depA, Piccadilly, 1, "Due", "Single", mediaCityPlatform);

        List<TramStationDepartureInfo> infos = doParsing(exampleData);

        assertEquals(1, infos.size());

        TramStationDepartureInfo info = infos.getFirst();

        assertTrue(info.hasDueTrams());

        List<UpcomingDeparture> dueTrams = info.getDueTrams();

        assertEquals(1, dueTrams.size());

        UpcomingDeparture dueTram = dueTrams.getFirst();

        assertEquals(TestEnv.MetAgency(), dueTram.getAgency());
        assertEquals(Piccadilly.getId() , dueTram.getDestinationId());
        assertEquals(TramTime.of(11,46), dueTram.getWhen());
        assertEquals("Due", dueTram.getStatus());
        assertEquals("Single", dueTram.getCarriages());
        assertTrue(dueTram.hasPlatform());
        assertEquals(mediaCityPlatform, dueTram.getPlatform());
    }

    @Test
    void shouldMapLiveDataToStationInfo() {

        String exampleData = """
            {
              "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                {
                  "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming",
                  "Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due","Wait0":"1",
                  "Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12",
                  "Dest2":"","Carriages2":"","Status2":"","Wait2":"",
                  "Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                },{
                  "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming",
                  "Dest0":"Deansgate Castlefield","Carriages0":"Single","Status0":"Due","Wait0":"5",
                  "Dest1":"Altrincham","Carriages1":"Double","Status1":"Delay","Wait1":"17",
                  "Dest2":"","Carriages2":"","Status2":"","Wait2":"",
                  "Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message B","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                }]
             }
            """;

        String msgA = "message A";
        String msgB = "message B";

        expectationByName(Altrincham);
        expectationByName(Piccadilly);
        expectationByName(Deansgate);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);
        TramStationDepartureInfo depA = setExpectationsForDeparture(1, OverheadDisplayLines.Eccles, LineDirection.Incoming, msgA, MediaCityUK, dateTimeA, "2", mediaCityPlatform);

        expectDueTram(depA, Piccadilly, 1, "Due", "Single", mediaCityPlatform);
        expectDueTram(depA, Piccadilly, 12, "Due", "Single", mediaCityPlatform);

        LocalDateTime dateTimeB = LocalDateTime.of(2017,6,29,13,55).plusHours(1);
        TramStationDepartureInfo depB = setExpectationsForDeparture(234, OverheadDisplayLines.Airport, LineDirection.Incoming, msgB, ManAirport, dateTimeB, "1", null);

        expectDueTram(depB, Deansgate, 5, "Due", "Single", null);
        expectDueTram(depB, Altrincham, 17, "Delay", "Double", null);


        List<TramStationDepartureInfo> info = doParsing(exampleData);

        assertEquals(2, info.size());

        TramStationDepartureInfo departureInfoA = info.getFirst();
        assertEquals("1", departureInfoA.getDisplayId());
        assertEquals(OverheadDisplayLines.Eccles, departureInfoA.getLine());
        assertTrue(departureInfoA.hasStationPlatform());
        assertEquals(mediaCityPlatform, departureInfoA.getStationPlatform());
        assertEquals(MediaCityUK.getId(), departureInfoA.getStation().getId());
        assertEquals(msgA, departureInfoA.getMessage());
        assertEquals(LineDirection.Incoming, departureInfoA.getDirection());

        List<UpcomingDeparture> dueTrams = departureInfoA.getDueTrams();
        assertEquals(2, dueTrams.size());
        UpcomingDeparture dueFromA = dueTrams.get(1);

        assertEquals(Piccadilly.getId(), dueFromA.getDestinationId());
        assertEquals("Due", dueFromA.getStatus());
        assertEquals("Single",dueFromA.getCarriages());
        assertEquals(TramTime.of(11,45).plusMinutes(12), dueFromA.getWhen());
        assertEquals(mediaCityPlatform, dueFromA.getPlatform());

        ZonedDateTime expectedDateA = ZonedDateTime.of(LocalDateTime.of(2017, 11, 29, 11, 45), TramchesterConfig.TimeZoneId);
        assertEquals(expectedDateA.toLocalDateTime(), departureInfoA.getLastUpdate());

        assertEquals(departureInfoA.getLastUpdate().plusMinutes(12).toLocalTime(), dueFromA.getWhen().asLocalTime());

        // WORKAROUND - Live data erroneously gives timestamps as 'UTC'/'Z' even though they switch to DST/BST
        TramStationDepartureInfo departureInfoB = info.get(1);
        assertFalse(departureInfoB.hasStationPlatform());
        assertEquals("234", departureInfoB.getDisplayId());

        assertEquals(OverheadDisplayLines.Airport, departureInfoB.getLine());
        ZonedDateTime expectedDateB = ZonedDateTime.of(LocalDateTime.of(2017, 6, 29, 13, 55), TramchesterConfig.TimeZoneId);
        assertEquals(expectedDateB.toLocalDateTime().plusHours(1), departureInfoB.getLastUpdate());
        assertEquals(LineDirection.Incoming, departureInfoB.getDirection());

        assertEquals(2, departureInfoB.getDueTrams().size());

        UpcomingDeparture dueFromB = departureInfoB.getDueTrams().getFirst();
        assertEquals(Deansgate.getId(), dueFromB.getDestinationId());
    }

    @Test
    void shouldNOTFilterOutPlatformsNotInTimetabledData() {

        // Turns out due trams are appearing, and for some single platform stations (i.e. nav road) the live data
        // does actually have 2 display and 2 'platforms'

        String navRoadBothDisplays = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                    {
                      "Id":1,"Line":"Altrincham","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"Navigation Road","AtcoCode":"9400ZZMANAV1","Direction":"Incoming",
                      "Dest0":"Piccadilly","Carriages0":"Single","Status0":"Due","Wait0":"1",
                      "Dest1":"","Carriages1":"","":"Due","":"",
                      "Dest2":"","Carriages2":"","Status2":"","Wait2":"",
                      "Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                    },{
                      "Id":234,"Line":"Altrincham","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Navigation Road","AtcoCode":"9400ZZMANAV2","Direction":"Outgoing",
                      "Dest0":"Altrincham","Carriages0":"Single","Status0":"Due","Wait0":"5",
                      "Dest1":"","Carriages1":"","Status1":"","Wait1":"",
                      "Dest2":"","Carriages2":"Single","Status2":"Due","Wait2":"",
                      "Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message B","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                    }]
                 }
                """;

        String msgA = "message A";
        String msgB = "message B";

        expectationByName(Altrincham);
        expectationByName(Piccadilly);

        Station navRaod = NavigationRoad.fakeWithPlatform(1, TestEnv.testDay());

        Platform navRoadPlatform1 = TestEnv.findOnlyPlatform(navRaod);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);
        TramStationDepartureInfo depA = setExpectationsForDeparture(1, OverheadDisplayLines.Altrincham, LineDirection.Incoming, msgA, NavigationRoad,
                dateTimeA, "1", navRoadPlatform1);
        expectDueTram(depA, Piccadilly, 1, "Due", "Single", navRoadPlatform1);

        LocalDateTime dateTimeB = LocalDateTime.of(2017,6,29,13,55).plusHours(1);
        TramStationDepartureInfo depB = setExpectationsForDeparture(234, OverheadDisplayLines.Altrincham, LineDirection.Outgoing, msgB, NavigationRoad,
                dateTimeB, "2", null);
        expectDueTram(depB, Altrincham, 5, "Due", "Single", null);

        List<TramStationDepartureInfo> infos = this.doParsing(navRoadBothDisplays);

        assertEquals(2, infos.size());

        TramStationDepartureInfo infoA = infos.getFirst();
        assertTrue(infoA.hasDueTrams());
        List<UpcomingDeparture> dueTramsA = infoA.getDueTrams();
        assertEquals(1, dueTramsA.size());
        assertEquals(Piccadilly.getId(), dueTramsA.getFirst().getDestinationId());

        TramStationDepartureInfo infoB = infos.get(1);
        assertTrue(infoB.hasDueTrams());
        List<UpcomingDeparture> dueTramsB = infoB.getDueTrams();
        assertEquals(1, dueTramsB.size());
        assertEquals(Altrincham.getId(), dueTramsB.getFirst().getDestinationId());

    }

    @Test
    void shouldExcludeSeeTramFrontDestination()  {

        String seeTramFront = """
            {
                    "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
            {
                "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming",
                "Dest0":"See Tram Front","Carriages0":"Single","Status0":"Due","Wait0":"1",
                "Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12",
                "Dest2":"","Carriages2":"","Status2":"","Wait2":"",
                "Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
            },{
                "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming",
                "Dest0":"Deansgate Castlefield","Carriages0":"Single","Status0":"Due","Wait0":"5",
                "Dest1":"","Carriages1":"","Status1":"","Wait1":"",
                "Dest2":"","Carriages2":"","Status2":"","Wait2":"",
                "Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message B","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
            }]
                 }
            """;

        expectationByName(Piccadilly);
        expectationByName(Deansgate);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);
        TramStationDepartureInfo depA = setExpectationsForDeparture(1, OverheadDisplayLines.Eccles, LineDirection.Incoming, "message A", MediaCityUK, dateTimeA, "2", mediaCityPlatform);

        expectDueTram(depA, Piccadilly, 12, "Due", "Single", null);

        LocalDateTime dateTimeB = LocalDateTime.of(2017,6,29,13,55).plusHours(1);
        TramStationDepartureInfo depB = setExpectationsForDeparture(234, OverheadDisplayLines.Airport, LineDirection.Incoming, "message B", ManAirport, dateTimeB, "1", null);

        expectDueTram(depB, Deansgate, 5, "Due", "Single", null);

        List<TramStationDepartureInfo> info = doParsing(seeTramFront);

        assertEquals(2, info.size());
        TramStationDepartureInfo departureInfo = info.getFirst();
        assertEquals("1", departureInfo.getDisplayId());

        // filter out the "See Tram Front" destination tram
        List<UpcomingDeparture> dueTrams = departureInfo.getDueTrams();
        assertEquals(1, dueTrams.size());

        assertEquals(Piccadilly.getId(), dueTrams.getFirst().getDestinationId());

    }

    @Test
    void shouldExcludeDueTramsWithDestinationSetToNotInService() {
        String seeTramFront = """
                {
                        "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                {
                    "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming",
                    "Dest0":"Not in Service","Carriages0":"Single","Status0":"Due","Wait0":"1",
                    "Dest1":"Piccadilly","Carriages1":"Single","Status1":"Due","Wait1":"12",
                    "Dest2":"","Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                }]
                     }
                """;

        expectationByName(Piccadilly);
        expectationByName(Deansgate);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);
        TramStationDepartureInfo dep = setExpectationsForDeparture(1, OverheadDisplayLines.Eccles, LineDirection.Incoming, "message A", MediaCityUK, dateTimeA, "2", mediaCityPlatform);

        expectDueTram(dep, Piccadilly, 12, "Due", "Single", mediaCityPlatform);

        List<TramStationDepartureInfo> info = doParsing(seeTramFront);

        assertEquals(1, info.size());
        TramStationDepartureInfo departureInfo = info.getFirst();
        assertEquals("1", departureInfo.getDisplayId());

        // filter out the "Not in Service" destination tram
        List<UpcomingDeparture> dueTrams = departureInfo.getDueTrams();
        assertEquals(1, dueTrams.size());

        assertEquals(Piccadilly.getId(), dueTrams.getFirst().getDestinationId());
    }

    @Test
    void shouldParseDataWithDirectionIncomingOutgoing() {

        String exampleData = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                    {
                      "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming/Outgoing","Dest0":"","Carriages0":"","Status0":"","Wait0":"","Dest1":"","Carriages1":"","Status1":"","Wait1":"","Dest2":"","Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                    },{
                      "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming/Outgoing","Dest0":"","Carriages0":"","Status0":"","Wait0":"","Dest1":"","Carriages1":"","Status1":"","Wait1":"","Dest2":"","Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message B","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                    }]
                 }
                """;

        expectationByName(Piccadilly);
        expectationByName(Deansgate);
        expectationByName(Ashton);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);
        LocalDateTime dateTimeB = LocalDateTime.of(2017,6,29,13,55).plusHours(1);

        setExpectationsForDeparture(1, OverheadDisplayLines.Eccles, LineDirection.Both, "message A", MediaCityUK, dateTimeA, "2", mediaCityPlatform);
        setExpectationsForDeparture(234, OverheadDisplayLines.Airport, LineDirection.Both, "message B", ManAirport, dateTimeB, "1", null);

        List<TramStationDepartureInfo> info = doParsing(exampleData);

        assertEquals(2, info.size());
        assertEquals(LineDirection.Both, info.get(0).getDirection());
        assertEquals(LineDirection.Both, info.get(1).getDirection());

    }

    @Test
    void shouldParseDestinationsThatIncludeVIAPostfixForDestination() {

        String exampleData = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                    {
                      "Id":1,"Line":"Eccles","TLAREF":"MEC","PIDREF":"MEC-TPID03","StationLocation":"MediaCityUK","AtcoCode":"9400ZZMAMCU2","Direction":"Incoming",
                      "Dest0":"Piccadilly Via Somewhere","Carriages0":"Single","Status0":"Due","Wait0":"1","Dest1":"","Carriages1":"","Status1":"","Wait1":"","Dest2":"","Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message A","Wait3":"","LastUpdated":"2017-11-29T11:45:00Z"
                    }]
                 }
                """;

        expectationByName(Piccadilly);

        LocalDateTime dateTimeA = LocalDateTime.of(2017, 11, 29, 11,45);

        TramStationDepartureInfo dep = setExpectationsForDeparture(1, OverheadDisplayLines.Eccles, LineDirection.Incoming, "message A", MediaCityUK, dateTimeA, "2", mediaCityPlatform);

        expectDueTram(dep, Piccadilly, 1, "Due", "Single", mediaCityPlatform);

        List<TramStationDepartureInfo> results = doParsing(exampleData);

        assertEquals(1, results.size());

        TramStationDepartureInfo result = results.getFirst();
        assertEquals("1", result.getDisplayId());

        List<UpcomingDeparture> allDue = result.getDueTrams();
        assertEquals(1, allDue.size());

        UpcomingDeparture due = allDue.getFirst();
        assertEquals(Piccadilly.getId(), due.getDestinationId());
    }

    @Test
    void shouldParseDeansgateWithMappingViaMCUK() {
        String exampleData = """
                {
                  "@odata.context":"https://opendataclientapi.azurewebsites.net/odata/$metadata#Metrolinks","value":[
                   {
                      "Id":234,"Line":"Airport","TLAREF":"AIR","PIDREF":"AIR-TPID01","StationLocation":"Manchester Airport","AtcoCode":"9400ZZMAAIR1","Direction":"Incoming",
                      "Dest0":"Deansgate Castlefield via MCUK","Carriages0":"Single","Status0":"Due","Wait0":"5",
                      "Dest1":"Deansgate Castlefield","Carriages1":"Single","Status1":"Due","Wait1":"17",
                      "Dest2":"","Carriages2":"","Status2":"","Wait2":"","Dest3":"","Carriages3":"","Status3":"","MessageBoard":"message B","Wait3":"","LastUpdated":"2017-06-29T13:55:00Z"
                    }]
                 }
                """;

        LocalDateTime dateTimeB = LocalDateTime.of(2017,6,29,13,55).plusHours(1);

        TramStationDepartureInfo dep = setExpectationsForDeparture(234, OverheadDisplayLines.Airport, LineDirection.Incoming, "message B", ManAirport, dateTimeB, "1", null);
        expectDueTram(dep, Deansgate, 5, "Due", "Single", null);
        expectDueTram(dep, Deansgate, 17, "Due", "Single", null);

        expectationByName(Deansgate);
        expectationByName(Piccadilly);

        List<TramStationDepartureInfo> results = doParsing(exampleData);

        TramStationDepartureInfo result = results.getFirst();
        assertEquals("234", result.getDisplayId());

        List<UpcomingDeparture> allDue = result.getDueTrams();
        assertEquals(2, allDue.size());

        UpcomingDeparture due1 = allDue.getFirst();
        assertEquals(Deansgate.getId(), due1.getDestinationId());
        UpcomingDeparture due2 = allDue.getFirst();
        assertEquals(Deansgate.getId(), due2.getDestinationId());
    }

    private List<TramStationDepartureInfo> doParsing(String data) {
        replayAll();
        parser.start();
        List<TramStationDepartureInfo> info = parser.parse(data);
        parser.stop();
        verifyAll();
        return info;
    }

    private TramStationDepartureInfo setExpectationsForDeparture(int displayId, OverheadDisplayLines line, LineDirection direction, String message,
                                                                 TramStations tramStation, LocalDateTime dateTime, String platformNumber, Platform platform) {

        TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(Integer.toString(displayId), line, direction, tramStation.fake(),
                message, dateTime, platform);
        EasyMock.expect(departureFactory.createStationDeparture(BigDecimal.valueOf(displayId), line, direction,
                        tramStation.getRawId()+platformNumber, message, dateTime)).
                andReturn(departureInfo);
        return departureInfo;
    }

    private void expectDueTram(TramStationDepartureInfo depA, TramStations dest, int waitInMinutes, String status, String carriages, Platform platform) {
        LocalDateTime updateDateTime = depA.getLastUpdate();
        TramTime tramTime = TramTime.ofHourMins(updateDateTime.toLocalTime()).plusMinutes(waitInMinutes);
        UpcomingDeparture upcomingDeparture = new UpcomingDeparture(updateDateTime.toLocalDate(), depA.getStation(), dest.fake(),
                status, tramTime, carriages, TestEnv.MetAgency(), TransportMode.Tram);
        if (platform!=null) {
            upcomingDeparture.setPlatform(platform);
        }

        EasyMock.expect(departureFactory.createDueTram(depA, status, dest.fake(), waitInMinutes, carriages)).
                andReturn(upcomingDeparture);
    }
}
