package com.tramchester.unit.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.config.RemoteDataSourceAppConfig;
import com.tramchester.domain.DataSourceID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class RemoteDataSourceConfigTest {

    // NOTE: does not use StandaloneConfigLoader so not entirely realistic

    private ObjectMapper mapper;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldParse() throws JsonProcessingException {
        String yaml = "    name: tfgm\n" +
                "    dataURL: s3://tramchesternewdist/dist/${RELEASE_NUMBER}/tfgm_data.zip\n" +
                "    dataCheckURL: https://odata.tfgm.com/opendata/downloads/TfGMgtfsnew.zip\n" +
                "    dataPath: data/tram\n" +
                "    downloadPath: data/tram\n" +
                "    filename: tfgm_data.zip\n" +
                "    defaultExpiryDays: 1";

        RemoteDataSourceAppConfig config = mapper.readValue(yaml, RemoteDataSourceAppConfig.class);


        Path expectedPath = Path.of("data", "tram");

        assertEquals(DataSourceID.tfgm, config.getDataSourceId());
        assertEquals("https://odata.tfgm.com/opendata/downloads/TfGMgtfsnew.zip", config.getDataCheckUrl());
        assertEquals("s3://tramchesternewdist/dist/${RELEASE_NUMBER}/tfgm_data.zip", config.getDataUrl());
        assertEquals(expectedPath, config.getDataPath());
        assertEquals("tfgm_data.zip", config.getDownloadFilename());
        assertEquals(Duration.ofDays(1), config.getDefaultExpiry());
        assertEquals(expectedPath, config.getDownloadPath());
        assertTrue( config.isMandatory());
        assertFalse( config.getSkipUpload());
        assertFalse(config.hasModCheckFilename());
        assertEquals("tfgm", config.getName());
        assertNull(config.getModTimeCheckFilename());
    }

    @Test
    void shouldThrowIfUnknownExtraField() {
        String yaml = "    name: tfgm\n" +
                "    dataURL: s3://tramchesternewdist/dist/${RELEASE_NUMBER}/tfgm_data.zip\n" +
                "    dataCheckURL: https://odata.tfgm.com/opendata/downloads/TfGMgtfsnew.zip\n" +
                "    dataPath: data/tram\n" +
                "    UNKNOWN: data/tram\n" +
                "    downloadPath: data/tram\n" +
                "    filename: tfgm_data.zip\n" +
                "    defaultExpiryDays: 1";

        assertThrows(JsonProcessingException.class, () -> mapper.readValue(yaml, RemoteDataSourceAppConfig.class));
    }

    @Test
    void shouldThrowIfMandatoryFieldMissing() {
        String yaml = "    name: tfgm\n" +
                "    dataURL: s3://tramchesternewdist/dist/${RELEASE_NUMBER}/tfgm_data.zip\n" +
                "    dataCheckURL: https://odata.tfgm.com/opendata/downloads/TfGMgtfsnew.zip\n" +
                "    downloadPath: data/tram\n" +
                "    filename: tfgm_data.zip\n" +
                "    defaultExpiryDays: 1";

        assertThrows(JsonProcessingException.class, () -> mapper.readValue(yaml, RemoteDataSourceAppConfig.class));
    }


}
