package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.repository.WriteableTransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
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

    public ExcludedRoutes load(final Stream<RouteData> routeDataStream, final CompositeIdMap<Agency, MutableAgency> allAgencies) {
        final EnumSet<GTFSTransportationType> transportModes = EnumSet.copyOf(sourceConfig.getTransportGTFSModes());
        final DataSourceID dataSourceID = sourceConfig.getDataSourceId();

        final AtomicInteger count = new AtomicInteger();

        final ExcludedRoutes excludedRoutes = new ExcludedRoutes();


        logger.info("Loading routes for transport modes " + transportModes.toString());
        routeDataStream.forEach(routeData -> {
            final IdFor<Agency> agencyId = routeData.getAgencyId();
            boolean missingAgency = !allAgencies.hasId(agencyId);
            if (missingAgency) {
                logger.error("Missing agency " + agencyId);
            }

            final GTFSTransportationType routeType = factory.getRouteType(routeData, agencyId);

            if (transportModes.contains(routeType)) {
                final MutableAgency agency = missingAgency ? createMissingAgency(dataSourceID, allAgencies, agencyId, factory)
                        : allAgencies.get(agencyId);

                final MutableRoute route = factory.createRoute(routeType, routeData, agency);

                agency.addRoute(route);
                if (!buildable.hasAgencyId(agencyId)) {
                    buildable.addAgency(agency);
                }
                buildable.addRoute(route);

                count.getAndIncrement();

            } else {
                String routeIdText = routeData.getId();
                excludedRoutes.excludeRoute(factory.createRouteId(routeIdText));
            }
        });
        excludedRoutes.recordInLog(transportModes);
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.numOfExcluded());

        return excludedRoutes;
    }

    private MutableAgency createMissingAgency(DataSourceID dataSourceID, CompositeIdMap<Agency, MutableAgency> allAgencies, IdFor<Agency> agencyId,
                                              TransportEntityFactory factory) {
        MutableAgency unknown = factory.createUnknownAgency(dataSourceID, agencyId);
        logger.error("Created agency" + unknown + " for " + dataSourceID);
        allAgencies.add(unknown);
        return unknown;
    }

    public static class ExcludedRoutes {
        private final IdSet<Route> excludedRouteIds;

        public ExcludedRoutes() {
            excludedRouteIds = new IdSet<>();
        }

        public void excludeRoute(final IdFor<Route> routeId) {
            excludedRouteIds.add(routeId);
        }

        public boolean wasExcluded(final IdFor<Route> routeId) {
            return excludedRouteIds.contains(routeId);
        }

        public IdSet<Route> getExcluded() {
            return excludedRouteIds;
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
    }
}
