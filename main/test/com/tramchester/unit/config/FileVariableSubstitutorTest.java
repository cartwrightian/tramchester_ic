package com.tramchester.unit.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.config.FileVariableSubstitutor;
import com.tramchester.config.StandaloneConfigLoader;
import com.tramchester.geo.BoundingBox;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.core.Configuration;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileVariableSubstitutorTest {

    private FileVariableSubstitutor substitutor;
    private Path folder;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        folder = Path.of("main","test","com","tramchester","unit","config", "examples");
        substitutor = new FileVariableSubstitutor(folder);
    }

    @Test
    void shouldSubFromFile() {
        String result = substitutor.replace("%[exampleText.yml]");
        assertEquals("expectedFileContents", result);
    }

    @Test
    void shouldSubFromFileManyLines() {
        String result = substitutor.replace("section: %[exampleLines.yml]");
        String expected =
                "section: " + System.lineSeparator() +
                "  minEastings: 333200\n" +
                "  minNorthings: 373130\n" +
                "  maxEasting: 414500\n" +
                "  maxNorthings: 437850";
        assertEquals(expected, result);
    }

    @Test
    void shouldThrowIfFileMissing() {
        assertThrows(IllegalArgumentException.class, () -> {
            substitutor.replace("%[notSuchFile.yml]");
        });
    }

    @Test
    void shouldLoadConfigWithIncludes() throws ConfigurationException, IOException {
        FileSubTestConfig loaded = StandaloneConfigLoader.LoadConfigFromFile(folder.resolve("exampleConfig.yml"),
                FileSubTestConfig.class);
        assertEquals(3500, loaded.calcTimeoutMillis);
        assertEquals("expectedFileContents", loaded.distributionBucket);
        BoundingBox expectedBox = new BoundingBox(333200, 373130, 414500, 437850);
        assertEquals(expectedBox, loaded.bounds);

        String expectedPlace = System.getenv("PLACE");
        assertEquals(expectedPlace, loaded.environmentName);
    }

    public static class FileSubTestConfig extends Configuration {

        @NotNull
        @JsonProperty("calcTimeoutMillis")
        private Long calcTimeoutMillis;

        @NotNull
        @JsonProperty("bounds")
        private BoundingBox bounds;

        @NotNull
        @JsonProperty("distributionBucket")
        private String distributionBucket;

        @NotNull
        @JsonProperty("environmentName")
        private String environmentName;
    }

}
