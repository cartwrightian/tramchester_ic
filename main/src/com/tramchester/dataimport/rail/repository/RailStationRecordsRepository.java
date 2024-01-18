package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ProvidesRailStationRecords;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Supports loading of rail station data only, use StationRepository in all other cases
 */
@LazySingleton
public class RailStationRecordsRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailStationRecordsRepository.class);

    private final IdSet<Station> inUseStations;
    private final Map<String, MutableStation> tiplocMap; // rail timetable id -> mutable station
    private final Set<String> missing;
    private final ProvidesRailStationRecords providesRailStationRecords;
    private final RailStationCRSRepository crsRepository;
    private final NaptanRepository naptanRepository;
    private final boolean enabled;

    @Inject
    public RailStationRecordsRepository(ProvidesRailStationRecords providesRailStationRecords, RailStationCRSRepository crsRepository,
                                        NaptanRepository naptanRepository, TramchesterConfig config) {
        this.providesRailStationRecords = providesRailStationRecords;
        this.crsRepository = crsRepository;
        this.naptanRepository = naptanRepository;
        inUseStations = new IdSet<>();
        tiplocMap = new HashMap<>();
        missing = new HashSet<>();
        enabled = config.hasRailConfig();
    }

    @PostConstruct
    public void start() {
        if (enabled) {
            logger.info("start");
            loadStations(providesRailStationRecords.load());
            logger.info("started");
        }
    }

    @PreDestroy
    private void close() {
        if (enabled) {
            if (!missing.isEmpty()) {
                logger.warn("Missing station locations that were referenced in timetable " + missing);
            }
            missing.clear();
            inUseStations.clear();
            tiplocMap.clear();
        }
    }

    private void loadStations(Stream<PhysicalStationRecord> physicalRecords) {

        Stream<Pair<MutableStation, PhysicalStationRecord>> railStations = physicalRecords.
                filter(this::validRecord).
                map(record -> Pair.of(createStationFor(record), record));

        railStations.forEach(railStationPair -> {
            final MutableStation mutableStation = railStationPair.getLeft();
            PhysicalStationRecord physicalStationRecord = railStationPair.getValue();
            addStation(mutableStation, physicalStationRecord.getTiplocCode());
            crsRepository.putCRS(mutableStation, physicalStationRecord.getCRS());
        });

        logger.info("Initially loaded " + tiplocMap.size() + " stations" );

    }

    private boolean validRecord(PhysicalStationRecord physicalStationRecord) {
        if (physicalStationRecord.getName().isEmpty()) {
            logger.warn("Invalid record " + physicalStationRecord);
            return false;
        }
        if (physicalStationRecord.getNorthing()==Integer.MAX_VALUE) {
            logger.warn("Invalid record " + physicalStationRecord);
            return false;
        }
        if (physicalStationRecord.getEasting()==Integer.MAX_VALUE) {
            logger.warn("Invalid record " + physicalStationRecord);
            return false;
        }
        return true;
    }

    private MutableStation createStationFor(final PhysicalStationRecord record) {
        final IdFor<Station> stationId = Station.createId(record.getTiplocCode());

        GridPosition grid = GridPosition.Invalid;
        LatLong latLong = LatLong.Invalid;

        final IdFor<NPTGLocality> areaId;
        final boolean isCentral;
        if (naptanRepository.containsTiploc(stationId)) {
            // prefer naptan data if available
            final NaptanRecord naptanRecord = naptanRepository.getForTiploc(stationId);
            areaId = naptanRecord.getLocalityId();
            grid = naptanRecord.getGridPosition();
            latLong = naptanRecord.getLatLong();
            isCentral = naptanRecord.isLocalityCenter();
        } else {
            areaId = NPTGLocality.InvalidId();
            isCentral = false;
        }

        if (!grid.isValid()) {
            // not from naptan, try to get from rail data
            if (record.getEasting() == Integer.MIN_VALUE || record.getNorthing() == Integer.MIN_VALUE) {
                grid = GridPosition.Invalid;
            } else {
                grid = convertToOsGrid(record.getEasting(), record.getNorthing());
                latLong = CoordinateTransforms.getLatLong(grid); // re-calc from rail grid
            }
        }

        final Duration minChangeTime = Duration.ofMinutes(record.getMinChangeTime());

        final String name = record.getName();

        final boolean isInterchange = (record.getRailInterchangeType()!= RailInterchangeType.None);

        return new MutableStation(stationId, areaId, name, latLong, grid, DataSourceID.rail, isInterchange,
                minChangeTime, isCentral);
    }

    private GridPosition convertToOsGrid(int easting, int northing) {
        return new GridPosition(easting* 100L, northing* 100L);
    }

    private void addStation(MutableStation mutableStation, String tipLoc) {
        tiplocMap.put(tipLoc, mutableStation);
    }

    public Set<MutableStation> getInUse() {
        return tiplocMap.values().stream().
                filter(station -> inUseStations.contains(station.getId())).
                collect(Collectors.toSet());
    }

    public void markAsInUse(Station station) {
        inUseStations.add(station.getId());
    }

    public int count() {
        return tiplocMap.size();
    }

    public int countNeeded() {
        return inUseStations.size();
    }

    public boolean hasStationRecord(RailLocationRecord record) {
        final String tiplocCode = record.getTiplocCode();
        boolean found = tiplocMap.containsKey(tiplocCode);
        if (!found) {
            missing.add(record.getTiplocCode());
        }
        return found;
    }

    public MutableStation getMutableStationFor(RailLocationRecord record) {
        final String tiplocCode = record.getTiplocCode();
        return tiplocMap.get(tiplocCode);
    }

    /***
     * diagnostic support only
     * @param tiploc must be a valid tiploc
     * @return the matching station
     */
    public Station getMutableStationForTiploc(IdFor<Station> tiploc) {
        final String tiplocAsText = tiploc.getGraphId();
        return tiplocMap.get(tiplocAsText);
    }
}
