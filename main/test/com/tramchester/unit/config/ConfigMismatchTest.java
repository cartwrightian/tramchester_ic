package com.tramchester.unit.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.config.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationIdPair;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import io.dropwizard.configuration.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMismatchTest {

    //   - name: postcode
    //    dataURL: https://api.os.uk/downloads/v1/products/CodePointOpen/downloads?area=GB&format=CSV&redirect
    //    dataCheckURL: ""
    //    dataPath: data/postcodes
    //    filename: codepo_gb.zip
    //    defaultExpiryDays: 180

    enum Category {
        Closures,
        Modes,
        Bounds;

        public boolean not(Collection<Category> excluded) {
            return !excluded.contains(this);
        }
    }

    @Test
    void shouldBeAbleToLoadAllConfigWithoutExceptions() throws IOException, ConfigurationException {
        // Note: this does not catch all the same validation cases as app start up
        Set<Path> configFiles = getConfigFiles();

        for (Path config : configFiles) {
            StandaloneConfigLoader.LoadConfigFromFile(config);
        }
    }

    @Test
    void shouldHaveValidIdsForRemoteConfigSections() throws IOException, ConfigurationException {
        Set<Path> configFiles = getConfigFiles();

        for(Path config : configFiles) {
            AppConfiguration configuration = StandaloneConfigLoader.LoadConfigFromFile(config);
            List<RemoteDataSourceConfig> remoteSourceConfigs = configuration.getRemoteDataSourceConfig();
            for(RemoteDataSourceConfig remoteSourceConfig : remoteSourceConfigs) {
                final DataSourceID dataSourceID = DataSourceID.findOrUnknown(remoteSourceConfig.getName());
                assertNotEquals(DataSourceID.unknown, dataSourceID,
                        "Bad source id for " + remoteSourceConfig.getName() + " in " + config.toAbsolutePath());

            }
        }
    }

    @Test
    void shouldNotUsePrimitiveTypesInAppConfigAsDisablesNullChecking() {
        Field[] fields = AppConfiguration.class.getDeclaredFields();
        Set<String> nonPrim = Arrays.stream(fields).
                filter(field -> field.getAnnotation(JsonProperty.class) != null).
                filter(field -> field.getType().isPrimitive()).
                map(Field::getName).
                collect(Collectors.toSet());
        assertEquals(Collections.emptySet(), nonPrim);
    }

    @Test
    void shouldHaveKeyParametersSameForTramIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.Enabled);

        validateCoreParameters(Collections.emptySet(), appConfig, testConfig);

        List<RemoteDataSourceConfig> dataSourceConfig = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testDataSourceConfig = testConfig.getRemoteDataSourceConfig();
        assertEquals(dataSourceConfig.size(), testDataSourceConfig.size());
        assertEquals(2, dataSourceConfig.size());

        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.tfgm);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.database);
    }

    @Test
    void shouldHaveKeyParametersSameForBusIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("buses.yml");
        IntegrationBusTestConfig testConfig = new IntegrationBusTestConfig();

        validateCoreParameters(Collections.emptySet(), appConfig, testConfig);
    }

    @Test
    void shouldHaveKeyParametersSameForRailIntegrationTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("rail.yml");
        IntegrationRailTestConfig testConfig = new IntegrationRailTestConfig();

        validateCoreParameters(Collections.emptySet(), appConfig, testConfig);

        List<RemoteDataSourceConfig> remoteSources = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testRemoteSources = testConfig.getRemoteDataSourceConfig();

        assertEquals(remoteSources.size(), testRemoteSources.size());
        assertEquals(3, testRemoteSources.size());

        assertRemoteSources(remoteSources, testRemoteSources, DataSourceID.rail);
        assertRemoteSources(remoteSources, testRemoteSources, DataSourceID.naptanxml);
        assertRemoteSources(remoteSources, testRemoteSources, DataSourceID.nptg);

        RailConfig rail = appConfig.getRailConfig();
        RailConfig testRail = appConfig.getRailConfig();

        assertEquals(rail.getStations(), testRail.getStations());
        assertEquals(rail.getTimetable(), testRail.getTimetable());
        assertEquals(rail.getModes(), testRail.getModes());

        assertRailLiveData(appConfig.getOpenldbwsConfig(), testConfig.getOpenldbwsConfig());

        checkDataSourceConfig(appConfig.getRailConfig(), testConfig.getRailConfig());

        checkRailDataVersionFor(appConfig);
        checkRailDataVersionFor(testConfig);
    }

    @Test
    void shouldHaveCheckFilenameForDBSourceMatchDBNameForLocal() throws ConfigurationException, IOException {
        AppConfiguration normalConfig = loadConfigFromFile("local.yml");

        RemoteDataSourceConfig remoteConfig = getSourceFrom(normalConfig.getRemoteDataSourceConfig(), DataSourceID.database);
        assertEquals(normalConfig.getGraphDBConfig().getDbPath(), remoteConfig.getDataPath().resolve(remoteConfig.getModTimeCheckFilename()));
    }

    @Test
    void shouldHaveCheckFilenameForDBSourceMatchDBNameForGM() throws ConfigurationException, IOException {
        AppConfiguration normalConfig = loadConfigFromFile("gm.yml");

        RemoteDataSourceConfig remoteConfig = getSourceFrom(normalConfig.getRemoteDataSourceConfig(), DataSourceID.database);
        assertEquals(normalConfig.getGraphDBConfig().getDbPath(), remoteConfig.getDataPath().resolve(remoteConfig.getModTimeCheckFilename()));
    }

    @Test
    void shouldHaveSameTFGMSourceConfigForNormalAndTrainEnabled() throws ConfigurationException, IOException {
        AppConfiguration normalConfig = loadConfigFromFile("local.yml");
        AppConfiguration gmConfig = loadConfigFromFile("gm.yml");

        // TODO Which other parameters should be the same?

        checkGTFSSourceConfig(normalConfig, gmConfig, Category.Closures.not(EnumSet.noneOf(Category.class)));
        assertRemoteSources(normalConfig.getRemoteDataSourceConfig(), gmConfig.getRemoteDataSourceConfig(), DataSourceID.tfgm);

        // TODO add this
        //assertRemoteSources(normalConfig.getRemoteDataSourceConfig(), gmConfig.getRemoteDataSourceConfig(), DataSourceID.database);

    }

    private void assertRailLiveData(OpenLdbConfig fromFile, OpenLdbConfig testConfig) {
        assertNotNull(fromFile);
        assertNotNull(testConfig);
    }

    @Test
    void shouldHaveKeyParamtersSameForGMRail() throws ConfigurationException, IOException {
        AppConfiguration appConfig = loadConfigFromFile("gm.yml");
        AppConfiguration testConfig = new RailAndTramGreaterManchesterConfig();

        validateCoreParameters(Collections.emptyList(), appConfig, testConfig);

        List<RemoteDataSourceConfig> configRemoteSources = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testRemoteSources = testConfig.getRemoteDataSourceConfig();

        assertEquals(appConfig.getNumberQueries(), testConfig.getNumberQueries(), "number of queries mismatch");
        assertEquals(appConfig.getQueryInterval(), testConfig.getQueryInterval(), "query interval mismatch");

        assertEquals(configRemoteSources.size(), testRemoteSources.size());
        assertEquals(5, testRemoteSources.size());

        assertRemoteSources(configRemoteSources, testRemoteSources, DataSourceID.tfgm);
        assertRemoteSources(configRemoteSources, testRemoteSources, DataSourceID.rail);
        assertRemoteSources(configRemoteSources, testRemoteSources, DataSourceID.naptanxml);
        assertRemoteSources(configRemoteSources, testRemoteSources, DataSourceID.nptg);

        RailConfig configRail = appConfig.getRailConfig();
        RailConfig testRail = testConfig.getRailConfig();

        assertEquals(configRail.getStations(), testRail.getStations());
        assertEquals(configRail.getTimetable(), testRail.getTimetable());
        assertEquals(configRail.getModes(), testRail.getModes());

        assertRailLiveData(appConfig.getOpenldbwsConfig(), testConfig.getOpenldbwsConfig());

        checkDataSourceConfig(configRail, testRail);

    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTests() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("local.yml");
        AppConfiguration accTestConfig = loadConfigFromFile("localAcceptance.yml");

        validateCoreParameters(Collections.emptySet(), appConfig, accTestConfig);

        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "getQueryInterval");
        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "getNumberQueries");

        List<RemoteDataSourceConfig> dataSourceConfig = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testDataSourceConfig = accTestConfig.getRemoteDataSourceConfig();
        assertEquals(dataSourceConfig.size(), testDataSourceConfig.size());
        assertEquals(2, dataSourceConfig.size());

        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.tfgm);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.database);
    }

    @Test
    void shouldHaveKeyParametersSameForAcceptanceTestsGM() throws IOException, ConfigurationException {

        AppConfiguration appConfig = loadConfigFromFile("gm.yml");
        AppConfiguration accTestConfig = loadConfigFromFile("localAcceptanceGM.yml");

        validateCoreParameters(Collections.singleton(Category.Modes), appConfig, accTestConfig);

        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "number of queries mismatch");
        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "query interval mismatch");

        assertEquals(appConfig.getQueryInterval(), accTestConfig.getQueryInterval(), "getQueryInterval");
        assertEquals(appConfig.getNumberQueries(), accTestConfig.getNumberQueries(), "getNumberQueries");

        checkDataSourceConfig(appConfig.getRailConfig(), accTestConfig.getRailConfig());

        checkRailDataVersionFor(appConfig);
        checkRailDataVersionFor(accTestConfig);

        List<RemoteDataSourceConfig> dataSourceConfig = appConfig.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> testDataSourceConfig = accTestConfig.getRemoteDataSourceConfig();
        assertEquals(dataSourceConfig.size(), testDataSourceConfig.size());
        assertEquals(5, dataSourceConfig.size());

        // rail tested above
        //assertRemoteSources(dataSourceConfig, testDataSourceConfig, 0);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.rail);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.tfgm);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.naptanxml);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.nptg);
        assertRemoteSources(dataSourceConfig, testDataSourceConfig, DataSourceID.database);


    }

    private void checkRailDataVersionFor(AppConfiguration appConfig) {
        String version = appConfig.getRailConfig().getVersion();
        RemoteDataSourceConfig dataSourceConfig = appConfig.getDataRemoteSourceConfig(DataSourceID.rail);
        String zip = String.format("ttis%s.zip", version);
        assertTrue(dataSourceConfig.getDataUrl().contains(zip),
                "Rail config and data source config mismatch? version:"+version+" Url: "+dataSourceConfig.getDataUrl());
    }

    private void validateCoreParameters(Collection<Category> excluded, AppConfiguration expected, AppConfiguration testConfig) {
        assertEquals(expected.getStaticAssetCacheTimeSeconds(), testConfig.getStaticAssetCacheTimeSeconds(), "StaticAssetCacheTimeSeconds");
        assertEquals(expected.getMaxWait(), testConfig.getMaxWait(), "MaxWait");
        assertEquals(expected.getChangeAtInterchangeOnly(), testConfig.getChangeAtInterchangeOnly(), "ChangeAtInterchangeOnly");
        assertEquals(expected.getWalkingMPH(), testConfig.getWalkingMPH(), "WalkingMPH");
        assertEquals(expected.getNearestStopRangeKM(), testConfig.getNearestStopRangeKM(), "NearestStopRangeKM");
        assertEquals(expected.getWalkingDistanceRange(), testConfig.getWalkingDistanceRange(), "NearestStopForWalkingRangeKM");

        assertEquals(expected.getNumOfNearestStopsToOffer(), testConfig.getNumOfNearestStopsToOffer(), "NumOfNearestStopsToOffer");
        assertEquals(expected.getNumOfNearestStopsForWalking(), testConfig.getNumOfNearestStopsForWalking(), "NumOfNearestStopsForWalking");
        assertEquals(expected.getRecentStopsToShow(), testConfig.getRecentStopsToShow(), "RecentStopsToShow");
        assertEquals(expected.getMaxNumResults(), testConfig.getMaxNumResults(), "MaxNumResults");
        assertEquals(expected.getDistributionBucket(), testConfig.getDistributionBucket(), "distributionBucket");
        assertEquals(expected.getMaxJourneyDuration(), testConfig.getMaxJourneyDuration(), "MaxJourneyDuration");
        assertEquals(expected.getDepthFirst(), testConfig.getDepthFirst(), "depthFirst");

        assertEquals(expected.redirectToSecure(), testConfig.redirectToSecure());

        boolean hasNeighbourConfig = expected.hasNeighbourConfig();
        assertEquals(hasNeighbourConfig, testConfig.hasNeighbourConfig(), "has neighbour config");
        if (hasNeighbourConfig) {
            validateNeighbourConfig(expected, testConfig);
        }
        if (Category.Modes.not(excluded)) {
            assertEquals(expected.getTransportModes(), testConfig.getTransportModes(), "getTransportModes");
        }
        assertEquals(expected.getCalcTimeoutMillis(), testConfig.getCalcTimeoutMillis(), "CalcTimeoutMillis");
        assertEquals(expected.getPlanningEnabled(), testConfig.getPlanningEnabled(), "planningEnabled");

        if (Category.Bounds.not(excluded)) {
            assertEquals(expected.getBounds(), testConfig.getBounds(), "bounds");
        }

        checkDBConfig(expected, testConfig);

        checkGTFSSourceConfig(expected, testConfig, Category.Closures.not(excluded));

        checkRemoteDataSourceConfig(expected, testConfig);

    }

    private void validateNeighbourConfig(AppConfiguration appConfiguration, AppConfiguration testAppConfig) {
        NeighbourConfig expected = appConfiguration.getNeighbourConfig();
        NeighbourConfig testConfig = testAppConfig.getNeighbourConfig();

        assertEquals(expected.getMaxNeighbourConnections(), testConfig.getMaxNeighbourConnections(),
                "Max neighbour connections");
        assertEquals(expected.getDistanceToNeighboursKM(), testConfig.getDistanceToNeighboursKM(),
                "DistanceToNeighboursKM");
        List<StationIdPair> expectedAdditional = expected.getAdditional();
        assertEquals(expectedAdditional.size(), testConfig.getAdditional().size(), "additional neighbours");
        expectedAdditional.forEach(pair ->
                assertTrue(testConfig.getAdditional().contains(pair),
                        pair.toString() + " is missing from " + testConfig.getAdditional()));
    }

    private void checkRemoteDataSourceConfig(AppConfiguration expected, AppConfiguration testConfig) {
        List<RemoteDataSourceConfig> expectedRemoteDataSourceConfig = expected.getRemoteDataSourceConfig();
        List<RemoteDataSourceConfig> foundRemoteDataSourceConfig = testConfig.getRemoteDataSourceConfig();

        assertFalse(expectedRemoteDataSourceConfig.isEmpty());
        assertEquals(expectedRemoteDataSourceConfig.size(), foundRemoteDataSourceConfig.size(), "RemoteDataSourceConfig");
        for (int i = 0; i < expectedRemoteDataSourceConfig.size(); i++) {
            RemoteDataSourceConfig expectedRemote = expectedRemoteDataSourceConfig.get(0);
            RemoteDataSourceConfig foundRemote = foundRemoteDataSourceConfig.get(0);
            //assertEquals(expectedRemote.getDataUrl(), foundRemote.getDataUrl());
            assertEquals(expectedRemote.getDataCheckUrl(), foundRemote.getDataCheckUrl(), "DataCheckUrl");
        }
    }

    private void checkGTFSSourceConfig(AppConfiguration expected, AppConfiguration testConfig, boolean checkClosures) {
        List<GTFSSourceConfig> expectedgtfsDataSource = expected.getGTFSDataSource();
        List<GTFSSourceConfig> foundgtfsDataSource = testConfig.getGTFSDataSource();
        assertEquals(expectedgtfsDataSource.size(), foundgtfsDataSource.size());
        //assume same order
        for (int i = 0; i < expectedgtfsDataSource.size(); i++) {
            GTFSSourceConfig expectedDataSource = expectedgtfsDataSource.get(i);
            GTFSSourceConfig dataSourceConfig = foundgtfsDataSource.get(i);

            assertEquals(expectedDataSource.getNoServices(), dataSourceConfig.getNoServices() , "NoServices");
            assertEquals(expectedDataSource.getTransportGTFSModes(), dataSourceConfig.getTransportGTFSModes(), "TransportGTFSModes");
            assertEquals(expectedDataSource.getAdditionalInterchanges(), dataSourceConfig.getAdditionalInterchanges(), "AdditionalInterchanges");
            if (checkClosures) {
                assertEquals(expectedDataSource.getStationClosures(), dataSourceConfig.getStationClosures(), "station closures");
            }
            assertEquals(expectedDataSource.getAddWalksForClosed(), dataSourceConfig.getAddWalksForClosed(), "AddWalksForClosed");
            assertEquals(expectedDataSource.getHasFeedInfo(), dataSourceConfig.getHasFeedInfo(), "feedinfo mismatch");
            checkDataSourceConfig(expectedDataSource, dataSourceConfig);
        }
    }

    private void checkDataSourceConfig(TransportDataSourceConfig expected, TransportDataSourceConfig testConfig) {
        assertEquals(expected.getMaxInitialWait(), testConfig.getMaxInitialWait());
        assertEquals(expected.getDataSourceId(), testConfig.getDataSourceId());
        assertEquals(expected.getOnlyMarkedInterchanges(), testConfig.getOnlyMarkedInterchanges());
    }

    private void checkDBConfig(AppConfiguration expected, AppConfiguration testConfig) {
        GraphDBConfig expectedGraphDBConfig = expected.getGraphDBConfig();
        GraphDBConfig testGraphDBConfig = testConfig.getGraphDBConfig();
        assertEquals(expectedGraphDBConfig.getNeo4jPagecacheMemory(), testGraphDBConfig.getNeo4jPagecacheMemory(),
                "neo4jPagecacheMemory");

        TfgmTramLiveDataConfig expectedLiveDataConfig = expected.getLiveDataConfig();

        if (expectedLiveDataConfig!=null) {
            TfgmTramLiveDataConfig liveDataConfig = testConfig.getLiveDataConfig();
            assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutMessages(), liveDataConfig.getMaxNumberStationsWithoutMessages());
            assertEquals(expectedLiveDataConfig.getMaxNumberStationsWithoutData(), liveDataConfig.getMaxNumberStationsWithoutData());
            assertEquals(expectedLiveDataConfig.getS3Bucket().isEmpty(), liveDataConfig.getS3Bucket().isEmpty(), "s3 data upload disabled");
        } else {
            assertNull(testConfig.getLiveDataConfig());
        }
    }

    private AppConfiguration loadConfigFromFile(String configFilename) throws IOException, ConfigurationException {
        Path mainConfig = Paths.get("config", configFilename).toAbsolutePath();
        return StandaloneConfigLoader.LoadConfigFromFile(mainConfig);
    }

    @NotNull
    private Set<Path> getConfigFiles() throws IOException {
        Path configDir = Paths.get("config").toAbsolutePath();
        return Files.list(configDir).
                filter(Files::isRegularFile).
                filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml")).
                collect(Collectors.toSet());
    }

    private void assertRemoteSources(List<RemoteDataSourceConfig> remoteSources, List<RemoteDataSourceConfig> testRemoteSources, DataSourceID dataSourceID) {

        final RemoteDataSourceConfig testRemoteSource = getSourceFrom(remoteSources, dataSourceID);
        final RemoteDataSourceConfig remoteSource = getSourceFrom(testRemoteSources, dataSourceID);
        assertEquals(remoteSource.getName(), testRemoteSource.getName(), "name for " + dataSourceID);
        assertEquals(remoteSource.getDataCheckUrl(), testRemoteSource.getDataCheckUrl(), "check url for " + dataSourceID);
        assertEquals(remoteSource.getDataUrl(), testRemoteSource.getDataUrl(),
                remoteSource.getDataUrl() + " not matching " + testRemoteSource.getDataUrl() + " for " + dataSourceID);

        assertEquals(remoteSource.getDownloadFilename(), testRemoteSource.getDownloadFilename(),
                remoteSource.getDownloadFilename() + " did not contain " + testRemoteSource.getDownloadFilename() + " for " + dataSourceID);
        assertEquals(remoteSource.getDefaultExpiry(), testRemoteSource.getDefaultExpiry(), "default expiry for " + dataSourceID);
        assertEquals(remoteSource.isMandatory(), testRemoteSource.isMandatory(), "mandatory " + dataSourceID);
        assertEquals(remoteSource.getSkipUpload(), testRemoteSource.getSkipUpload(), "skip upload " + dataSourceID);

        assertEquals(remoteSource.getDownloadPath(), testRemoteSource.getDownloadPath(), "downloadPath " + dataSourceID);
        assertEquals(remoteSource.getDataPath(), testRemoteSource.getDataPath(), "dataPath " + dataSourceID);

    }

    private RemoteDataSourceConfig getSourceFrom(List<RemoteDataSourceConfig> remoteSources, DataSourceID dataSourceID) {
        List<RemoteDataSourceConfig> matched = remoteSources.stream().
                filter(source -> source.getDataSourceId().equals(dataSourceID)).toList();
        assertFalse(matched.isEmpty(), "Could not find a data source for " + dataSourceID);
        assertEquals(1, matched.size(), "Wrong number of data sources matched");
        return matched.get(0);

    }

}
