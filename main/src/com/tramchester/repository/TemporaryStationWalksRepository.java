package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.TemporaryStationWalk;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.dates.TramDate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class TemporaryStationWalksRepository {
    private static final Logger logger = LoggerFactory.getLogger(TemporaryStationWalksRepository.class);
    private final TramchesterConfig config;
    private final StationRepository stationRepository;

    private final Set<TemporaryStationWalk> walks;

    @Inject
    public TemporaryStationWalksRepository(TramchesterConfig config, StationRepository stationRepository) {
        this.config = config;
        this.stationRepository = stationRepository;
        walks = new HashSet<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        config.getGTFSDataSource().forEach(source -> {
            final List<TemporaryStationsWalkIds> walksFromConfig = source.getTemporaryStationWalks();

            Set<TemporaryStationWalk> walksFromSource = walksFromConfig.stream().
                    map(walkFromConfig -> resolveStations(walkFromConfig, source.getDataSourceId())).collect(Collectors.toSet());
            walks.addAll(walksFromSource);

            if (walks.isEmpty()) {
                logger.info("No closures for " + source.getName());
            }
        });
        logger.warn("Added " + walks.size() + " temporary station walks");
        logger.info("Started");
    }

    private TemporaryStationWalk resolveStations(TemporaryStationsWalkIds temporaryStationWalkIds, DataSourceID dataSourceId) {
        StationPair stationPair = stationRepository.getStationPair(temporaryStationWalkIds.getStationPair());
        return new TemporaryStationWalk(stationPair, temporaryStationWalkIds.getDateRange(), dataSourceId);
    }

    public Set<TemporaryStationWalk> getWalksBetweenFor(final TramDate date) {
        return walks.stream().filter(walk -> walk.getDateRange().contains(date)).
                collect(Collectors.toSet());
    }

    public Set<TemporaryStationWalk> getTemporaryWalksFor(final DataSourceID dataSourceId) {
        return walks.stream().filter(walk -> walk.getDataSourceID().equals(dataSourceId)).
                collect(Collectors.toSet());
    }
}
