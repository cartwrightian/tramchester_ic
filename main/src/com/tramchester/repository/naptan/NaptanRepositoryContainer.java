package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanXMLData;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataCallbackImporter;
import com.tramchester.dataimport.NaPTAN.xml.NaptanFromXMLFile;
import com.tramchester.dataimport.NaPTAN.xml.stopArea.NaptanStopAreaData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanXMLStopAreaRef;
import com.tramchester.domain.id.*;
import com.tramchester.domain.places.*;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.repository.nptg.NPTGRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRepositoryContainer implements NaptanRepository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRepositoryContainer.class);

    private final NaptanDataCallbackImporter naptanDataImporter;
    private final NPTGRepository nptgRepository;
    private final TramchesterConfig config;

    private final IdMap<NaptanRecord> stops;
    private final Map<IdFor<Station>, IdFor<NaptanRecord>> tiplocToAtco;

    @Inject
    public NaptanRepositoryContainer(NaptanDataCallbackImporter naptanDataImporter, NPTGRepository nptgRepository, TramchesterConfig config) {
        this.naptanDataImporter = naptanDataImporter;
        this.nptgRepository = nptgRepository;
        this.config = config;
        stops = new IdMap<>();
        tiplocToAtco = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        final boolean enabled = naptanDataImporter.isEnabled();

        if (!enabled) {
            logger.warn("Not enabled, imported is disabled, no config for naptan?");
            return;
        } else {
            loadStopDataForConfiguredArea();
        }

        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        stops.clear();
        tiplocToAtco.clear();
        logger.info("stopped");
    }

    private void loadStopDataForConfiguredArea() {

        Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds = new HashMap<>();

        final BoundingBox bounds = config.getBounds();
        final Double range = config.getNearestStopForWalkingRangeKM();
        final MarginInMeters margin = MarginInMeters.of(range);

        logger.info("Loading data for " + bounds + " and range " + margin);

        Consumer consumer = new Consumer(bounds, margin, pendingAreaIds);

        naptanDataImporter.loadData(consumer);

        logger.info("Loaded " + stops.size() + " stops");
        logger.info("Loaded " + tiplocToAtco.size() + " mappings for rail stations" );

        consumer.logSkipped(logger);
    }

    private boolean consumeStop(final NaptanStopData stopData, final BoundingBox bounds, final MarginInMeters margin,
                             final Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds) {
        if (!stopData.hasValidAtcoCode()) {
            return false;
        }

        if (filterBy(bounds, margin, stopData)) {
            final NaptanRecord record = createRecord(stopData, pendingAreaIds);
            stops.add(record);

            if (stopData.hasRailInfo()) {
                final IdFor<Station> id = Station.createId(stopData.getRailInfo().getTiploc());
                tiplocToAtco.put(id, stopData.getAtcoCode());
            }

            return true;
        } else {
            return false;
        }
    }

    private NaptanRecord createRecord(final NaptanStopData original, final Map<IdFor<NaptanRecord>,
            List<NaptanXMLStopAreaRef>> pendingAreaIds) {
        IdFor<NaptanRecord> atcoCode = original.getAtcoCode();

        String rawLocalityCode = original.getNptgLocality();
        final IdFor<NPTGLocality> localityCode = NPTGLocality.createId(rawLocalityCode);

        String suburb = original.getSuburb();
        String town = original.getTown();

        if (nptgRepository.hasLocaility(localityCode)) {
            final NPTGLocality extra = nptgRepository.get(localityCode);
            if (suburb.isBlank()) {
                suburb = extra.getLocalityName();
            }
            if (town.isBlank()) {
                town = extra.getParentLocalityName();
            }
        } else {
            logger.warn(format("Naptan localityCode '%s' missing from nptg for acto %s", localityCode, atcoCode));
        }

        final List<NaptanXMLStopAreaRef> stopAreaRefs = original.stopAreasRefs();

        // TODO Unlcear how useful this actually is, see also StationArea's etc, switch to nptg locality instead?
        // record pending areas, need to have loaded entire file before can properly check if active or not
        // see load method above
        List<NaptanXMLStopAreaRef> areaIds = stopAreaRefs.stream().
                filter(NaptanXMLStopAreaRef::isActive). // filter out if marked inactive for *this* stop
                collect(Collectors.toList());
        pendingAreaIds.put(atcoCode, areaIds);

        return new NaptanRecord(atcoCode, localityCode, original.getCommonName(), original.getGridPosition(), original.getLatLong(),
                suburb, town, original.getStopType());
    }

    private boolean filterBy(final BoundingBox bounds, final MarginInMeters margin, final NaptanXMLData item) {
        final GridPosition gridPosition = item.getGridPosition();
        if (!gridPosition.isValid()) {
            return false;
        }
        return bounds.within(margin, gridPosition);
    }

    // TODO Check or diag on NaptanStopType
    @Override
    public <T extends Location<?>>  boolean containsActo(final IdFor<T> locationId) {
        final IdFor<NaptanRecord> id = convertId(locationId);
        return stops.hasId(id);
    }

    // TODO Check or diag on NaptanStopType
    @Override
    public <T extends Location<?>> NaptanRecord getForActo(final IdFor<T> actoCode) {
        final IdFor<NaptanRecord> id = convertId(actoCode);
        return stops.get(id);
    }

    private <T extends Location<?>> IdFor<NaptanRecord> convertId(final IdFor<T> actoCode) {
        if (actoCode instanceof PlatformId) {
            return PlatformId.convert(actoCode, NaptanRecord.class);
        }
        return StringIdFor.convert(actoCode, NaptanRecord.class);
    }

    public boolean isEnabled() {
        return naptanDataImporter.isEnabled();
    }

    /***
     * Look up via train location code
     * @param railStationTiploc the code for the station
     * @return data if present, null otherwise
     */
    @Override
    public NaptanRecord getForTiploc(IdFor<Station> railStationTiploc) {
        if (!tiplocToAtco.containsKey(railStationTiploc)) {
            return null;
        }
        final IdFor<NaptanRecord> acto = tiplocToAtco.get(railStationTiploc);
        if (stops.hasId(acto)) {
            return stops.get(acto);
        }
        return null;
    }

    @Override
    public boolean containsTiploc(IdFor<Station> tiploc) {
        return tiplocToAtco.containsKey(tiploc);
    }

    @Override
    public boolean containsArea(IdFor<NPTGLocality> id) {
        // TODO use a map to do this?
        return stops.getValuesStream().anyMatch(stop -> stop.getLocalityId().equals(id));
    }

    /***
     * Naptan records for an area. For stations in an area use StationLocations.
     * @see com.tramchester.geo.StationLocations
     * @param areaId naptan area id
     * @return matching record
     */
    @Override
    public Set<NaptanRecord> getRecordsForLocality(IdFor<NPTGLocality> areaId) {
        // TODO use a map to do this?
        return stops.filterStream(stop -> areaId.equals(stop.getLocalityId())).collect(Collectors.toSet());
    }

    private class Consumer implements NaptanFromXMLFile.NaptanXmlConsumer {

        private final BoundingBox bounds;
        private final MarginInMeters margin;
        private final Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds;
        int skippedStopArea;
        int skippedStop;

        private Consumer(BoundingBox bounds, MarginInMeters margin, Map<IdFor<NaptanRecord>, List<NaptanXMLStopAreaRef>> pendingAreaIds) {
            this.bounds = bounds;
            this.margin = margin;
            this.pendingAreaIds = pendingAreaIds;
            skippedStop = 0;
            skippedStopArea = 0;
        }

        @Override
        public void process(NaptanStopAreaData element) {
//            if (!consumeStopArea(element, bounds, margin)) {
//                skippedStopArea++;
//            }
        }

        @Override
        public void process(NaptanStopData element) {
            if (!consumeStop(element, bounds, margin, pendingAreaIds)) {
                skippedStop++;
            }
        }

        public void logSkipped(Logger logger) {
            if (skippedStop>0) {
                logger.info("Skipped " + skippedStop + " stops");
            }
            if (skippedStopArea>0) {
                logger.warn("Skipped " + skippedStopArea + " stop areas");
            }
        }
    }
}
