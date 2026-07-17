package com.tramchester.config;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.RouteRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;

import static com.tramchester.domain.Route.REPLACEMENT_BUS_PREFIX;

@LazySingleton
public class BusReplacementRepository {
    private static final Logger logger = LoggerFactory.getLogger(BusReplacementRepository.class);

    private final RouteRepository repository;
    private IdSet<Route> replacements;

    @Inject
    public BusReplacementRepository(RouteRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void start() {
        logger.info("started");
        final Set<Route> tramRoutes = repository.getRoutes(TransportMode.TramsOnly);
        replacements = tramRoutes.stream().
                filter(route -> route.getShortName().startsWith(REPLACEMENT_BUS_PREFIX)).
                collect(IdSet.collector());
        if (replacements.isEmpty()) {
            logger.info("No replacement buses");
        } else {
            logger.warn("Have " + replacements.size() + " replacement buses");
        }
    }

    @PreDestroy
    public void stop() {
        logger.info("stop");
        replacements.clear();
    }

    public boolean hasReplacementBuses() {
        return !replacements.isEmpty();
    }

    public boolean isReplacement(final IdFor<Route> id) {
        return replacements.contains(id);
    }

    public int number() {
        return replacements.size();
    }

}
