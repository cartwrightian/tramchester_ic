package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.TemporaryStationsWalk;
import com.tramchester.domain.dates.TramDate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class TemporaryStationWalksRepository {
    private static final Logger logger = LoggerFactory.getLogger(TemporaryStationWalksRepository.class);
    private final TramchesterConfig config;
    private final StationRepository stationRepository;

    private final Set<TemporaryStationsWalk> walks;

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
            final List<TemporaryStationsWalk> walksFromConfig = new ArrayList<>(source.getTemporaryStationWalks());
            walks.addAll(walksFromConfig);

            if (walks.isEmpty()) {
                logger.info("No closures for " + source.getName());
            }
        });
        logger.warn("Added " + walks.size() + " temporary station walks");
        logger.info("Started");
    }

    public Set<StationPair> getWalksBetweenFor(final TramDate date) {
        return walks.stream().filter(walk -> walk.getDateRange().contains(date)).
                map(walk -> stationRepository.getStationPair(walk.getStationPair())).
                collect(Collectors.toSet());
    }
}
