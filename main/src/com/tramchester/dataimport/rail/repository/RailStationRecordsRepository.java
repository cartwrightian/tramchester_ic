package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ProvidesRailStationRecords;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.TIPLOCInsert;
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
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
    private final Map<String, MutableStation> tiplocToStation; // rail timetable id -> mutable station
    private final Map<String, TIPLOCInsert> tiplocToTiplocInsert;
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
        tiplocToStation = new HashMap<>();
        tiplocToTiplocInsert = new HashMap<>();
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
            tiplocToStation.clear();
        }
    }

    private void loadStations(final Stream<PhysicalStationRecord> physicalRecords) {

        Stream<Pair<MutableStation, PhysicalStationRecord>> railStations = physicalRecords.
                filter(this::validRecord).
                map(record -> Pair.of(createStationFor(record), record));

        railStations.forEach(railStationPair -> {
            final MutableStation mutableStation = railStationPair.getLeft();
            final PhysicalStationRecord physicalStationRecord = railStationPair.getValue();
            addStation(mutableStation, physicalStationRecord.getTiplocCode());
            crsRepository.putCRS(mutableStation, physicalStationRecord.getCRS());
        });

        logger.info("Initially loaded " + tiplocToStation.size() + " stations" );

    }

    private boolean validRecord(final PhysicalStationRecord physicalStationRecord) {
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

    private MutableStation createStationFor(final PhysicalStationRecord stationRecord) {
        final IdFor<Station> stationId = Station.createId(stationRecord.getTiplocCode());

        GridPosition grid = GridPosition.Invalid;
        LatLong latLong = LatLong.Invalid;

        final IdFor<NPTGLocality> areaId;
        final boolean isCentral;
        final String name;
        if (naptanRepository.containsTiploc(stationId)) {
            // prefer naptan data if available
            final NaptanRecord naptanRecord = naptanRepository.getForTiploc(stationId);
            areaId = naptanRecord.getLocalityId();
            grid = naptanRecord.getGridPosition();
            latLong = naptanRecord.getLatLong();
            isCentral = naptanRecord.isLocalityCenter();
            name = naptanRecord.getCommonName();
        } else {
            areaId = NPTGLocality.InvalidId();
            isCentral = false;
            name = stationRecord.getName();
        }

        if (!grid.isValid()) {
            // not from naptan, try to get from rail data
            if (stationRecord.getEasting() == Integer.MIN_VALUE || stationRecord.getNorthing() == Integer.MIN_VALUE) {
                grid = GridPosition.Invalid;
            } else {
                grid = convertToOsGrid(stationRecord.getEasting(), stationRecord.getNorthing());
                latLong = CoordinateTransforms.getLatLong(grid); // re-calc from rail grid
            }
        }

        final Duration minChangeTime = Duration.ofMinutes(stationRecord.getMinChangeTime());

        //final String name = stationRecord.getName();

        final boolean isInterchange = (stationRecord.getRailInterchangeType()!= RailInterchangeType.None);

        return new MutableStation(stationId, areaId, name, latLong, grid, DataSourceID.openRailData, isInterchange,
                minChangeTime, isCentral);
    }

    private GridPosition convertToOsGrid(final int easting, final int northing) {
        return new GridPosition(easting * 100, northing * 100);
    }

    private void addStation(final MutableStation mutableStation, final String tipLoc) {
        tiplocToStation.put(tipLoc, mutableStation);
    }

    public Set<MutableStation> getInUse() {
        return tiplocToStation.values().stream().
                filter(station -> inUseStations.contains(station.getId())).
                collect(Collectors.toSet());
    }

    public void markAsInUse(final Station station) {
        inUseStations.add(station.getId());
    }

    public int count() {
        return tiplocToStation.size();
    }

    public int countNeeded() {
        return inUseStations.size();
    }

    public boolean hasStationRecord(final RailLocationRecord record) {
        final String tiplocCode = record.getTiplocCode();
//        boolean found = tiplocToStation.containsKey(tiplocCode);
        if (tiplocToStation.containsKey(tiplocCode)) {
            return true;
        }
        if (tiplocToTiplocInsert.containsKey(tiplocCode)) {
            return attemptAdd(tiplocToTiplocInsert.get(tiplocCode));
        }
        return false;
//        if (!found) {
//            missing.add(record.getTiplocCode());
//        }
//        return found;
    }

    private boolean attemptAdd(final TIPLOCInsert tiplocInsert) {
        final String tiplocCode = tiplocInsert.getTiplocCode();

        final MutableStation mutableStation = createStationFor(tiplocInsert);

        if (tiplocInsert.isUseful()) {
            crsRepository.putCRS(mutableStation, tiplocInsert.getCRS());
            addStation(mutableStation, tiplocCode);
            return true;
        }

        return false;
    }

    private MutableStation createStationFor(TIPLOCInsert tiplocInsert) {
        final IdFor<Station> stationId = Station.createId(tiplocInsert.getTiplocCode());

        GridPosition grid = GridPosition.Invalid;
        LatLong latLong = LatLong.Invalid;

        final IdFor<NPTGLocality> areaId;
        final boolean isCentral;
        final String name;
        if (naptanRepository.containsTiploc(stationId)) {
            // prefer naptan data if available
            final NaptanRecord naptanRecord = naptanRepository.getForTiploc(stationId);
            areaId = naptanRecord.getLocalityId();
            grid = naptanRecord.getGridPosition();
            latLong = naptanRecord.getLatLong();
            isCentral = naptanRecord.isLocalityCenter();
            name = naptanRecord.getCommonName();
        } else {
            areaId = NPTGLocality.InvalidId();
            isCentral = false;
            name = tiplocInsert.getName();
        }

        boolean isInterchange = false;
        // TODO
        Duration minChangeTime = Duration.ofMinutes(5);

        return new MutableStation(stationId, areaId, name, latLong, grid, DataSourceID.openRailData, isInterchange,
                minChangeTime, isCentral);
    }

    public MutableStation getMutableStationFor(final RailLocationRecord record) {
        final String tiplocCode = record.getTiplocCode();
        return tiplocToStation.get(tiplocCode);
    }

    /***
     * diagnostic support only
     * @param tiploc must be a valid tiploc
     * @return the matching station
     */
    public Station getMutableStationForTiploc(IdFor<Station> tiploc) {
        final String tiplocAsText = tiploc.getGraphId();
        return tiplocToStation.get(tiplocAsText);
    }

    public void add(final TIPLOCInsert tiplocInsert) {
        final String tiploc = tiplocInsert.getTiplocCode();
        if (tiplocToStation.containsKey(tiploc)) {
            // nothing to do here
            return;
        }
        tiplocToTiplocInsert.put(tiplocInsert.getTiplocCode(), tiplocInsert);
    }
}
