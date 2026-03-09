package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;

import java.nio.file.Path;

@Valid
@JsonIgnoreProperties(ignoreUnknown = false)
public class GraphDBAppConfig extends Configuration implements GraphDBConfig {

    // TODO Make a path, rename config name
    private final String graphName;
    private final Boolean enableDiagnostics;

    public GraphDBAppConfig(@JsonProperty(value = "graphName", required = true) String graphName,
                            @JsonProperty(value = "enableDiagnostics", required = false, defaultValue = "false") Boolean enableDiagnostics) {
        this.graphName = graphName;
        this.enableDiagnostics = enableDiagnostics;
    }

    @Override
    public Path getDbPath() {
        return Path.of(graphName);
    }

    @Override
    public Boolean enableDiagnostics() {
        if (enableDiagnostics==null) {
            return false;
        }
        return enableDiagnostics;
    }

    @Override
    public String toString() {
        return "GraphDBAppConfig{" +
                "graphName='" + graphName + '\'' +
                ", enableDiagnostics=" + enableDiagnostics +
                "} " + super.toString();
    }
}
