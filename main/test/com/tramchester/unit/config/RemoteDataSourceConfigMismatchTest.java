package com.tramchester.unit.config;

import com.tramchester.config.AppConfiguration;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.integration.testSupport.naptan.NaptanRemoteDataSourceTestConfig;
import com.tramchester.integration.testSupport.nptg.NPTGDataSourceTestConfig;
import com.tramchester.integration.testSupport.postcodes.PostCodeDatasourceConfig;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoteDataSourceConfigMismatchTest {

    Path configDir = Paths.get("config").toAbsolutePath();

    // TODO Postcode - which is currently unused?

    @Test
    void shouldHaveMatchingNaptanSectionForTest() throws ConfigurationException, IOException {
        NaptanRemoteDataSourceTestConfig naptanTestConfig = new NaptanRemoteDataSourceTestConfig(Path.of("data/naptan"));
        RemoteDataSourceConfig naptanConfig = getRemoteDataSourceConfig("gm.yml", DataSourceID.naptanxml);
        validateConfig(naptanConfig, naptanTestConfig);
    }

    @Test
    void shouldHaveMatchingNPTGSectionForTest() throws ConfigurationException, IOException {
        NPTGDataSourceTestConfig nptgTestConfig = new NPTGDataSourceTestConfig();
        RemoteDataSourceConfig naptanConfig = getRemoteDataSourceConfig("gm.yml", DataSourceID.nptg);
        validateConfig(naptanConfig, nptgTestConfig);
    }

    @Test
    void shouldHaveMatchingTFGMForBusAndDefault() throws ConfigurationException, IOException {
        validateConfig("buses.yml", "local.yml", DataSourceID.tfgm);
    }

    @Test
    void shouldHaveMatchingTFGMForGMAndDefault() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "local.yml", DataSourceID.tfgm);
    }

    @Test
    void shouldHaveMatchingNPTGSectionGMandBuses() throws ConfigurationException, IOException {
        validateConfig("buses.yml", "gm.yml", DataSourceID.nptg);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandBuses() throws ConfigurationException, IOException {
        validateConfig("buses.yml", "gm.yml", DataSourceID.naptanxml);
    }

    @Test
    void shouldHaveMatchingNPTGSectionGMandRail() throws ConfigurationException, IOException {
        validateConfig("rail.yml", "gm.yml", DataSourceID.nptg);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandRail() throws ConfigurationException, IOException {
        validateConfig("rail.yml", "gm.yml", DataSourceID.naptanxml);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMAcceptance() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "localAcceptanceGM.yml", DataSourceID.naptanxml);
    }

    @Test
    void shouldHaveMatchingNptgSectionGMAcceptance() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "localAcceptanceGM.yml", DataSourceID.nptg);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandAll() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "all.yml", DataSourceID.naptanxml);
    }

    @Test
    void shouldHaveMatchingNptgSectionGMandAll() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "all.yml", DataSourceID.nptg);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandFrequency() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "frequency.yml", DataSourceID.naptanxml);
    }

    @Test
    void shouldHaveMatchingNptgSectionGMandFrequency() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "frequency.yml", DataSourceID.nptg);
    }

    @Test
    void shouldHaveMatchingRailSectionGMandFrequency() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "frequency.yml", DataSourceID.rail);
    }

    @Test
    void shouldHaveMatchingTFGMSectionGMandFrequency() throws ConfigurationException, IOException {
        validateConfig("gm.yml", "frequency.yml", DataSourceID.tfgm);
    }


    @Disabled("postcode no longer enabled for bus")
    @Test
    void shouldHaveMatchingPostcodeSectionTestAndBus() throws ConfigurationException, IOException {
        RemoteDataSourceConfig testConfig = new PostCodeDatasourceConfig();
        RemoteDataSourceConfig buses = getRemoteDataSourceConfig("buses.yml", DataSourceID.postcode);

        validateConfig(testConfig, buses);
    }


    private RemoteDataSourceConfig getRemoteDataSourceConfig(String configFileName, DataSourceID dataSourceID) throws IOException, ConfigurationException {
        AppConfiguration busConfig = getConfigFile(configFileName);
        return busConfig.getDataRemoteSourceConfig(dataSourceID);
    }

    void validateConfig(String filenameA, String filenameB, DataSourceID dataSourceID) throws ConfigurationException, IOException {
        RemoteDataSourceConfig remoteDataSourceConfigA = getRemoteDataSourceConfig(filenameA, dataSourceID);
        RemoteDataSourceConfig remoteDataSourceConfigB = getRemoteDataSourceConfig(filenameB, dataSourceID);
        validateConfig(remoteDataSourceConfigA, remoteDataSourceConfigB);
    }

    private void validateConfig(RemoteDataSourceConfig sourceA, RemoteDataSourceConfig sourceB) {
        assertEquals(sourceA.getDataUrl(), sourceB.getDataUrl());
        assertEquals(sourceA.getDataSourceId(), sourceB.getDataSourceId());
        assertEquals(sourceA.getDownloadFilename(), sourceB.getDownloadFilename());
        assertEquals(sourceA.getDataCheckUrl(), sourceB.getDataCheckUrl());

        assertEquals(sourceA.getDefaultExpiry(), sourceB.getDefaultExpiry());
    }


    private AppConfiguration getConfigFile(String configFileName) throws IOException, ConfigurationException {
        Path busConfigFile = configDir.resolve(Path.of(configFileName));
        return TestEnv.LoadConfigFromFile(busConfigFile);
    }

}
