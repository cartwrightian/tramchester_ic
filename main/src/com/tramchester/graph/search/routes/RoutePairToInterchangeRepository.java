package com.tramchester.graph.search.routes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.InterchangeRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class RoutePairToInterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoutePairToInterchangeRepository.class);

    private final InterchangeRepository interchangeRepository;

    private final Map<RoutePair, Set<InterchangeStation>> routePairToInterchange;

    @Inject
    public RoutePairToInterchangeRepository(InterchangeRepository interchangeRepository) {
        this.interchangeRepository = interchangeRepository;
        routePairToInterchange = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        final Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        interchanges.forEach(this::populateFor);
        logger.info("Added interchanges for " + routePairToInterchange.size() + " route pairs");
        logger.info("Started");
    }

    @PreDestroy
    private void stop() {
        logger.info("Stop");
        routePairToInterchange.clear();
        logger.info("Stopped");
    }

    private void populateFor(final InterchangeStation interchange) {

        final Set<Route> dropOffAtInterchange = interchange.getDropoffRoutes();
        final Set<Route> pickupAtInterchange = interchange.getPickupRoutes();

        for (final Route dropOff : dropOffAtInterchange) {
            for (final Route pickup : pickupAtInterchange) {
                if ((!dropOff.equals(pickup)) && pickup.isDateOverlap(dropOff)) {
                    final RoutePair routePair = RoutePair.of(dropOff, pickup);
                    addInterchangeBetween(routePair, interchange);
                }
            }
        }
    }

    private void addInterchangeBetween(RoutePair pair, InterchangeStation interchange) {
        if (!routePairToInterchange.containsKey(pair)) {
            routePairToInterchange.put(pair, new HashSet<>());
        }
        routePairToInterchange.get(pair).add(interchange);
    }

    public boolean hasAnyInterchangesFor(RoutePair indexPair) {
        return routePairToInterchange.containsKey(indexPair);
    }

    public Set<InterchangeStation> getInterchanges(final RoutePair indexPair, final EnumSet<TransportMode> requestedModes) {
        return routePairToInterchange.get(indexPair).stream().
                filter(interchangeStation -> interchangeStation.anyOverlapWith(requestedModes)).
                collect(Collectors.toSet());
    }
}
