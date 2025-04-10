package com.tramchester.testSupport.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.loader.TransportDataFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableNormalServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.domain.time.TramTime.ofHourMins;
import static com.tramchester.testSupport.reference.KnownLocations.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.INTERCHANGE;
import static java.lang.String.format;

@LazySingleton
public class TramTransportDataForTestFactory implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(TramTransportDataForTestFactory.class);

    private final TramTransportDataForTest container;

    private final DataSourceID dataSourceID = DataSourceID.tfgm;

    public final static TramDate startDate = TramDate.of(2014, 2, 10);
    public final static TramDate endDate = TramDate.of(2020, 8, 15);
    private static final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

    // TODO remove this workaround and make the startDate and endDate use current date(s)
    public final static TramDate routeDate = TestEnv.testDay();

    @Inject
    public TramTransportDataForTestFactory(ProvidesNow providesNow) {
        container = new TramTransportDataForTest(providesNow);
    }

    public static TramDate getValidDate() {
        TramDate validDate = startDate;
        while(validDate.getDayOfWeek()!=dayOfWeek) {
            validDate = validDate.plusDays(1);
        }
        return validDate;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateTestData(container);
        logger.debug(container.toString());
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stop");
        container.dispose();
        logger.info("stopped");
    }

    public TramTransportDataForTest getTestData() {
        return container;
    }

    public TransportData getData() {
        return getTestData();
    }

    private void populateTestData(final TransportDataContainer container) {
        final MutableAgency agency =  new MutableAgency(DataSourceID.tfgm, MutableAgency.METL, "Metrolink");


        final MutableRoute routeA = createTramRoute(getRed(routeDate));
        final MutableRoute routeB = createTramRoute(getPink(routeDate));
        final MutableRoute routeC = createTramRoute(getBlue(routeDate));
        final MutableRoute routeD = createTramRoute(getNavy(routeDate));

        agency.addRoute(routeA);
        agency.addRoute(routeB);
        agency.addRoute(routeC);
        agency.addRoute(routeD);

        container.addAgency(agency);

        logger.info("Add services");

        final MutableService serviceA = new MutableService(TramTransportDataForTest.serviceAId, dataSourceID);
        final MutableService serviceB = new MutableService(TramTransportDataForTest.serviceBId, dataSourceID);
        final MutableService serviceC = new MutableService(TramTransportDataForTest.serviceCId, dataSourceID);
        final MutableService serviceD = new MutableService(TramTransportDataForTest.serviceDId, dataSourceID);

        final MutableNormalServiceCalendar serviceCalendarA = new MutableNormalServiceCalendar(startDate, endDate, dayOfWeek);
        final MutableNormalServiceCalendar serviceCalendarB = new MutableNormalServiceCalendar(startDate, endDate, dayOfWeek);
        final MutableNormalServiceCalendar serviceCalendarC = new MutableNormalServiceCalendar(startDate, endDate, dayOfWeek);
        final MutableNormalServiceCalendar serviceCalendarD = new MutableNormalServiceCalendar(startDate, endDate, dayOfWeek);

        serviceA.setCalendar(serviceCalendarA);
        serviceB.setCalendar(serviceCalendarB);
        serviceC.setCalendar(serviceCalendarC);
        serviceD.setCalendar(serviceCalendarD);

        routeA.addService(serviceA);
        routeB.addService(serviceB);
        routeC.addService(serviceC);
        routeD.addService(serviceD);

        container.addRoute(routeA);
        container.addRoute(routeB);
        container.addRoute(routeC);
        container.addRoute(routeD);

        logger.info("Create trips and stops");

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        final MutableTrip tripA = new MutableTrip(Trip.createId(TramTransportDataForTest.TRIP_A_ID), "headSign",
                serviceA, routeA, Tram);
        serviceA.addTrip(tripA);

        final MutableStation first = createStation(TramTransportDataForTest.FIRST_STATION, NPTGLocality.createId("area1"), "startStation",
                nearAltrincham, dataSourceID, true);
        addAStation(container, first);
        addRouteStation(container, first, routeA);
        final PlatformStopCall stopA = createStop(container, tripA, first, of(7, 55), of(8, 0), 1);
        tripA.addStop(stopA);

        // trip Z, firstNameDup - for composite station testing
        final MutableTrip tripZ = new MutableTrip(Trip.createId("tripZ"), "for dup", serviceD, routeD, Tram);
        serviceD.addTrip(tripZ);
        final MutableStation firstDupName = createStation(TramTransportDataForTest.FIRST_STATION_DUP_NAME,
                NPTGLocality.createId("area1"), "startStation", nearAltrincham, dataSourceID, true);
        addAStation(container, firstDupName);
        addRouteStation(container, firstDupName, routeD);
        final PlatformStopCall stopZ = createStop(container, tripZ, firstDupName, of(12, 0), of(12, 0), 1);
        tripZ.addStop(stopZ);

        // trip Z, firstNameDup2 - for composite station testing
        final MutableStation firstDup2Name = createStation(TramTransportDataForTest.FIRST_STATION_DUP2_NAME,
                NPTGLocality.createId("area1"), "startStation", nearAltrincham, dataSourceID, true);
        addAStation(container, firstDup2Name);
        addRouteStation(container, firstDup2Name, routeD);
        final PlatformStopCall stopZZ = createStop(container, tripZ, firstDup2Name, of(12, 0), of(12, 0), 2);
        tripZ.addStop(stopZZ);

        routeD.addTrip(tripZ);

        final MutableStation second = createStation(TramTransportDataForTest.SECOND_STATION, NPTGLocality.createId("area1"), "secondStation",
                atRoundthornTram, dataSourceID, true);
        addAStation(container, second);
        addRouteStation(container, second, routeA);
        final PlatformStopCall stopB = createStop(container, tripA, second, of(8, 11), of(8, 11), 2);
        tripA.addStop(stopB);

        final MutableStation interchangeStation = createStation(INTERCHANGE, NPTGLocality.createId("area3"), "cornbrookStation",
                nearShudehill, dataSourceID, true);
        addAStation(container, interchangeStation);
        addRouteStation(container, interchangeStation, routeA);
        final PlatformStopCall stopC = createStop(container, tripA, interchangeStation, of(8, 20),
                of(8, 20), 3);
        tripA.addStop(stopC);

        final MutableStation last = createStation(TramTransportDataForTest.LAST_STATION, NPTGLocality.createId("area4"),
                "endStation", nearPiccGardens, dataSourceID, true);
        addAStation(container, last);
        addRouteStation(container, last, routeA);
        final PlatformStopCall stopD = createStop(container, tripA, last, of(8, 40), of(8, 40), 4);
        tripA.addStop(stopD);

        // service A
        routeA.addTrip(tripA);
        final MutableStation stationFour = createStation(TramTransportDataForTest.STATION_FOUR, NPTGLocality.createId("area4"),
                "Station4", nearKnutsfordBusStation, dataSourceID, true);
        addAStation(container, stationFour);

        // trip ZZ, fourthNameDup - for composite station testing
        final MutableTrip tripZZ = new MutableTrip(Trip.createId("tripZZ"), "for dup of 4", serviceA, routeD, Tram);
        serviceA.addTrip(tripZZ);
        final MutableStation fourDupName = createStation(TramTransportDataForTest.STATION_FOUR_DUP_NAME,
                NPTGLocality.createId("area4"), "Station4", nearKnutsfordBusStation, dataSourceID, true);
        addAStation(container, fourDupName);
        addRouteStation(container, fourDupName, routeD);
        final PlatformStopCall fourDupStop = createStop(container, tripZZ, fourDupName,
                of(13, 0), of(13, 0), 1);
        tripZZ.addStop(fourDupStop);

        routeD.addTrip(tripZZ);

        final MutableStation stationFive = createStation(TramTransportDataForTest.STATION_FIVE, NPTGLocality.createId("area5"),
                "Station5", nearStockportBus, dataSourceID, true);
        addAStation(container, stationFive);

        //
        final MutableTrip tripC = new MutableTrip(Trip.createId("tripCId"), "headSignC", serviceC, routeC, Tram);
        serviceC.addTrip(tripC);
        final PlatformStopCall stopG = createStop(container, tripC, interchangeStation, of(8, 26),
                of(8, 27), 1);
        addRouteStation(container, interchangeStation, routeC);
        final PlatformStopCall stopH = createStop(container, tripC, stationFive, of(8, 31),
                of(8, 33), 2);
        addRouteStation(container, stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        routeC.addTrip(tripC);

        // INTERCHANGE -> STATION_FOUR
        addRouteStation(container, stationFour, routeB);
        addRouteStation(container, interchangeStation, routeB);

        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 20), "tripB3Id");

        logger.info("Update container");

        container.addTrip(tripA);
        container.addTrip(tripC);

        container.addService(serviceA);
        container.addService(serviceB);
        container.addService(serviceC);

        container.reportNumbers();
    }

    private MutableStation createStation(final String station, IdFor<NPTGLocality> areaId, final String stationName, KnownLocations knownLocation,
                                         DataSourceID dataSourceID, final boolean isCentral) {
        return new MutableStation(Station.createId(station), areaId, stationName, knownLocation.latLong(), knownLocation.grid(),
                dataSourceID, isCentral);
    }

    private MutableRoute createTramRoute(final TestRoute knownRoute) {
        return new MutableRoute(knownRoute.getId(), knownRoute.shortName(), "route " + knownRoute.shortName(), TestEnv.MetAgency(),
                knownRoute.mode());
    }

    private void addAStation(TransportDataContainer container, MutableStation station) {
        container.addStation(station);
    }

    private static void addRouteStation(TransportDataContainer container, MutableStation station, Route route) {
        final RouteStation routeStation = new RouteStation(station, route);
        container.addRouteStation(routeStation);

        // set large span of times for these
        station.addRoutePickUp(route);
        station.addRoutePickUp(route);

        station.addRouteDropOff(route);
        station.addRouteDropOff(route);
    }

    private static void createInterchangeToStation4Trip(TransportDataContainer container, MutableRoute route, MutableService service,
                                                        MutableStation interchangeStation, MutableStation station, LocalTime startTime, String tripId) {
        final MutableTrip trip = new MutableTrip(Trip.createId(tripId), "headSignTripB2", service, route, Tram);
        final PlatformStopCall stop1 = createStop(container,trip, interchangeStation, ofHourMins(startTime),
                ofHourMins(startTime.plusMinutes(5)), 1);
        trip.addStop(stop1);
        final PlatformStopCall stop2 = createStop(container,trip, station, ofHourMins(startTime.plusMinutes(5)),
                ofHourMins(startTime.plusMinutes(8)), 2);
        trip.addStop(stop2);
        route.addTrip(trip);
        service.addTrip(trip);
        container.addTrip(trip);
    }

    private static PlatformStopCall createStop(TransportDataContainer container, MutableTrip trip,
                                               MutableStation station, TramTime arrivalTime, TramTime departureTime, int sequenceNum) {
        //String platformId = station.getId() + "1";
        final String platformName = format("%s platform 1", station.getName());

        final PlatformId platformId = PlatformId.createId(station, "1");
        final MutablePlatform platform = new MutablePlatform(platformId,
                station, platformName, station.getDataSourceID(), "1",
                station.getLocalityId(), station.getLatLong(), station.getGridPosition(), station.isMarkedInterchange());

        container.addPlatform(platform);
        station.addPlatform(platform);

        return new PlatformStopCall(platform, station, arrivalTime, departureTime, sequenceNum,
                GTFSPickupDropoffType.Regular, GTFSPickupDropoffType.Regular, trip);
    }

    public static class TramTransportDataForTest extends TransportDataContainer {

        private static final IdFor<Service> serviceAId = Service.createId("serviceAId");
        private static final IdFor<Service> serviceBId = Service.createId("serviceBId");
        private static final IdFor<Service> serviceCId = Service.createId("serviceCId");
        private static final IdFor<Service> serviceDId = Service.createId("serviceDId");

        private static final String METROLINK_PREFIX = "9400ZZ";

        public static final String TRIP_A_ID = "tripAId";
        public static final String FIRST_STATION = METROLINK_PREFIX + "FIRST";
        public static final String FIRST_STATION_DUP_NAME = METROLINK_PREFIX + "FIRSTDUP";
        public static final String FIRST_STATION_DUP2_NAME = METROLINK_PREFIX + "FIRSTDUP2";
        public static final String SECOND_STATION = METROLINK_PREFIX + "SECOND";
        public static final String LAST_STATION = METROLINK_PREFIX + "LAST";
        public static final String INTERCHANGE = TramStations.Cornbrook.getRawId();
        private static final String STATION_FOUR = METROLINK_PREFIX + "FOUR";
        private static final String STATION_FOUR_DUP_NAME = METROLINK_PREFIX + "FOURDUP";
        private static final String STATION_FIVE = METROLINK_PREFIX + "FIVE";

        public TramTransportDataForTest(ProvidesNow providesNow) {
            super(providesNow, "TramTransportDataForTest");
        }

        public Station getFirst() {
            return getStationById(Station.createId(FIRST_STATION));
        }

        public Station getFirstDupName() {
            return getStationById(Station.createId(FIRST_STATION_DUP_NAME));
        }

        public Station getFirstDup2Name() {
            return getStationById(Station.createId(FIRST_STATION_DUP2_NAME));
        }

        public Station getSecond() {
            return getStationById(Station.createId(SECOND_STATION));
        }

        public Station getInterchange() {
            return getStationById(Station.createId(INTERCHANGE));
        }

        public Station getLast() {
            return getStationById(Station.createId(LAST_STATION));
        }

        public Station getFifthStation() {
            return getStationById(Station.createId(STATION_FIVE));
        }

        public Station getFourthStation() {
            return getStationById(Station.createId(STATION_FOUR));
        }

        public Station getFourthStationDupName() {
            return getStationById(Station.createId(STATION_FOUR_DUP_NAME));
        }

        public Route getRouteA() {
            return getRouteById(getRed(routeDate).getId());
        }

        public Route getRouteB() {
            return getRouteById(getPink(routeDate).getId());
        }

    }
}
