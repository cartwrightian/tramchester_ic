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
    void shouldHaveMatchingNPTGSectionGMandBuses() throws ConfigurationException, IOException {
        RemoteDataSourceConfig buses = getRemoteDataSourceConfig("buses.yml", DataSourceID.nptg);
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.nptg);

        validateConfig(gm, buses);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandBuses() throws ConfigurationException, IOException {
        RemoteDataSourceConfig buses = getRemoteDataSourceConfig("buses.yml", DataSourceID.naptanxml);
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.naptanxml);

        validateConfig(gm, buses);
    }

    @Test
    void shouldHaveMatchingNPTGSectionGMandRail() throws ConfigurationException, IOException {
        RemoteDataSourceConfig rail = getRemoteDataSourceConfig("rail.yml", DataSourceID.nptg);
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.nptg);

        validateConfig(gm, rail);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandRail() throws ConfigurationException, IOException {
        RemoteDataSourceConfig rail = getRemoteDataSourceConfig("rail.yml", DataSourceID.naptanxml);
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.naptanxml);

        validateConfig(gm, rail);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMAcceptance() throws ConfigurationException, IOException {
        RemoteDataSourceConfig buses = getRemoteDataSourceConfig("gm.yml", DataSourceID.naptanxml);
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("localAcceptanceGM.yml", DataSourceID.naptanxml);

        validateConfig(gm, buses);
    }

    @Test
    void shouldHaveMatchingNptgSectionGMAcceptance() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.nptg);
        RemoteDataSourceConfig gmAcceptance = getRemoteDataSourceConfig("localAcceptanceGM.yml", DataSourceID.nptg);

        validateConfig(gm, gmAcceptance);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandAll() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.naptanxml);
        RemoteDataSourceConfig all = getRemoteDataSourceConfig("all.yml", DataSourceID.naptanxml);

        validateConfig(gm, all);
    }

    @Test
    void shouldHaveMatchingNptgSectionGMandAll() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.nptg);
        RemoteDataSourceConfig all = getRemoteDataSourceConfig("all.yml", DataSourceID.nptg);

        validateConfig(gm, all);
    }

    @Test
    void shouldHaveMatchingNaptanSectionGMandFrequency() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.naptanxml);
        RemoteDataSourceConfig frequency = getRemoteDataSourceConfig("frequency.yml", DataSourceID.naptanxml);

        validateConfig(gm, frequency);
    }

    @Test
    void shouldHaveMatchingNptgSectionGMandFrequency() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.nptg);
        RemoteDataSourceConfig frequency = getRemoteDataSourceConfig("frequency.yml", DataSourceID.nptg);

        validateConfig(gm, frequency);
    }

    @Test
    void shouldHaveMatchingRailSectionGMandFrequency() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.rail);
        RemoteDataSourceConfig frequency = getRemoteDataSourceConfig("frequency.yml", DataSourceID.rail);

        validateConfig(gm, frequency);
    }

    @Test
    void shouldHaveMatchingTFGMSectionGMandFrequency() throws ConfigurationException, IOException {
        RemoteDataSourceConfig gm = getRemoteDataSourceConfig("gm.yml", DataSourceID.tfgm);
        RemoteDataSourceConfig frequency = getRemoteDataSourceConfig("frequency.yml", DataSourceID.tfgm);

        validateConfig(gm, frequency);
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
