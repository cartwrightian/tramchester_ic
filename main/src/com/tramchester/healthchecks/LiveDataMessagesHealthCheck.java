package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TfgmTramLiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

import static java.lang.String.format;

@LazySingleton
public class LiveDataMessagesHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataMessagesHealthCheck.class);

    private final PlatformMessageRepository repository;
    private final ProvidesNow currentTimeProvider;
    private final TfgmTramLiveDataConfig config;
    private final StationRepository stationRepository;
    private int numberOfStations;

    @Inject
    public LiveDataMessagesHealthCheck(TramchesterConfig config, PlatformMessageRepository repository,
                                       ProvidesNow currentTimeProvider, StationRepository stationRepository,
                                       ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.config = config.getLiveDataConfig();
        this.repository = repository;
        this.currentTimeProvider = currentTimeProvider;
        this.stationRepository = stationRepository;
    }

    @PostConstruct
    public void start() {
        // TODO Correct way to know which count to get?
        numberOfStations = (int) stationRepository.getNumberOfStations(DataSourceID.tfgm, TransportMode.Tram);
    }

    @Override
    public boolean isEnabled() {
        return config!=null;
    }

    // normally only between 2 and 4 missing
    // private static final int MISSING_MSGS_LIMIT = 4;
    // during night hours gradually goes to zero than back to full about 6.05am

    @Override
    public Result check() {
        logger.debug("Checking live data health");
        int entries = repository.numberOfEntries();

        if (entries==0) {
            String msg = "No entires present";
            logger.warn(msg);
            return Result.unhealthy(msg);
        }

        LocalDateTime dateTime = currentTimeProvider.getDateTime();
        int stationsWithMessages = repository.numberStationsWithMessages(dateTime);

        int offset = numberOfStations - stationsWithMessages;
        boolean lateNight = isLateNight(dateTime);

        if (offset > config.getMaxNumberStationsWithoutMessages()) {
            if (!lateNight) {
                String message = format("Not enough messages present, %s entries, %s out of %s stations",
                        entries, stationsWithMessages, numberOfStations);
                logger.warn(message);
                return Result.unhealthy(message);
            }
        }

        String msg = format("Live data messages healthy with %s entries for %s out of %s stations ",
                entries, stationsWithMessages, numberOfStations);
        logger.info(msg);
        return Result.healthy(msg);
    }

    @Override
    public String getName() {
        return "liveDataMessages";
    }

}
