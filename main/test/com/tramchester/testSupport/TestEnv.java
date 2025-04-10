package com.tramchester.testSupport;

import com.codahale.metrics.Gauge;
import com.tramchester.ComponentContainer;
import com.tramchester.caching.FileDataCache;
import com.tramchester.config.AppConfiguration;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEnv {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestEnv.class);

    public static final Path CACHE_DIR = Path.of("testData","cache");
    public static final String TEST_SNS_TOPIC_PREFIX = "TRAMCHESTER_TEST_TOPIC_";
    private static final String TEST_SQS_QUEUE = "TRAMCHESTER_TEST_QUEUE_";

    public static final String SERVER_URL_ENV_VAR = "SERVER_URL";
    public static final String DISABLE_HEADLESS_ENV_VAR = "DISABLE_HEADLESS";
    public static final String CHROMEDRIVER_PATH_ENV_VAR = "CHROMEDRIVER_PATH";

    public static final int NumberOfStationLinks = 202;

    private static final TramDate testDay;

    public static final DateTimeFormatter dateFormatDashes = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final Path LiveDataExampleFile = Paths.get("data","test","liveDataSample.json");
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:00");
    public static final String BRISTOL_BUSSTOP_OCTOCODE = "0100BRP90268";

    private static final Agency MET = MutableAgency.build(DataSourceID.tfgm, MutableAgency.METL, "Metrolink");

//    public static final Agency StagecoachManchester = MutableAgency.build(DataSourceID.tfgm, Agency.createId("7778462"),
//            "Stagecoach Manchester");

    public static final Agency BEE_A = MutableAgency.build(DataSourceID.tfgm, Agency.createId("7778550"),
            "Bee Network");

    public static final Agency WarringtonsOwnBuses = MutableAgency.build(DataSourceID.tfgm, Agency.createId("7778560"),
            "Warrington's Own Buses");

    public static final String TFGM_TIMETABLE_URL = "https://odata.tfgm.com/opendata/downloads/TfGMgtfsnew.zip";

    public final static HashSet<GTFSTransportationType> tramAndBus =
            new HashSet<>(Arrays.asList(GTFSTransportationType.tram, GTFSTransportationType.bus));

    public static AppConfiguration GET() {
        return new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return Collections.emptyList();
            }
        };
    }

