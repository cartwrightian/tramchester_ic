package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.TripData;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TripLoader {
    private static final Logger logger = LoggerFactory.getLogger(TripLoader.class);

    private final WriteableTransportData buildable;
    private final TransportEntityFactory factory;

    public TripLoader(WriteableTransportData buildable, TransportEntityFactory factory) {
        this.buildable = buildable;
        this.factory = factory;
    }

    public TripAndServices load(final Stream<TripData> tripDataStream, final RouteDataLoader.ExcludedRoutes excludedRoutes) {
        logger.info("Loading trips");
        final TripAndServices results = new TripAndServices(factory);
        final Map<IdFor<Route>, IdSet<Trip>> missingRoutes = new HashMap<>();
        final AtomicInteger count = new AtomicInteger();

        tripDataStream.forEach((tripData) -> {
            final IdFor<Service> serviceId = tripData.getServiceId();
            final IdFor<Route> routeId = factory.createRouteId(tripData.getRouteId());
            final IdFor<Trip> tripId = tripData.getTripId();

            if (buildable.hasRouteId(routeId)) {
                final Route route = buildable.getMutableRoute(routeId);
                final MutableService service = results.getOrCreateService(serviceId);
                results.createTripIfMissing(tripId, tripData, service, route, route.getTransportMode());
                count.getAndIncrement();
            } else {
                if (!excludedRoutes.wasExcluded(routeId)) {
                    if (!missingRoutes.containsKey(routeId)) {
                        missingRoutes.put(routeId, new IdSet<>());
                    }
                    missingRoutes.get(routeId).add(tripId);
                }
            }
        });
        logger.info("Loaded " + count.get() + " trips");
        if (!missingRoutes.isEmpty()) {
            missingRoutes.forEach((routeId, tripIds) -> logger.warn(format("Route '%s' missing for trips '%s", routeId, tripIds)));
        }
        return results;
    }
}
