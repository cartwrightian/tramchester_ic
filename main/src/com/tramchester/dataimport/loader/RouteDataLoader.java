package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RouteDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(RouteDataLoader.class);

    private final WriteableTransportData buildable;
    private final GTFSSourceConfig sourceConfig;
    private final TransportEntityFactory factory;

    public RouteDataLoader(WriteableTransportData buildable, GTFSSourceConfig sourceConfig, TransportEntityFactory factory) {
        this.buildable = buildable;
        this.sourceConfig = sourceConfig;
        this.factory = factory;
    }

    public LoadedRoutesCache load(final Stream<RouteData> routeDataStream, final CompositeIdMap<Agency, MutableAgency> allAgencies) {
        final EnumSet<GTFSTransportationType> transportModes = EnumSet.copyOf(sourceConfig.getTransportGTFSModes());
        final DataSourceID dataSourceID = sourceConfig.getDataSourceId();

        final AtomicInteger count = new AtomicInteger();

        final LoadedRoutesCache loadedRoutesCache = new LoadedRoutesCache();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        routeDataStream.forEach(routeData -> {
            final IdFor<Agency> agencyId = routeData.getAgencyId();
            boolean missingAgency = !allAgencies.hasId(agencyId);
            if (missingAgency) {
                logger.error("Missing agency " + agencyId);
            }

            final GTFSTransportationType routeType = factory.getRouteType(routeData, agencyId);

            final RawRouteId rawRouteId = RawRouteId.create(routeData);

            if (transportModes.contains(routeType)) {
                final MutableAgency agency = missingAgency ? createMissingAgency(dataSourceID, allAgencies, agencyId, factory)
                        : allAgencies.get(agencyId);

                final MutableRoute route = factory.createRoute(routeType, routeData, agency);

                agency.addRoute(route);
                if (!buildable.hasAgencyId(agencyId)) {
                    buildable.addAgency(agency);
                }
                buildable.addRoute(route);

                loadedRoutesCache.record(rawRouteId, route.getId());

                count.getAndIncrement();

            } else {
                loadedRoutesCache.excludeRoute(rawRouteId);
            }
        });
        loadedRoutesCache.recordInLog(transportModes);
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ loadedRoutesCache.numOfExcluded());

        return loadedRoutesCache;
    }

    private MutableAgency createMissingAgency(DataSourceID dataSourceID, CompositeIdMap<Agency, MutableAgency> allAgencies, IdFor<Agency> agencyId,
                                              TransportEntityFactory factory) {
        MutableAgency unknown = factory.createUnknownAgency(dataSourceID, agencyId);
        logger.error("Created agency" + unknown + " for " + dataSourceID);
        allAgencies.add(unknown);
        return unknown;
    }

    public static class LoadedRoutesCache {
        private final Set<RawRouteId> excludedRouteIds; // so we only log missing routes that have not been specifically excluded
        private final Map<RawRouteId, IdFor<Route>> recordedRoutes;

        public LoadedRoutesCache() {
            excludedRouteIds = new HashSet<>();
            recordedRoutes = new HashMap<>();
        }

        public void excludeRoute(final RawRouteId routeId) {
            excludedRouteIds.add(routeId);
        }

        public boolean wasExcluded(final RawRouteId routeId) {
            return excludedRouteIds.contains(routeId);
        }

        public int numOfExcluded() {
            return excludedRouteIds.size();
        }

        public void clear() {
            excludedRouteIds.clear();
        }

        public void recordInLog(final Set<GTFSTransportationType> transportModes) {
            if (excludedRouteIds.isEmpty()) {
                return;
            }
            logger.info(format("Excluded %s route id's as did not match modes %s", excludedRouteIds.size(), transportModes));
        }

        public void record(final RawRouteId rawRouteId, final IdFor<Route> id) {
            recordedRoutes.put(rawRouteId, id);
        }

        public boolean hasRouteFor(final RawRouteId rawRouteId) {
            return recordedRoutes.containsKey(rawRouteId);
        }

        public IdFor<Route> getRouteIdFor(final RawRouteId rawRouteId) {
            return recordedRoutes.get(rawRouteId);
        }
    }

    public static class RawRouteId {
        private final String text;

        private RawRouteId(final String text) {
            this.text = text;
        }

        public static RawRouteId create(final String text) {
            return new RawRouteId(text);
        }

        public static RawRouteId create(final RouteData routeData) {
            return new RawRouteId(routeData.getId());
        }


        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RawRouteId that)) return false;
            return Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(text);
        }

        @Override
        public String toString() {
            return "RawRouteId{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }
}
