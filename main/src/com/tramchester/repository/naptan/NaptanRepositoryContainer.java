package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.ComponentThatCaches;
import com.tramchester.caching.FileDataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.HasDataSaver;
import com.tramchester.dataimport.NaPTAN.xml.NaptanDataImporter;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.id.*;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.*;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.nptg.NPTGRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.DataSourceID.*;
import static com.tramchester.domain.reference.TransportMode.Train;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRepositoryContainer  extends ComponentThatCaches<NaptanRecord, NaptanRepositoryContainer.CachingLoader> implements NaptanRepository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRepositoryContainer.class);

    private static final EnumSet<NaptanStopType> stopTypesForRail =
            EnumSet.copyOf(NaptanStopType.getTypesFor(Train));

    private final NaptanDataImporter naptanDataImporter;
    private final Geography geography;
    private final TramchesterConfig config;
    private final CachingLoader stops;

    private boolean hadCacheAtStart;
    private Map<IdFor<NPTGLocality>, ImmutableIdSet<NaptanRecord>> localities;

    @Inject
    public NaptanRepositoryContainer(NaptanDataImporter naptanDataImporter, NPTGRepository nptgRepository,
                                     Geography geography, TramchesterConfig config, FileDataCache dataCache) {
        super(dataCache, NaptanRecord.class, ImmutableEnumSet.of(naptanxml, nptg));
        this.naptanDataImporter = naptanDataImporter;
        this.geography = geography;
        this.config = config;

        localities = Collections.emptyMap();
        hadCacheAtStart = false;
        stops = new CachingLoader(nptgRepository, config);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        final boolean enabled = naptanDataImporter.isEnabled();

        if (!enabled) {
            logger.warn("Not enabled, imported is disabled, no config for naptan?");
            return;
        }

        hadCacheAtStart = super.loadFromCache(stops);
        if (hadCacheAtStart) {
            logger.info("loaded from cache");
        } else {
            logger.info("No cached data, in record mode");
            loadStopDataForConfiguredArea();
        }
        localities = stops.getLocatlities();

        stops.diagnostics();
        logger.info("Loaded " + localities.size() + " localities");

        logger.info("started");
    }

    @PreDestroy
    public void stop() {

        final boolean enabled = naptanDataImporter.isEnabled();

        if (enabled) {
            logger.info("stopping");

            if (!hadCacheAtStart) {
                super.saveCacheIfNeeded(stops);
            }
            stops.clear();
            logger.info("stopped");
        } else {
            logger.info("Was disabled in config");
        }
    }

    private void loadStopDataForConfiguredArea() {

        final BoundingBox bounds = config.getBounds();
        final MarginInMeters margin = config.getWalkingDistanceRange();

        logger.info("Loading data for " + bounds + " and range " + margin);

        final Receiver receiver = new Receiver(config, stops::createRecordForStop);

        naptanDataImporter.loadData(receiver);
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

    @Override
    public Stream<NaptanRecord> getAll() {
        return stops.getAll();
        //return stops.getValuesStream();
    }

    @Override
    public long size() {
        return stops.size();
    }

    /***
     * Look up via train location code
     * @param railStationTiploc the code for the station
     * @return data if present, null otherwise
     */
    @Override
    public NaptanRecord getForTiploc(final IdFor<Station> railStationTiploc) {
        return stops.getForTiploc(railStationTiploc);
    }

    @Override
    public boolean containsTiploc(final IdFor<Station> tiploc) {
        return stops.hasTiploc(tiploc);
    }

    @Override
    public boolean containsLocality(final IdFor<NPTGLocality> id) {
        return localities.containsKey(id);
    }

    /***
     * Naptan records for an area. For stations in an area use StationLocations.
     * @see StationLocations
     * @param localityId NPTG locality id
     * @return matching record
     */
    @Override
    public Set<NaptanRecord> getRecordsForLocality(final IdFor<NPTGLocality> localityId) {
        return localities.get(localityId).stream().map(stops::get).collect(toSet());
    }

    /***
     * Uses Latitude/Longitude and EPSG
     * @param localityId the area id
     * @return A list of points on convex hull containing the points within the given area
     */
    @Override
    public List<LatLong> getBoundaryFor(final IdFor<NPTGLocality> localityId) {

        final Set<NaptanRecord> records = getRecordsForLocality(localityId);

        final Stream<LatLong> points = records.stream().map(NaptanRecord::getLatLong);

        return geography.createBoundaryFor(points);
    }

    public static class Receiver extends ElementsFromXMLFile.XmlElementConsumer<NaptanStopData> {

        private final BoundingBox bounds;
        private final MarginInMeters margin;
        private final ImmutableEnumSet<NaptanStopType> requiredStopTypes;
        private final boolean hasTrain;

        public Receiver(final TramchesterConfig config, final Consumer<NaptanStopData> consumer) {
            super(NaptanStopData.class, consumer);

            bounds = config.getBounds();
            margin = config.getWalkingDistanceRange();

            hasTrain = config.getTransportModes().contains(Train);
            requiredStopTypes = NaptanStopType.getTypesFor(config.getTransportModesImmutable());
        }

        @Override
        protected boolean shouldInclude(final NaptanStopData stopData) {
            if (!stopData.hasValidAtcoCode()) {
                return false;
            }

            if (!stopData.isActive()) {
                return false;
            }

            final NaptanStopType stopType = stopData.getStopType();
            if (stopType == NaptanStopType.unknown) {
                logger.warn("Unknown stop type for " + stopData.getAtcoCode());
                return false;
            }

            if (!requiredStopTypes.contains(stopType)) {
                return false;
            }

            if (withinBounds(stopData)) {
                return true;
            }

            if (hasTrain && stopTypesForRail.contains(stopType)) {
                // when rail enabled we need rail records so can decode station IDs for route names etc.
                // i.e. train from Manchester to London where we want to show final station name properly in the route
                return true;
            }
            return false;
        }

        private boolean withinBounds(final HasGridPosition item) {
            final GridPosition gridPosition = item.getGridPosition();
            if (!gridPosition.isValid()) {
                return false;
            }
            return bounds.within(margin, gridPosition);
        }
    }

    public static class CachingLoader implements FileDataCache.CachesData<NaptanRecord> {

        // TODO currently FileDataCache clears all caches if any newer data is found from any data source

        private final boolean hasTrain;
        private IdMap<NaptanRecord> stops;
        private Map<IdFor<Station>, IdFor<NaptanRecord>> tiplocToAtco;
        private final NPTGRepository nptgRepository;

        public CachingLoader(final NPTGRepository nptgRepository, TramchesterConfig config) {
            this.nptgRepository = nptgRepository;
            this.hasTrain = config.getTransportModes().contains(Train);

            stops = new IdMap<>();
            tiplocToAtco = new HashMap<>();
        }

        private void createRecordForStop(final NaptanStopData stopData) {
            final NaptanRecord record = createRecord(stopData);

            stops.add(record);

            if (stopData.hasRailInfo()) {
                final IdFor<Station> id = Station.createId(stopData.getRailInfo().getTiploc());
                tiplocToAtco.put(id, stopData.getAtcoCode());
            }
        }

        @Override
        public void cacheTo(final HasDataSaver<NaptanRecord> saver) {
            final Stream<NaptanRecord> toCache = stops.getValuesStream();
            saver.cacheStream(toCache);
        }

        @Override
        public String getFilename() {
            final String name = hasTrain ? "naptan_records_rail" : "naptan_records_no_rail";
            return String.format("%s.json", name);
        }

        @Override
        public void loadFrom(final Stream<NaptanRecord> stream) throws FileDataCache.CacheLoadException {
            stops = stream.collect(IdMap.collector());
            tiplocToAtco = stops.getValuesStream().
                    filter(record -> record.getRailStationId().isValid()).
                    collect(Collectors.toMap(NaptanRecord::getRailStationId, NaptanRecord::getId));
        }

        @Override
        public Class<NaptanRecord> getDataType() {
            return NaptanRecord.class;
        }

        public void clear() {
            stops.clear();
            tiplocToAtco.clear();
        }

        public long size() {
            return stops.size();
        }

        public Map<IdFor<NPTGLocality>, ImmutableIdSet<NaptanRecord>> getLocatlities() {
            final Collector<NaptanRecord, IdSet<NaptanRecord>, ImmutableIdSet<NaptanRecord>> collector = ImmutableIdSet.collector();
            return stops.getValuesStream().collect(Collectors.groupingBy(NaptanRecord::getLocalityId, collector));
        }

        public boolean hasId(final IdFor<NaptanRecord> id) {
            return stops.hasId(id);
        }

        public NaptanRecord get(final IdFor<NaptanRecord> id) {
            return stops.get(id);
        }

        public Stream<NaptanRecord> getAll() {
            return stops.getValuesStream();
        }

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

        public boolean hasTiploc(final IdFor<Station> tiploc) {
            return tiplocToAtco.containsKey(tiploc);
        }

        private NaptanRecord createRecord(final NaptanStopData original) {
            final IdFor<NaptanRecord> atcoCode = original.getAtcoCode();
            final NaptanStopType stopType = original.getStopType();

            final String rawLocalityCode = original.getNptgLocality();
            final IdFor<NPTGLocality> localityCode = NPTGLocality.createId(rawLocalityCode);

            String suburb = original.getSuburb();
            String town = original.getTown();

            if (nptgRepository.hasLocality(localityCode)) {
                final NPTGLocality locality = nptgRepository.get(localityCode);
                if (town.isBlank()) {
                    town = getTownFrom(locality);
                }
                if (suburb.isBlank()) {
                    suburb = getSuburb(locality);
                }
            } else {
                String msg = format("Naptan localityCode '%s' missing from nptg for acto %s and type %s", localityCode, atcoCode, original.getStopType());
                if (stopTypesForRail.contains(stopType)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(msg);
                    }
                } else {
                    logger.warn(msg);
                }
            }

            final IdFor<Station> railStationId;
            if (original.hasRailInfo()) {
                railStationId = Station.createId(original.getRailInfo().getTiploc());
                tiplocToAtco.put(railStationId, atcoCode);
            } else {
                railStationId = IdFor.invalid(Station.class);
            }

            return new NaptanRecord(atcoCode, localityCode, original.getCommonName(), original.getGridPosition(),
                    original.getLatLong(), suburb, town, stopType, original.getStreet(), original.getIndicator(),
                    original.isLocalityCentre(), railStationId);
        }

        private String getSuburb(final NPTGLocality locality) {
            if (locality.getParentLocalityName().isEmpty()) {
                // not a suburb/subunit of somewhere else
                return "";
            } else {
                return locality.getLocalityName();
            }
        }

        private String getTownFrom(final NPTGLocality locality) {
            final String parentLocalityName = locality.getParentLocalityName();
            if (parentLocalityName.isEmpty()) {
                return locality.getLocalityName();
            } else {
                return parentLocalityName;
            }
        }

        public void diagnostics() {
            logger.info("Loaded " + stops.size() + " stops");
            logger.info("Loaded " + tiplocToAtco.size() + " mappings for rail stations");
        }
    }
}
