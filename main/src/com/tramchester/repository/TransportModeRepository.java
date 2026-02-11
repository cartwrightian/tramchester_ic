package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LazySingleton
public class TransportModeRepository {
    private static final Logger logger = LoggerFactory.getLogger(TransportModeRepository.class);

    public static final ImmutableEnumSet<TransportMode> ProductionModes = ImmutableEnumSet.of(TransportMode.Tram);

    private final ImmutableEnumSet<TransportMode> enabledModes;
    private final boolean inProduction;

    @Inject
    public TransportModeRepository(TramchesterConfig config) {
        this.enabledModes = ImmutableEnumSet.copyOf(config.getTransportModes());
        this.inProduction = config.inProdEnv();
    }

    public ImmutableEnumSet<TransportMode> getModes() {
        return enabledModes;
    }

    public ImmutableEnumSet<TransportMode> getModes(boolean beta) {
        if (!inProduction) {
            return enabledModes;
        }

        if (beta) {
            logger.warn("In Prod Environment, Beta mode is enabled");
        } else {
            if (enabledModes.size()>1) {
                logger.warn("Multiple transport modes enabled but beta mode is false");
            }
        }

        return beta ? enabledModes : ProductionModes;
    }
}
