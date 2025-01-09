package com.tramchester.config;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.MarginInMeters;
import io.dropwizard.core.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static tech.units.indriya.unit.Units.METRE_PER_SECOND;
import static tech.units.indriya.unit.Units.SECOND;

public abstract class TramchesterConfig extends Configuration implements HasRemoteDataSourceConfig, HasGraphDBConfig {

    public static final ZoneId TimeZoneId = ZoneId.of("Europe/London");

    public static final String DateFormatForJson = "yyyy-MM-dd";

    public static final String DateTimeFormatForJson = "yyyy-MM-dd'T'HH:mm:ss";

    public final static double KILO_PER_MILE = 1.609344D;

    private final Map<DataSourceID, TransportDataSourceConfig> dataSources;

    protected TramchesterConfig() {
        dataSources = new HashMap<>();
    }

    public abstract String getEnvironmentName() ;

    public abstract Integer getBuildNumber();

    public abstract Integer getStaticAssetCacheTimeSeconds();

    // URL to pull Cloud instance meta-data from
    public abstract String getInstanceDataUrl();

    // range to scan for nearby stations to display
    public abstract Double getNearestStopRangeKM();

    // range to scan for nearby stations when routing walking to/from
    protected abstract Double getNearestStopForWalkingRangeKM();

    // range to scan for nearby stations when routing walking to/from
    public MarginInMeters getWalkingDistanceRange() {
        return MarginInMeters.ofKM(getNearestStopForWalkingRangeKM());
    }

    // limit on number of near stops to display front-end
    public abstract int getNumOfNearestStopsToOffer();

    // limit on number of near stops to consider when walking to/from a station
    public abstract int getNumOfNearestStopsForWalking();

    // an assumed mph for walking
    public abstract double getWalkingMPH();

    // the secure host, the one the certificate matches
    public abstract String getSecureHost();

    // max time to wait for tram/connection
    public abstract int getMaxWait();

    // max number of results to return via the API
    public abstract int getMaxNumResults();

    // number of queries to do for each given time, spaced by QueryInterval below
    public abstract int getNumberQueries();

    // Interval between queryies i.e. every 6 minutes
    public abstract int getQueryInterval();

    // how many stops show in Recent on the stations dropdowns
    public abstract int getRecentStopsToShow();

    // maximum length of a journey in minutes
    public abstract int getMaxJourneyDuration();

    public abstract SwaggerBundleConfiguration getSwaggerBundleConfiguration();

    // number of days before data expiry to start warning
    public abstract int getDataExpiryThreadhold();

    // remove the (Purple Line) part of the route name?
    //public abstract boolean getRemoveRouteNameSuffix();

    // only allow changing vehicles at interchanges
    public abstract boolean getChangeAtInterchangeOnly();

    // neighbours config
    public abstract NeighbourConfig getNeighbourConfig();

    // config for each of the GTFS data sources
    public abstract List<GTFSSourceConfig> getGTFSDataSource();

    // config for each remote data source to be downloaded
    public abstract List<RemoteDataSourceConfig> getRemoteDataSourceConfig();

    // live transport data config
    public abstract TfgmTramLiveDataConfig getLiveDataConfig();

    //  Open Live Departure Boards Web Service config
    public abstract OpenLdbConfig getOpenldbwsConfig();

    // config for auth against open rail data download
    public abstract OpenRailDataConfig getOpenRailDataConfig();

    // rail data
    public abstract RailConfig getRailConfig();

    // Graph DB Config
    public abstract GraphDBConfig getGraphDBConfig();

    // bounding box for stations to include
    public abstract BoundingBox getBounds();

    public EnumSet<TransportMode> getTransportModes() {
        final Set<TransportMode> modes = getGTFSDataSource().stream().
                map(GTFSSourceConfig::getTransportModes).
                flatMap(Collection::stream).
                collect(Collectors.toSet());

        final RailConfig railConfig = getRailConfig();
        if (railConfig!=null) {
            modes.add(Train);
            modes.add(RailReplacementBus);
        }

        return modes.isEmpty() ? EnumSet.noneOf(TransportMode.class) : EnumSet.copyOf(modes);
    }

