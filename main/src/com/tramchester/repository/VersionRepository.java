package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.Version;

import jakarta.inject.Inject;

import static java.lang.String.format;

@LazySingleton
public class VersionRepository {
    private final TramchesterConfig config;

    @Inject
    public VersionRepository(TramchesterConfig config) {
        this.config = config;
    }

    public Version getVersion() {
        Integer build = config.getBuildNumber();
        String version = format("%s.%s", Version.MajorVersion, build);
        return new Version(version);
    }
}
