package com.tramchester.config;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Path;

@JsonDeserialize(as=GraphDBAppConfig.class)
public interface GraphDBConfig {

    Path getDbPath();

    Boolean enableDiagnostics();

}
