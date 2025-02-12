package com.tramchester.config;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@LazySingleton
public class BusReplacementRepository {
    private static final Logger logger = LoggerFactory.getLogger(BusReplacementRepository.class);

    private final IdSet<Route> replacementBusRoutes;

    @Inject
    public BusReplacementRepository() {
        replacementBusRoutes = new IdSet<>();
    }

    @PostConstruct
    public void start() {
        // TODO maybe into config at some point, but fine in code for now
        replacementBusRoutes.add(Route.createId("2749"));
        replacementBusRoutes.add(Route.createId("2757"));
        replacementBusRoutes.forEach(routeId -> {
            logger.info("Have bus replacement route " + routeId);
        });
    }

    public boolean hasReplacementBuses() {
        return !replacementBusRoutes.isEmpty();
    }

    public boolean isReplacement(IdFor<Route> id) {
        return replacementBusRoutes.contains(id);
    }
}
