package com.tramchester.livedata.tfgm;


import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Agency;
import com.tramchester.domain.MutableAgency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.factory.TransportEntityFactoryForTFGM;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepositoryPublic;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@LazySingleton
public class TramDepartureFactory {
    private static final Logger logger = LoggerFactory.getLogger(TramDepartureFactory.class);

    private final AgencyRepository agencyRepository;
    private final StationRepositoryPublic stationRepository;
    private final PlatformRepository platformRepository;
    private Agency agency;

    @Inject
    public TramDepartureFactory(AgencyRepository agencyRepository, StationRepositoryPublic stationRepository, PlatformRepository platformRepository) {
        this.agencyRepository = agencyRepository;
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
    }

    @PostConstruct
    public void start() {
        agency = agencyRepository.get(MutableAgency.METL);
    }

    public TramStationDepartureInfo createStationDeparture(BigDecimal displayId, Lines line, LineDirection direction, String atcoCode, String message,
                                                           LocalDateTime updateTime) {
        final Optional<Station> maybeStation = getStationByAtcoCode(atcoCode);
        return maybeStation.map(station -> createWithPlatform(displayId, line, direction, message, updateTime, station, atcoCode)).orElse(null);
    }

    private TramStationDepartureInfo createWithPlatform(BigDecimal displayId, Lines line, LineDirection direction, final String message,
                                                        LocalDateTime updateTime, Station station, String atcoCode) {
        Platform platform = getPlatform(station, atcoCode);
        if (platform!=null) {
            return new TramStationDepartureInfo(displayId.toString(), line, direction, station, message, updateTime, platform);
        } else {
            return new TramStationDepartureInfo(displayId.toString(), line, direction, station, message, updateTime);
        }
    }

    private Platform getPlatform(Station station, String atcoCode) {

        final IdFor<Platform> platformId = getPlatformIdFor(station, atcoCode);
        if (platformRepository.hasPlatformId(platformId)) {
            return platformRepository.getPlatformById(platformId);
        } else {
            logger.warn("Could not find platform for " + atcoCode);
            return null;
        }
    }

    private Optional<Station> getStationByAtcoCode(final String atcoCode) {
        final IdFor<Station> stationId = TransportEntityFactoryForTFGM.getStationIdFor(atcoCode);
        if (stationRepository.hasStationId(stationId)) {
            return Optional.of(stationRepository.getStationById(stationId));
        } else {
            logger.warn("Could not find station for " + atcoCode);
            return Optional.empty();
        }
    }

    private IdFor<Platform> getPlatformIdFor(final Station station, String atcoCode) {
        if ("9400ZZMATRC1".equals(atcoCode)) {
            // trafford park platform workaround
            atcoCode = "9400ZZMATRC2";
        }
        return TransportEntityFactoryForTFGM.createPlatformId(station.getId(), atcoCode);
    }

    public UpcomingDeparture createDueTram(TramStationDepartureInfo departureInfo, String status, Station station,
                                           int waitInMinutes, String carriages) {
        final LocalDateTime updateDateTime = departureInfo.getLastUpdate();
        final LocalTime updateTime = updateDateTime.toLocalTime();
        final LocalDate updateDate = updateDateTime.toLocalDate();
        final Station displayLocation = departureInfo.getStation();

        final TramTime when = TramTime.ofHourMins(updateTime.plusMinutes(waitInMinutes));

        final UpcomingDeparture dueTram = new UpcomingDeparture(updateDate, displayLocation, station, status,
                when, carriages, agency, TransportMode.Tram);
        if (departureInfo.hasStationPlatform()) {
            dueTram.setPlatform(departureInfo.getStationPlatform());
        }

        return dueTram;
    }
}