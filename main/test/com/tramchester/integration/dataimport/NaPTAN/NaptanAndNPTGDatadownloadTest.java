package com.tramchester.integration.dataimport.NaPTAN;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.nptg.NPTGXMLDataLoader;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfigWithNaptan;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NaptanAndNPTGDatadownloadTest {
    private static GuiceContainerDependencies componentContainer;
    private static TramchesterConfig testConfig;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfigWithNaptan(EnumSet.of(TransportMode.Bus, TransportMode.Tram, TransportMode.Train));
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldHaveExpectedDownloadFilePresentForNaptan() {
        final DataSourceID sourceID = DataSourceID.naptanxml;

        Path result = getDownloadedFileFor(sourceID);

        assertTrue(Files.exists(result));

        RemoteDataSourceConfig config = testConfig.getDataRemoteSourceConfig(sourceID);

        Path expected = config.getDataPath().resolve("NaPTAN.xml");
        assertEquals(expected, result);

    }

    @Test
    void shouldHaveExpectedDownloadFilePresentForNPTG() {
        final DataSourceID sourceID = DataSourceID.nptg;

        RemoteDataSourceConfig config = testConfig.getDataRemoteSourceConfig(sourceID);
        final Path configDataPath = config.getDataPath();

        Path result = getDownloadedFileFor(sourceID);

        assertTrue(Files.exists(result));

        Path expected = configDataPath.resolve(NPTGXMLDataLoader.LOCALITIES_XML);
        assertEquals(expected, result);

        FileFilter filter = pathname -> pathname.getName().toLowerCase().endsWith(".xml");
        final File[] fileArray = configDataPath.toFile().listFiles(filter);
        assertNotNull(fileArray);

        Set<String> names = Arrays.stream(fileArray).map(File::getName).collect(Collectors.toSet());

        // todo this keeps changing from 1 to 9, might have to disable this assertion
        assertEquals(1, names.size(), "Unexpected number of files " + names);

        // this is the important one, is the file we need present or not?
        assertTrue(names.contains(NPTGXMLDataLoader.LOCALITIES_XML), NPTGXMLDataLoader.LOCALITIES_XML + " is missing from " + names);

    }

    private Path getDownloadedFileFor(DataSourceID sourceID) {
        UnzipFetchedData unzipFetchedData = componentContainer.get(UnzipFetchedData.class);
        unzipFetchedData.getReady();

        RemoteDataAvailable dataRefreshed = componentContainer.get(RemoteDataAvailable.class);

        assertTrue(dataRefreshed.hasFileFor(sourceID));

        return dataRefreshed.fileFor(sourceID);
    }


}