    public RemoteDataSourceConfig getDataRemoteSourceConfig(DataSourceID dataSourceID) {
        return getRemoteDataSourceConfig().stream().
                filter(config -> config.getDataSourceId()==dataSourceID).
                findFirst().orElseThrow();
    }

    public boolean hasRemoteDataSourceConfig(DataSourceID dataSourceID) {
        return getRemoteDataSourceConfig().stream().anyMatch(config -> config.getDataSourceId()==dataSourceID);
    }

    // number of connections to make by walking
    public abstract int getMaxWalkingConnections();

    public abstract boolean getSendCloudWatchMetrics();

    public boolean liveTfgmTramDataEnabled() {
        return getLiveDataConfig()!=null;
    }

    public boolean liveTrainDataEnabled() {
        return getOpenldbwsConfig()!=null;
    }

    public abstract boolean getDepthFirst();

    public abstract Path getCacheFolder();

    public abstract long getCalcTimeoutMillis();

    public abstract long GetCloudWatchMetricsFrequencyMinutes();

    public abstract boolean getPlanningEnabled();

    public abstract boolean hasNeighbourConfig();

    public abstract String getDistributionBucket();

    public abstract boolean redirectToSecure();

    public abstract boolean getCachingDisabled();

    public boolean hasRailConfig() {
        return getRailConfig()!=null;
    }

    public boolean onlyMarkedInterchange(Station station) {
        DataSourceID sourceId = station.getDataSourceID();
        TransportDataSourceConfig sourceConfig = getGetSourceConfigFor(sourceId);
        return sourceConfig.getOnlyMarkedInterchanges();
    }

    private TransportDataSourceConfig getGetSourceConfigFor(DataSourceID sourceId) {
        populateDataSourceMap();
        return dataSources.get(sourceId);
    }

    private void populateDataSourceMap() {
        if (dataSources.isEmpty()) {
            final List<GTFSSourceConfig> gtfsSources = getGTFSDataSource();
            gtfsSources.forEach(gtfsSource -> dataSources.put(gtfsSource.getDataSourceId(), gtfsSource));
            final RailConfig railConfig = getRailConfig();
            if (railConfig != null) {
                dataSources.put(railConfig.getDataSourceId(), railConfig);
            }
        }
    }

    public Duration getInitialMaxWaitFor(final DataSourceID sourceId) {
        final TransportDataSourceConfig sourceConfig = getGetSourceConfigFor(sourceId);
        return sourceConfig.getMaxInitialWait();
    }

    public String getLiveDataSNSPublishTopic() {
        if (liveTfgmTramDataEnabled()) {
            TfgmTramLiveDataConfig liveDataConfig = getLiveDataConfig();
            String topic = liveDataConfig.getSnsTopicPublishPrefix();
            if (topic==null) {
                return "";
            }
            if (topic.isEmpty()) {
                return "";
            }
            return topic + getEnvironmentName();
        }
        return "";
    }

    public abstract boolean isGraphFiltered();

    public ComparableQuantity<Speed> getWalkingSpeed() {
        final double kilometers = getWalkingMPH() * KILO_PER_MILE;
        final double metersPerSecond = (kilometers * 1000) / 3600D;
        return Quantities.getQuantity(metersPerSecond, METRE_PER_SECOND);
    }

    public Duration getWalkingDuration() {
        final ComparableQuantity<Speed> speed = getWalkingSpeed();
        final ComparableQuantity<Length> distance = getWalkingDistanceRange().getDistance();

        final ComparableQuantity<Time> result = distance.divide(speed, Time.class);

        Number seconds = result.to(SECOND).getValue();
        return Duration.ofSeconds(seconds.longValue());
    }

    public boolean inProdEnv() {
        // TODO env name into an enum
        return getEnvironmentName().startsWith("Prod");
    }


}
