package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.EnumSet;

@LazySingleton
public class TransportModeRepository {
    private static final Logger logger = LoggerFactory.getLogger(TransportModeRepository.class);

    private final EnumSet<TransportMode> enabledModes;

    @Inject
    public TransportModeRepository(TramchesterConfig config) {
        this.enabledModes = config.getTransportModes();
    }

    public EnumSet<TransportMode> getModes() {
        return enabledModes;
    }

    public EnumSet<TransportMode> getModes(boolean beta) {
        if (beta) {
            logger.warn("Beta mode is enabled");
        } else {
            if (enabledModes.size()>1) {
                logger.warn("Multiple transport modes enabled but beta mode is false");
            }
        }

        return beta ? enabledModes : EnumSet.of(TransportMode.Tram);
    }
}
