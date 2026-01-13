package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.geo.BoundingBox;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class AppConfiguration extends TramchesterConfig {

    /////////
    //
    // Use Boxed types here to get @NotNull checking (i.e. Boolean not boolean)
    //
    ////////

    @NotNull
    private Integer staticAssetCacheTimeSeconds;

    @NotNull
    private String instanceDataUrl;

    @NotNull
    private Double nearestStopRangeKM;

    @NotNull
    private Double nearestStopForWalkingRangeKM;

    @NotNull
    private Integer numOfNearestStopsToOffer;

    @NotNull
    private Integer numOfNearestStopsForWalking;

    @NotNull
    private Double walkingMPH;

    @NotNull
    private String secureHost;

    @NotNull
    private Boolean redirectToSecure;

    @NotNull
    private Integer maxWait;

    @NotNull
    private Integer queryInterval;

    @NotNull
    private Integer recentStopsToShow;

    @Valid
    private SwaggerBundleConfiguration swagger;

    private Integer dataExpiryThreshold;
    
    @NotNull
    private Integer maxJourneyDuration;

    @NotNull
    private Boolean changeAtInterchangeOnly;

    @NotNull
    private Integer maxNumberResults;

    @NotNull
    private Integer numberQueries;

    @NotNull
    private Integer maxWalkingConnections;

    @Valid
    private List<GTFSSourceConfig> gtfsSourceConfig;

    @Valid
    private List<RemoteDataSourceConfig> remoteSources;

    //@NotNull
    private GraphDBConfig graphDBConfig;

    private TfgmTramLiveDataAppConfig tfgmTramliveData;

    private OpenLdbAppConfig openLdb;

    private OpenRailDataAppConfig openRailData;

    private RailAppConfig rail;

    @NotNull
    private BoundingBox bounds;

    @NotNull
    private Boolean sendCloudWatchMetrics;

    @NotNull
    private Path cacheFolder;

    @NotNull
    private Long calcTimeoutMillis;

    @NotNull
    private Boolean planningEnabled;

    @NotNull
    private Long cloudWatchMetricsFrequencyMinutes;

    @NotNull
    private String distributionBucket;

    private NeighbourAppConfig neighbourConfig;

    private Boolean cachingDisabled;

    @NotNull
    private String environmentName;

    @NotNull
    private Integer buildNumber;

    @NotNull
    private Boolean depthFirst;

    @NotNull
    private Boolean inMemoryGraph;

    ///  GETTERS

    @JsonProperty("instanceDataUrl")
    @Override
    public String getInstanceDataUrl() {
        return instanceDataUrl;
    }

    @JsonProperty("maxWait")
    @Override
    public int getMaxWait() {
        return maxWait;
    }

    @JsonProperty("maxNumberResults")
    @Override
    public int getMaxNumberResults() {
        return maxNumberResults;
    }

    @JsonProperty("numberQueries")
    @Override
    public int getNumberQueries() {
        return numberQueries;
    }

    @JsonProperty("queryInterval")
    @Override
    public int getQueryInterval() {
        return queryInterval;
    }

    @JsonProperty("recentStopsToShow")
    @Override
    public int getRecentStopsToShow() {
        return recentStopsToShow;
    }

    @JsonProperty("swagger")
    @Override
    public SwaggerBundleConfiguration getSwagger() {
        return swagger;
    }

    @JsonProperty("dataExpiryThreshold")
    @Override
    public int getDataExpiryThreshold() {
        return dataExpiryThreshold;
    }

    @JsonProperty("secureHost")
    @Override
    public String getSecureHost() {
        return secureHost;
    }

    @JsonProperty("redirectToSecure")
    @Override
    public boolean redirectToSecure() {
        return redirectToSecure;
    }

    @JsonProperty("cachingDisabled")
    @Override
    public boolean getCachingDisabled() {
        if (cachingDisabled==null) {
            return false;
        }
        return cachingDisabled;
    }

    @JsonProperty("inMemoryGraph")
    @Override
    public boolean getInMemoryGraph() {
        return inMemoryGraph;
    }

    @Override
    public boolean isGraphFiltered() {
        // support for test cases
        return false;
    }

    @JsonProperty("nearestStopRangeKM")
    @Override
    public Double getNearestStopRangeKM() {
        return nearestStopRangeKM;
    }

    @JsonProperty("nearestStopForWalkingRangeKM")
    @Override
    public Double getNearestStopForWalkingRangeKM() {
        return nearestStopForWalkingRangeKM;
    }

    @JsonProperty("numOfNearestStopsToOffer")
    @Override
    public int getNumOfNearestStopsToOffer() {
        return numOfNearestStopsToOffer;
    }

    @JsonProperty("numOfNearestStopsForWalking")
    @Override
    public int getNumOfNearestStopsForWalking() {
        return numOfNearestStopsForWalking;
    }

    @JsonProperty("walkingMPH")
    @Override
    public double getWalkingMPH() {
        return walkingMPH;
    }

    @JsonProperty("environmentName")
    @Override
    public String getEnvironmentName() {
        return environmentName;
    }

    @JsonProperty("buildNumber")
    @Override
    public Integer getBuildNumber() {
        return buildNumber;
    }

    @JsonProperty("staticAssetCacheTimeSeconds")
    @Override
    public Integer getStaticAssetCacheTimeSeconds() {
        return staticAssetCacheTimeSeconds;
    }

    @JsonProperty("changeAtInterchangeOnly")
    @Override
    public boolean getChangeAtInterchangeOnly() {
        return changeAtInterchangeOnly;
    }

    @JsonProperty("neighbourConfig")
    @Override
    public NeighbourConfig getNeighbourConfig() {
        return neighbourConfig;
    }

    @JsonProperty("maxJourneyDuration")
    @Override
    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    @JsonProperty("gtfsSourceConfig")
    @Valid
    @Override
    public List<GTFSSourceConfig> getGtfsSourceConfig() {
        return gtfsSourceConfig;
    }

    @JsonProperty("remoteSources")
    @Override
    public List<RemoteDataSourceConfig> getRemoteSources() {
        return remoteSources;
    }

    // optional
    @JsonProperty("tfgmTramliveData")
    @Override
    public TfgmTramLiveDataConfig getTfgmTramliveData() {
        return tfgmTramliveData;
    }

    @JsonProperty("openLdb")
    @Override
    public OpenLdbConfig getOpenLdb() {
        return openLdb;
    }

    @JsonProperty("openRailData")
    @Override
    public OpenRailDataConfig getOpenRailData() {
        return openRailData;
    }

    @JsonProperty("rail")
    @Override
    public RailConfig getRail() {
        return rail;
    }

    @JsonProperty("graphDBConfig")
    @Valid
    @Override
    public GraphDBConfig getGraphDBConfig() {
        return graphDBConfig;
    }

    @Valid
    @Override
    @JsonProperty("bounds")
    public BoundingBox getBounds() {
        return bounds;
    }

    @JsonProperty("maxWalkingConnections")
    @Override
    public int getMaxWalkingConnections() {
        return maxWalkingConnections;
    }

    @JsonProperty("sendCloudWatchMetrics")
    @Override
    public boolean getSendCloudWatchMetrics() {
        return sendCloudWatchMetrics;
    }

    @JsonProperty("depthFirst")
    @Override
    public boolean getDepthFirst() {
        return depthFirst;
    }

    @JsonProperty("cacheFolder")
    @Override
    public Path getCacheFolder() {
        return cacheFolder;
    }

    @JsonProperty("calcTimeoutMillis")
    @Override
    public long getCalcTimeoutMillis() {
        return calcTimeoutMillis;
    }

    @JsonProperty("cloudWatchMetricsFrequencyMinutes")
    @Override
    public long getCloudWatchMetricsFrequencyMinutes() {
        return cloudWatchMetricsFrequencyMinutes;
    }

    @JsonProperty("planningEnabled")
    @Override
    public boolean getPlanningEnabled() {
        return planningEnabled;
    }

    @Override
    public boolean hasNeighbourConfig() {
        return neighbourConfig!=null;
    }

    @JsonProperty("distributionBucket")
    @Override
    public String getDistributionBucket() {
        return distributionBucket;
    }

}