//    public static final DateRange cornbrookClosed = new DateRange(TramDate.of(2024,10,5), TramDate.of(2024, 10,6));

    public static TramchesterConfig GET(TfgmTramLiveDataConfig testLiveDataConfig) {
        return new TestConfig() {
            @Override
            protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
                return null;
            }

            @Override
            public TfgmTramLiveDataConfig getLiveDataConfig() {
                return testLiveDataConfig;
            }
        };
    }

    public static LocalDateTime LocalNow() {
        return LocalDateTime.now(TestConfig.TimeZoneId);
    }


    public static ZonedDateTime UTCNow() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    static {
        TramDate today = TramDate.from(LocalNow());
        testDay = UpcomingDates.getNextDate(DayOfWeek.THURSDAY, today);
    }

    public static TramDate testDay() {
        return testDay;
    }

    public static Route getTramTestRoute() {
        return getTramTestRoute(Route.createId("RouteId"), "routeName");
    }

    public static Route getTramTestRoute(IdFor<Route> routeId, String routeName) {
        return MutableRoute.getRoute(routeId, "routeCode", routeName, TestEnv.MetAgency(), Tram);
    }

    public static Route getTrainTestRoute(IdFor<Route> routeId, String routeName) {
        return MutableRoute.getRoute(routeId, "routeCode", routeName, TestEnv.NorthernTrainsAgency(), Train);
    }

    public static Agency MetAgency() {
        return MET;
    }

    public static Agency NorthernTrainsAgency() {
        return MutableAgency.build(DataSourceID.openRailData, TrainOperatingCompanies.NT.getAgencyId(), TrainOperatingCompanies.NT.getCompanyName());
    }

    // useful for diagnosing issues in windows env with spaces in paths etc.......
    public static Path getPathFromEnv(String envVarName) {
        String value = System.getenv(envVarName);
        if (value==null) {
            logger.warn(format("Environmental Variable %s not set", envVarName));
            return null;
        }
        Path path = Paths.get(value).toAbsolutePath();
        if (Files.exists(path)) {
            logger.info(format("Env var %s set to '%s' resulting in path '%s'", envVarName, value, path));
        }
        else {
            logger.error(format("Env var %s set to '%s' resulting in MISSING path '%s'", envVarName, value, path));
        }
        if (Files.isDirectory(path)) {
            logger.error(format("Env var %s set to '%s' resulting in DIRECTORY path '%s'", envVarName, value, path));
            return null;
        }
        return path;
    }

    public static boolean isCircleci() {
        return System.getenv("CIRCLECI") != null;
    }

    public static PlatformStopCall createTramStopCall(Trip trip, String stopCode, TramStations tramStation, int seq, TramTime arrive,
                                                      TramTime depart) {
        final Station station = tramStation.fake();

        PlatformId platformId = TransportEntityFactoryForTFGM.createPlatformId(station.getId(), stopCode);
        Platform platform = MutablePlatform.buildForTFGMTram(platformId, station, tramStation.getLatLong(),
                DataSourceID.unknown, NPTGLocality.InvalidId());
        GTFSPickupDropoffType pickupDropoff = GTFSPickupDropoffType.Regular;

        return new PlatformStopCall(platform, station, arrive, depart, seq, pickupDropoff, pickupDropoff, trip);
    }

    public static BoundingBox getGreaterManchesterBounds() {
        return new BoundingBox(333200, 373130, 414500, 437850);
    }

    public static BoundingBox getNationalTrainBounds() {
        return new BoundingBox(147588, 30599, 654747, 967921);
    }

    public static BoundingBox getGreaterManchester() { return new BoundingBox(370000, 380000,
            404903, 414500); }

    public static CacheMetrics.RegistersCacheMetrics NoopRegisterMetrics() {
        return new CacheMetrics.RegistersCacheMetrics() {
            @Override
            public <T> void register(String metricName, Gauge<T> Gauge) {
                // noop
            }
        };
    }

    public static EnumSet<DayOfWeek> allDays() {
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    public static void deleteDBIfPresent(final TramchesterConfig config) throws IOException {
        final Path dbPath = config.getGraphDBConfig().getDbPath();
        if (Files.exists(dbPath)) {
            logger.warn("deleting database file: " + dbPath.toAbsolutePath());
            FileUtils.deleteDirectory(dbPath.toFile());
        }
    }

    public static void assertLatLongEquals(LatLong expected, LatLong actual, double delta, String message) {
        assertEquals(expected.getLat(), actual.getLat(), delta, "lat:" + message);
        assertEquals(expected.getLon(), actual.getLon(), delta, "lon: " +message);
    }

    public static void assertMinutesEquals(int minutes, Duration duration) {
        assertEquals(Duration.ofMinutes(minutes), duration, "Duration %s did match %d minutes".formatted(duration, minutes));
    }

    public static void assertMinutesRoundedEquals(Duration durationA, Duration durationB) {
        assertEquals(roundToMinutes(durationA), roundToMinutes(durationB), "Duration %s did match %s round to mins".formatted(durationA, durationB));
    }

    private static long roundToMinutes(Duration duration) {
        final double minutesExact = duration.toSeconds() / 60D;
        return Math.round(minutesExact);
    }

    @Deprecated
    public static void assertMinutesEquals(int minutes, Duration duration, String message) {
        assertEquals(Duration.ofMinutes(minutes), duration, message);
    }

    public static void clearDataCache(ComponentContainer componentContainer) {
        FileDataCache cache = componentContainer.get(FileDataCache.class);
        logger.warn("Clearing cache");
        cache.clearFiles();
    }

    public static Duration calcCostInMinutes(Location<?> locationA, Location<?> locationB, final double mph) {
        final double distanceInMiles = distanceInMiles(locationA.getLatLong(), locationB.getLatLong());
        final double hours = distanceInMiles / mph;
        final double seconds = hours * 3600D;
        return Duration.ofSeconds((long) Math.ceil(seconds));
    }

    private static double distanceInMiles(final LatLong point1, final LatLong point2) {

        final double EARTH_RADIUS = 3958.75;

        final double lat1 = point1.getLat();
        final double lat2 = point2.getLat();
        final double diffLat = Math.toRadians(lat2-lat1);
        final double diffLong = Math.toRadians(point2.getLon()-point1.getLon());
        final double sineDiffLat = Math.sin(diffLat / 2D);
        final double sineDiffLong = Math.sin(diffLong / 2D);

        final double a = Math.pow(sineDiffLat, 2) + Math.pow(sineDiffLong, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        final double fractionOfRadius = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * fractionOfRadius;
    }

    public static Platform createPlatformFor(Station station, String platformNumber) {
        PlatformId platformId = PlatformId.createId(station.getId(), platformNumber);
        return MutablePlatform.buildForTFGMTram(platformId, station, station.getLatLong(),
                station.getDataSourceID(), station.getLocalityId());
    }

    public static Platform findOnlyPlatform(Station station) {
        if (!station.hasPlatforms()) {
            throw new RuntimeException("No platforms");
        }
        List<Platform> platforms = new ArrayList<>(station.getPlatforms());
        if (platforms.size()!=1) {
            throw new RuntimeException("Wrong number of platforms " + platforms.size());
        }
        return platforms.get(0);
    }

    public static Path getTempDir() {
        String tempDir=System.getProperty("java.io.tmpdir");
        return Path.of(tempDir);
    }

    public static <T> void assertSetEquals(Set<T> itemsA, Set<T> itemsB) {
        SetUtils.SetView<T> difference = SetUtils.disjunction(itemsA, itemsB);
        Set<T> inBnotA = new HashSet<>(itemsA);
        inBnotA.removeAll(itemsB);
        Set<T> inAnotB = new HashSet<>(itemsB);
        inAnotB.removeAll(itemsA);
        String message = "Different A:" + itemsA.size() + " B:" + itemsB.size() + " " + difference +  " in A but not B " +
                inAnotB + " in B but not A " + inBnotA;
        assertTrue(difference.isEmpty(), message);
    }

    public static String getBucketUrl() {
        return "s3://tramchesternewdist/";
    }

    public static String getDatabaseRemoteURL() {
        String releaseNumber = System.getenv("RELEASE_NUMBER");
        if (releaseNumber==null) {
            releaseNumber="0";
        }
        // s3://tramchesternewdist/dist/0/database.zip
        return String.format("%sdist/%s/database.zip", getBucketUrl(), releaseNumber);

    }

    public static String getTestQueueName() {
        if (isCircleci()) {
            return TEST_SQS_QUEUE+"CI";
        }
        return TEST_SQS_QUEUE+ getEnv();
    }

    private static String getEnv() {
        String text = System.getenv("PLACE");
        if (text==null) {
            return "Dev";
        }
        return text;
    }

    @Deprecated
    public static TramDate nextSunday() {
        return UpcomingDates.nextSunday();
    }

    @Deprecated
    public static TramDate nextSaturday() {
        return UpcomingDates.nextSaturday();
    }

    @Deprecated
    public static Collection<TramDate> daysAhead() {
        return UpcomingDates.daysAhead();
    }

    @Deprecated
    public static boolean UpcomingClosures(Station station, TramDate date) {
        return UpcomingDates.hasClosure(station,date);
    }

    @Deprecated
    public static Stream<TramDate> getUpcomingDates() {
        return UpcomingDates.getUpcomingDates();
    }

    public static TramDate nextMonday() {
        return UpcomingDates.nextMonday();
    }

    public static class Modes {

        public static final EnumSet<TransportMode> TramsOnly = EnumSet.of(Tram);
        public static final EnumSet<TransportMode> BusesOnly = EnumSet.of(Bus);
        public static final EnumSet<TransportMode> RailOnly = EnumSet.of(Train);
        public static final EnumSet<TransportMode> TrainAndTram = EnumSet.of(Train, Tram, RailReplacementBus);
    }
}
