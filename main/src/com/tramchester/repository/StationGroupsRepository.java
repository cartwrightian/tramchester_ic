package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.repository.naptan.NaptanRepository;
import com.tramchester.repository.nptg.NPTGRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static java.lang.String.format;

/***
 * Wrap stations with same area id inside a group so at API/UI level see unique list of station names
 * TODO area ref id is missing for some stations, need to find an alternative or different way to do this?
 */
@LazySingleton
public class StationGroupsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationGroupsRepository.class);

    private final TramchesterConfig config;
    private final NaptanRepository naptanRepository;
    private final NPTGRepository nptgRepository;
    private final StationRepository stationRepository;
    private final GraphFilter graphFilter;

    private final boolean enabled;

    private final Map<IdFor<NPTGLocality>, StationGroup> stationGroups;
    private final Map<String, StationGroup> stationGroupsByName;

    @Inject
    public StationGroupsRepository(StationRepository stationRepository, TramchesterConfig config,
                                   NPTGRepository nptgRepository, NaptanRepository naptanRepository,
                                   GraphFilter graphFilter) {
        this.config = config;
        this.nptgRepository = nptgRepository;
        this.enabled = naptanRepository.isEnabled();

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
        if (!enabled) {
            logger.warn("Naptan is disabled, cannot find grouped stations, need areaId and areaName from naptan");
            return;
        }

        logger.info("starting");
        List<GTFSSourceConfig> dataSources = config.getGTFSDataSource();
        dataSources.forEach(dataSource -> {
            final Set<TransportMode> modesToGroupStations = dataSource.groupedStationModes();
            if (!modesToGroupStations.isEmpty()) {
                populateFor(dataSource, modesToGroupStations);
            } else {
                logger.warn("Not adding " + dataSource.getName() + " since no composite station modes");
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

    private void populateFor(GTFSSourceConfig dataSource, Set<TransportMode> enabledModes) {
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
                filter(item -> item.getValue().size() > 1).
                forEach(item -> groupByAreaAndAdd(item.getKey(), item.getValue()));

        logger.info("Created " + stationGroups.size() + " composite stations from " + groupedByAreaId.size());
    }

    private void groupByAreaAndAdd(IdFor<NPTGLocality> areaId, Set<Station> stationsInArea) {
        stationsInArea.forEach(station ->  addComposite(areaId, stationsInArea));
    }

    private void addComposite(IdFor<NPTGLocality> areaId, Set<Station> stationsToGroup) {

        String areaName  = areaId.toString();
        if (nptgRepository.hasLocaility(areaId)) {
            NPTGLocality locality = nptgRepository.get(areaId);
            areaName = locality.getLocalityName();
        } else {
            logger.error(format("Using %s as name, missing area code %s for station group %s", areaName, areaId, HasId.asIds(stationsToGroup)));
        }

        StationGroup stationGroup = new StationGroup(stationsToGroup, areaId, areaName);

        stationGroups.put(areaId, stationGroup);
        stationGroupsByName.put(areaName, stationGroup);
    }

    private void guardIsEnabled() {
        if (enabled) {
            return;
        }
        String msg = "Repository is disabled";
        logger.error(msg);
        throw new RuntimeException(msg);
    }

    public Set<StationGroup> getStationGroupsFor(TransportMode mode) {
        guardIsEnabled();
        return stationGroups.values().stream().
                filter(station -> station.getTransportModes().contains(mode)).
                collect(Collectors.toSet());
    }

    public StationGroup findByName(String name) {
        guardIsEnabled();
        return stationGroupsByName.get(name);
    }

    public Set<StationGroup> getAllGroups() {
        guardIsEnabled();
        return new HashSet<>(stationGroups.values());
    }

    public StationGroup getStationGroup(IdFor<NPTGLocality> areaId) {
        guardIsEnabled();
        return stationGroups.get(areaId);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
