package com.tramchester.repository.naptan;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
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
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.geo.MarginInMeters;
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

import static com.tramchester.domain.reference.TransportMode.Train;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

// http://naptan.dft.gov.uk/naptan/schema/2.5/doc/NaPTANSchemaGuide-2.5-v0.67.pdf

@LazySingleton
public class NaptanRepositoryContainer implements NaptanRepository {
    private static final Logger logger = LoggerFactory.getLogger(NaptanRepositoryContainer.class);

    private static final EnumSet<NaptanStopType> stopTypesForRail =
            EnumSet.copyOf(NaptanStopType.getTypesFor(Train));

    private final NaptanDataImporter naptanDataImporter;
    private final NPTGRepository nptgRepository;
    private final Geography geography;
    private final TramchesterConfig config;

    private final IdMap<NaptanRecord> stops;
    private final Map<IdFor<Station>, IdFor<NaptanRecord>> tiplocToAtco;

    private Map<IdFor<NPTGLocality>, ImmutableIdSet<NaptanRecord>> localities;

    @Inject
    public NaptanRepositoryContainer(NaptanDataImporter naptanDataImporter, NPTGRepository nptgRepository,
                                     Geography geography, TramchesterConfig config) {
        this.naptanDataImporter = naptanDataImporter;
        this.nptgRepository = nptgRepository;
        this.geography = geography;
        this.config = config;
        stops = new IdMap<>();
        tiplocToAtco = new HashMap<>();
        localities = Collections.emptyMap();
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

        final BoundingBox bounds = config.getBounds();
        final MarginInMeters margin = config.getWalkingDistanceRange();

        logger.info("Loading data for " + bounds + " and range " + margin);

        final Receiver receiver = new Receiver(config, this::createRecordForStop);

        naptanDataImporter.loadData(receiver);

        populateLocalityMap();

        logger.info("Loaded " + stops.size() + " stops");
        logger.info("Loaded " + tiplocToAtco.size() + " mappings for rail stations");
        logger.info("Loaded " + localities.size() + " localities");

    }

    private void populateLocalityMap() {
        final Collector<NaptanRecord, IdSet<NaptanRecord>, ImmutableIdSet<NaptanRecord>> collector = ImmutableIdSet.collector();
        localities = stops.getValuesStream().collect(Collectors.groupingBy(NaptanRecord::getLocalityId, collector));
    }

    private void createRecordForStop(final NaptanStopData stopData) {
        final NaptanRecord record = createRecord(stopData);

        stops.add(record);

        if (stopData.hasRailInfo()) {
            final IdFor<Station> id = Station.createId(stopData.getRailInfo().getTiploc());
            tiplocToAtco.put(id, stopData.getAtcoCode());
        }
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

        return new NaptanRecord(atcoCode, localityCode, original.getCommonName(), original.getGridPosition(), original.getLatLong(),
                suburb, town, stopType, original.getStreet(), original.getIndicator(), original.isLocalityCentre());
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
        return stops.getValuesStream();
    }

    /***
     * Look up via train location code
     * @param railStationTiploc the code for the station
     * @return data if present, null otherwise
     */
    @Override
    public NaptanRecord getForTiploc(final IdFor<Station> railStationTiploc) {
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
    public boolean containsTiploc(final IdFor<Station> tiploc) {
        return tiplocToAtco.containsKey(tiploc);
    }

    @Override
    public boolean containsLocality(final IdFor<NPTGLocality> id) {
        return localities.containsKey(id);
    }

    /***
     * Naptan records for an area. For stations in an area use StationLocations.
     * @see com.tramchester.geo.StationLocations
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

        protected boolean shouldInclude(final NaptanStopData stopData) {
            if (!stopData.hasValidAtcoCode()) {
                return false;
            }

            if (!"active".equals(stopData.getStatus())) {
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
}
