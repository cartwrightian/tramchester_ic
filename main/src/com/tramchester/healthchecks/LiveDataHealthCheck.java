package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDateTime;

import static java.lang.String.format;

@LazySingleton
public class LiveDataHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataHealthCheck.class);

    private final TramDepartureRepository repository;
    private final ProvidesNow providesNow;
    private final StationRepository stationRepository;
    private final TfgmTramLiveDataConfig config;

    @Inject
    public LiveDataHealthCheck(TramDepartureRepository repository, ProvidesNow providesNow, StationRepository stationRepository,
                               TramchesterConfig config, ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.config = config.getLiveDataConfig();

        this.repository = repository;
        this.providesNow = providesNow;
        this.stationRepository = stationRepository;
    }

    @Override
    public boolean isEnabled() {
        return config!=null;
    }

    @Override
    public Result check() {
        logger.info("Checking live data health");
        int entries = repository.upToDateEntries();

        String noEntriesPresent = "no entries present";

        if (entries==0) {
            logger.error(noEntriesPresent);
            return Result.unhealthy(noEntriesPresent);
        }

        int numberOfStations = stationRepository.getNumberOfStations();
        LocalDateTime dateTime = providesNow.getDateTime();
        int stationsWithData = repository.getNumStationsWithData(dateTime);
        int offset = numberOfStations-stationsWithData;

        if (offset > config.getMaxNumberStationsWithoutData()) {
            if (!isLateNight(dateTime)) {
                String msg = format("Only %s of %s stations have data, %s entries present", stationsWithData,
                        numberOfStations, entries);

                logger.warn(msg);
                return Result.unhealthy(msg);
            }
        }

        String msg = format("Live data healthy with %s entires and %s of %s stations", entries, stationsWithData,
                numberOfStations);
        logger.info(msg);
        return Result.healthy(msg);
    }

    @Override
    public String getName() {
        return "liveData";
    }

}
