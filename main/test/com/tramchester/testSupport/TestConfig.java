package com.tramchester.testSupport;

import com.tramchester.config.AppConfiguration;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.geo.BoundingBox;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.server.ServerFactory;
import io.dropwizard.jetty.GzipHandlerFactory;
import io.dropwizard.util.DataSize;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import jakarta.validation.Valid;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public abstract class TestConfig extends AppConfiguration {

    // TODO Better way to handle server factory

    @Override
    public void setServerFactory(ServerFactory factory) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public ServerFactory getServerFactory() {
        DefaultServerFactory factory = new DefaultServerFactory();
        factory.setApplicationContextPath("/");
        factory.setAdminContextPath("/admin");
        factory.setJerseyRootPath("/api/*");

        // replicate real sever config
        final GzipHandlerFactory gzip = new GzipHandlerFactory();
        gzip.setSyncFlush(true);
        gzip.setEnabled(true);
        gzip.setBufferSize(DataSize.kilobytes(8));
        gzip.setMinimumEntitySize(DataSize.bytes(180));

        gzip.setExcludedPaths(Collections.singleton("/api/grid/chunked"));

        factory.setGzipFilterFactory(gzip);

        // disables request logging
//        LogbackAccessRequestLogFactory requestLogFactory = (LogbackAccessRequestLogFactory) factory.getRequestLogFactory();
//        requestLogFactory.setAppenders(Collections.emptyList());

        return factory;
    }

    @Override
    public List<GTFSSourceConfig> getGTFSDataSource() {
        return getDataSourceFORTESTING();
    }

    protected abstract List<GTFSSourceConfig> getDataSourceFORTESTING();

    @Override
    public boolean getChangeAtInterchangeOnly() { return true; }

    @Override
    public boolean getSendCloudWatchMetrics() {
        return false;
    }

    @Override
    public long GetCloudWatchMetricsFrequencyMinutes() {
        return 10;
    }

    @Override
    public Integer getStaticAssetCacheTimeSeconds() {
        return 5*60;
    }

    @Override
    public String getInstanceDataUrl() {
        return "";
    }

    @Override
    public Double getNearestStopRangeKM() {
        return 1.6D;
    }

    @Override
    public Double getNearestStopForWalkingRangeKM() {
        return 1.6D;
    }

    @Override
    public int getNumOfNearestStopsToOffer() {
        return 5;
    }

    @Override
    public int getNumOfNearestStopsForWalking() {
        return 3;
    }

    @Override
    public double getWalkingMPH() {
        return 3;
    }

    @Override
    public String getSecureHost() {
        return "tramchester.com";
    }

    @Override
    public int getMaxWait() {
        return 25;
    }

    // see RouteCalculatorTest.shouldFindEndOfLinesToEndOfLines
    // october 2024 closures 143 -> 155
    @Override
    public int getMaxJourneyDuration() {
        return 155;
//        return 143;
//        return 127;
    }

    @Override
    public int getNumberQueries() { return 3; }

    @Override
    public int getQueryInterval() { return 12; }

    @Override
    public int getRecentStopsToShow() {
        return 5;
    }

    @Override
    public int getMaxNumResults() {
        return 5;
    }

    @Override
    public long getCalcTimeoutMillis() {
        return 3500;
    }

    @Override
    public boolean getDepthFirst() {
        return true;
    }

    @Override
    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        SwaggerBundleConfiguration bundleConfiguration = new SwaggerBundleConfiguration();
        bundleConfiguration.setResourcePackage("com.tramchester.resources");
        return bundleConfiguration;
    }

    @Override
    public int getDataExpiryThreadhold() { return 3; }


    @Override
    public @Valid BoundingBox getBounds() {
        return TestEnv.getGreaterManchesterBounds();
    }

    @Override
    public TfgmTramLiveDataConfig getLiveDataConfig() {
        return null;
    }

    @Override
    public int getMaxWalkingConnections() {
        return 2;
    }

    @Override
    public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
        return Collections.emptyList();
    }

    @Override
    public Path getCacheFolder() {
        throw new RuntimeException("Must define");
    }

    @Override
    public boolean getPlanningEnabled() {
        return true;
    }

    @Override
    public boolean hasNeighbourConfig() {
        return false;
    }

    @Override
    public String getDistributionBucket() {
        return "tramchesternewdist";
    }

    @Override
    public boolean redirectToSecure() {
        return true;
    }

    @Override
    public String getEnvironmentName() {
        String place = System.getenv("PLACE");
        return place==null ? "" : place;
    }

    @Override
    public Integer getBuildNumber() {
        String build = System.getenv("BUILD");
        if (build==null) {
            return 0;
        }
        return Integer.parseInt(build);
    }
}
