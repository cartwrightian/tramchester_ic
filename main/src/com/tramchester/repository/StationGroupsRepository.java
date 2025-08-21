package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationLocalityGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static java.lang.String.format;

/***
 * Wrap stations with same area id inside a group so at API/UI level see unique list of station names
 * Relies on naptan and NPTG data for grouping
 */
@LazySingleton
public class StationGroupsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationGroupsRepository.class);

    // TODO this should be zero?
    // Group needs to have more than this many stations to be includes
    public static final int NUMBER_OF_STATIONS_THRESHOLD = 1;

    private final TramchesterConfig config;
    private final NaptanRepository naptanRepository;
    private final NPTGRepository nptgRepository;
    private final StationRepository stationRepository;
    private final GraphFilter graphFilter;

    //private final boolean enabled;

    private final Map<IdFor<StationLocalityGroup>, StationLocalityGroup> stationGroups;
    private final Map<String, StationLocalityGroup> stationGroupsByName;

    @Inject
    public StationGroupsRepository(StationRepository stationRepository, TramchesterConfig config,
                                   NPTGRepository nptgRepository, NaptanRepository naptanRepository,
                                   GraphFilter graphFilter) {
        this.config = config;
        this.nptgRepository = nptgRepository;
        //this.enabled = naptanRepository.isEnabled();

        this.stationRepository = stationRepository;
        this.naptanRepository = naptanRepository;
        this.graphFilter = graphFilter;

        stationGroups = new HashMap<>();
        stationGroupsByName = new HashMap<>();
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        stationGroups.clear();
        stationGroupsByName.clear();
        logger.info("Stopped");
    }

    @PostConstruct
    public void start() {
        if (!naptanRepository.isEnabled()) {
            logger.warn("Naptan is disabled, cannot find grouped stations, need areaId and areaName from naptan");
            return;
        }

        logger.info("starting");
        final List<GTFSSourceConfig> dataSources = config.getGTFSDataSource();
        dataSources.forEach(dataSource -> {
            final Set<TransportMode> modesToGroupStations = dataSource.groupedStationModes();
            if (!modesToGroupStations.isEmpty()) {
                populateFor(dataSource, modesToGroupStations);
            } else {
                logger.warn("Not adding " + dataSource.getName() + " since no station group modes");
            }
        });
        String message = format("Loaded %s groups and %s names", stationGroupsByName.size(), stationGroups.size());
        if (stationGroups.isEmpty() || stationGroupsByName.isEmpty()) {
            logger.warn(message);
        } else {
            logger.info(message);
        }
        logger.info("started");
    }

    private void populateFor(final GTFSSourceConfig dataSource, final Set<TransportMode> enabledModes) {
        if (graphFilter.isFiltered()) {
            logger.warn("Filtering is enabled");
        }

        // guard for mis-config
        if (dataSource.getDataSourceId()==DataSourceID.tfgm && enabledModes.contains(Bus)) {
            if (!naptanRepository.isEnabled()) {
                String msg = "Naptan config not present in remoteSources, it is required when Bus is enabled for " + dataSource;
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        }

        logger.info("Populating for source:" + dataSource.getDataSourceId() + " modes:" + enabledModes);
        enabledModes.forEach(mode -> capture(dataSource.getDataSourceId(), mode) );
    }

    private void capture(final DataSourceID dataSourceID, final TransportMode mode) {

        Map<IdFor<NPTGLocality>, Set<Station>> groupedByAreaId = stationRepository.getStationsFromSource(dataSourceID).
                filter(graphFilter::shouldInclude).
                filter(station -> station.servesMode(mode)).
                filter(station -> station.getLocalityId().isValid()).
                collect(Collectors.groupingBy(Station::getLocalityId, Collectors.toSet()));

        groupedByAreaId.entrySet().stream().
                filter(item -> item.getValue().size() > NUMBER_OF_STATIONS_THRESHOLD).
                forEach(item -> {
                    final Set<Station> stationsInArea = item.getValue();
                    stationsInArea.forEach(station ->  addStationGroup(item.getKey(), stationsInArea, groupedByAreaId));
                });

        if (stationGroups.isEmpty()) {
            logger.warn("No station groups created from " + groupedByAreaId.size());
        } else {
            logger.info("Created " + stationGroups.size() + " grouped stations from " + groupedByAreaId.size());
        }
    }

    private void addStationGroup(final IdFor<NPTGLocality> localityId, final Set<Station> stationsToGroup,
                                 final Map<IdFor<NPTGLocality>, Set<Station>> groupedByAreaId) {

        final String areaName;
        final NPTGLocality locality;
        if (nptgRepository.hasLocality(localityId)) {
            locality = nptgRepository.get(localityId);
            areaName = locality.getLocalityName();
        } else {
            String message = "Missing locality " + localityId;
            logger.error(message);
            throw new RuntimeException(message);
//            areaName  = localityId.toString();
//            locality = null;
//            logger.error(format("Using %s as name, missing area code %s for station group %s", areaName, localityId, HasId.asIds(stationsToGroup)));
        }

        final IdFor<NPTGLocality> parentId = getParentIdFor(locality, groupedByAreaId);

        final StationLocalityGroup stationGroup = new StationLocalityGroup(stationsToGroup, localityId, areaName, parentId, locality.getLatLong());

        stationGroups.put(stationGroup.getId(), stationGroup);
        stationGroupsByName.put(areaName, stationGroup);
    }

    private IdFor<NPTGLocality> getParentIdFor(final NPTGLocality locality, final Map<IdFor<NPTGLocality>, Set<Station>> groupedByAreaId) {
        if (locality==null) {
            return NPTGLocality.InvalidId();
        }

        final IdFor<NPTGLocality> parentId = locality.getParentLocalityId();

        if (!parentId.isValid()) {
            return NPTGLocality.InvalidId();
        }

        if (!groupedByAreaId.containsKey(parentId)) {
            return NPTGLocality.InvalidId();
        }

        if (groupedByAreaId.get(parentId).size()<= NUMBER_OF_STATIONS_THRESHOLD) {
            return NPTGLocality.InvalidId();
        }

        return parentId;
    }

    private void guardIsEnabled() {
        if (naptanRepository.isEnabled()) {
            return;
        }
        String msg = "Station Group Repository is disabled";
        logger.error(msg);
        throw new RuntimeException(msg);
    }

    public Set<StationLocalityGroup> getStationGroupsFor(final TransportMode mode) {
        guardIsEnabled();
        return stationGroups.values().stream().
                filter(station -> station.getTransportModes().contains(mode)).
                collect(Collectors.toSet());
    }

    public StationLocalityGroup findByName(final String name) {
        guardIsEnabled();
        return stationGroupsByName.get(name);
    }

    public Set<StationLocalityGroup> getAllGroups() {
        guardIsEnabled();
        return new HashSet<>(stationGroups.values());
    }

    public StationLocalityGroup getStationGroup(final IdFor<StationLocalityGroup> stationGroupId) {
        guardIsEnabled();
        return stationGroups.get(stationGroupId);
    }

    public StationLocalityGroup getStationGroupForArea(final IdFor<NPTGLocality> areaId) {
        return getStationGroup(StationLocalityGroup.createId(areaId));
    }

    public boolean hasGroup(final IdFor<StationLocalityGroup> id) {
        // no guard here as need to handle situation where Group is set in cookie but groups no longer enabled
        if (isEnabled()) {
            return stationGroups.containsKey(id);
        } else {
            logger.info("Returning false for hasGroup when disabled for " + id);
            return false;
        }
    }

    public boolean hasArea(final IdFor<NPTGLocality> areaId) {
        return hasGroup(StationLocalityGroup.createId(areaId));
    }

    public boolean isEnabled() {
        return naptanRepository.isEnabled();
    }



}
