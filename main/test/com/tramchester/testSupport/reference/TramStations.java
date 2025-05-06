package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.*;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum TramStations implements FakeStation, HasId<Station> {

    Altrincham("9400ZZMAALT", "Altrincham", pos(53.38726, -2.34755)),
    Ashton("9400ZZMAAUL", "Ashton-Under-Lyne", pos(53.49035, -2.09798)),
    Brooklands("9400ZZMABKS", "Brooklands", pos(53.41704,-2.32599)),
    ManAirport("9400ZZMAAIR", "Manchester Airport", pos(53.36541, -2.27222)),
    TraffordBar("9400ZZMATRA", "Trafford Bar", pos(53.46163, -2.27762)),
    VeloPark("9400ZZMAVPK", "Velopark", pos(53.48224, -2.1933)),
    Cornbrook("9400ZZMACRN", "Cornbrook", pos(53.46996, -2.26768)),
    Etihad("9400ZZMAECS", "Etihad Campus", pos(53.48535, -2.20233565746)),
    Piccadilly("9400ZZMAPIC", "Piccadilly", pos(53.47731, -2.23121)),
    HoltTown("9400ZZMAHTN", "Holt Town", pos(53.4832, -2.21228)),
    Bury("9400ZZMABUR", "Bury", pos(53.59082, -2.29726)),
    EastDidsbury("9400ZZMAEDY", "East Didsbury", pos(53.41208, -2.21739)),
    Pomona("9400ZZMAPOM", "Pomona", pos(53.46521, -2.27791)),
    Deansgate("9400ZZMAGMX", "Deansgate-Castlefield", pos(53.47476, -2.25018)),
    Broadway("9400ZZMABWY", "Broadway", pos(53.47478, -2.29506)),
    PiccadillyGardens("9400ZZMAPGD", "Piccadilly Gardens", pos(53.48029, -2.23705)),
    ExchangeSquare("9400ZZMAEXS", "Exchange Square", pos(53.48439, -2.2427)),
    Victoria("9400ZZMAVIC", "Victoria", pos(53.48787, -2.24187)),
    NavigationRoad("9400ZZMANAV", "Navigation Road", pos(53.39589, -2.34331)),
    HarbourCity("9400ZZMAHCY", "Harbour City", pos(53.47401, -2.29156264773)),
    StPetersSquare("9400ZZMASTP", "St Peter's Square", pos(53.47825, -2.24314)),
    MarketStreet("9400ZZMAMKT", "Market Street", pos(53.48192, -2.23883)),
    MediaCityUK("9400ZZMAMCU", "MediaCityUK", pos(53.47214, -2.29733)),
    StWerburghsRoad("9400ZZMASTW", "St Werburgh's Road", pos(53.4387, -2.26547)),
    Chorlton("9400ZZMACHO", "Chorlton", pos(53.44262, -2.27335) ),
    Shudehill("9400ZZMASHU", "Shudehill", pos(53.48524, -2.23918)),
    Monsall("9400ZZMAMON", "Monsall", pos(53.50111, -2.21061)),
    ExchangeQuay("9400ZZMAEXC", "Exchange Quay", pos(53.46769, -2.28242)),
    SalfordQuay("9400ZZMASQY", "Salford Quays", pos(53.4703, -2.28407)),
    Anchorage("9400ZZMAANC", "Anchorage", pos(53.47425, -2.28607)),
    HeatonPark("9400ZZMAHEA", "Heaton Park", pos(53.53036, -2.26699)),
    BurtonRoad("9400ZZMABNR", "Burton Road", pos(53.42908, -2.24064)),
    OldTrafford("9400ZZMAOLD", "Old Trafford", pos(53.45634, -2.28496)),
    Wharfside("9400ZZMAWFS", "Wharfside", pos(53.46625, -2.28748)),
    PeelHall("9400ZZMAPLL", "Peel Hall", pos(53.37373, -2.25038)),
    TraffordCentre("9400ZZMATRC", "The Trafford Centre", pos(53.46782, -2.34751)),
    ImperialWarMuseum("9400ZZMAIWM", "Imperial War Museum", pos(53.46862272157,-2.29682786715)),
    Eccles("9400ZZMAECC", "Eccles", pos(53.48307, -2.33454)),
    NewIslington("9400ZZMANIS", "New Islington", pos(53.48108550908, -2.21985483562)),
    Timperley("9400ZZMATIM", "Timperley", pos(53.40429833013,-2.33826968737)),
    Whitefield("9400ZZMAWFD", "Whitefield", pos(53.55113165424,-2.2951414371)),
    Langworthy("9400ZZMALWY", "Langworthy", pos(53.48061,-2.29618)),
    Weaste("9400ZZMAWST", "Weaste", pos(53.48236,-2.30736)),
    Ladywell("9400ZZMALDY", "Ladywell", pos(53.48404,-2.32683)),
    OldhamMumps("9400ZZMAOMP", "Oldham Mumps", pos(53.54245415127,-2.10345025085)),
    OldhamCentral("9400ZZMAOMC", "Oldham Central", pos(53.54033404153,-2.11213659253)),
    OldhamKingStreet("9400ZZMAOKS", "Oldham King Street", pos(53.54033404153,-2.11746120969)),
    RochdaleRail("9400ZZMARRS", "Rochdale Railway Station", pos(53.61102, -2.15449)),
    Rochdale("9400ZZMARIN", "Rochdale Town Centre", pos(53.61736, -2.15509)),
    Crumpsal("9400ZZMACRU", "Crumpsall", pos(53.51716010436,-2.24104993052)),
    SaleWaterPark("9400ZZMASWP", "Sale Water Park", pos(53.428243,-2.290767)),
    ShawAndCrompton("9400ZZMASHA", "Shaw and Crompton", pos(53.5763, -2.08963));

    private final String rawId;
    private final String name;
    private final LatLong latlong;

    TramStations(String rawId, String name, LatLong latlong) {
        this.rawId = rawId;
        this.name = name;
        this.latlong = latlong;
    }

    private static final Set<TramStations> EndOfTheLine = new HashSet<>(Arrays.asList(Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury,
            Ashton,
            Rochdale,
            Bury,
            TraffordCentre
            //MediaCityUK
    ));

    public static Set<TramStations> getEndOfTheLine() {
        return EndOfTheLine;
    }

    public static boolean isEndOfLine(final Station station) {
        return containedIn(station.getId(), getEndOfTheLine());
    }

    private static boolean containedIn(final IdFor<Station> stationId, final Set<TramStations> theSet) {
        final IdSet<Station> ids = theSet.stream().map(TramStations::getId).collect(IdSet.idCollector());
        return ids.contains(stationId);
    }

    private static LatLong pos(final double lat, final double lon) {
        return new LatLong(lat, lon);
    }

    public static IdSet<Station> ids(TramStations... values) {
        return Stream.of(values).map(FakeStation::getId).collect(IdSet.idCollector());
    }

    @Override
    public String getRawId() {
        return rawId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latlong;
    }

    @NotNull
    private MutableStation createMutable() {
        final GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
        final MutableStation mutableStation = new FakeTramStaton(getId(), NPTGLocality.InvalidId(), name, latlong, grid, DataSourceID.tfgm, true);
        mutableStation.addMode(TransportMode.Tram);
        return mutableStation;
    }

    @Override
    public Station fake() {
        return faker().build();
    }

    public Station fakeWithPlatform(final int platformNumber, TramDate date) {
        return faker().platform(platformNumber, date).build();
    }

    public Station fake(final TestRoute knownTramRoute) {
        return faker().dropOff(knownTramRoute).build();
    }

    public FakeStationBuilder faker() {
        return new FakeStationBuilder(this);
    }

    public static class FakeStationBuilder {
        private final Map<Integer, TestRoute> fakeDropOffPlatforms;
        private final Set<TestRoute> fakeRoutes;
        private final TramStations tramStation;

        public FakeStationBuilder(final TramStations tramStation) {
            this.tramStation = tramStation;
            fakeDropOffPlatforms = new HashMap<>();
            fakeRoutes = new HashSet<>();
        }

        public FakeStationBuilder dropOff(final TestRoute knownTramRoute) {
            fakeRoutes.add(knownTramRoute);
            return this;
        }

        public FakeStationBuilder platform(final int platformNumber, TramDate date) {
            fakeDropOffPlatforms.put(platformNumber, KnownTramRoute.getGreen(date));
            return this;
        }

        public FakeStationBuilder dropOffPlatform(final int platformNumber, final TestRoute route) {
            if (fakeDropOffPlatforms.containsKey(platformNumber)) {
                throw new RuntimeException("Platform " + platformNumber + " already seen for route " + fakeDropOffPlatforms.get(platformNumber));
            }
            fakeDropOffPlatforms.put(platformNumber, route);
            return this;
        }

        public Station build() {
            final MutableStation station = tramStation.createMutable();

            final Set<Route> routes = fakeRoutes.stream().map(TestRoute::fake).collect(Collectors.toSet());
            final Set<Platform> platforms = fakeDropOffPlatforms.entrySet().stream().
                    map(entry -> createPlatform(station, entry.getKey()).addRouteDropOff(entry.getValue().fake())).
                    collect(Collectors.toSet());

            platforms.forEach(station::addPlatform);
            routes.forEach(station::addRouteDropOff);

            return station;
        }

        private MutablePlatform createPlatform(final Station station, Integer platformNumber) {
            final PlatformId platformId = PlatformId.createId(station, platformNumber.toString());
            return MutablePlatform.buildForTFGMTram(platformId, station,
                    station.getLatLong(), station.getDataSourceID(), station.getLocalityId());
        }

    }

    public class FakeTramStaton extends MutableStation {

        public FakeTramStaton(IdFor<Station> id, IdFor<NPTGLocality> localityId, String stationName, LatLong latLong,
                              GridPosition gridPosition, DataSourceID dataSourceID, boolean isCentral) {
            super(id, localityId, stationName, latLong, gridPosition, dataSourceID, isCentral);
        }
    }
}
