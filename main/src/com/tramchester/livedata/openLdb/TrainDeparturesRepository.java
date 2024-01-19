package com.tramchester.livedata.openLdb;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.thalesgroup.rtti._2015_11_27.ldb.types.ArrayOfServiceLocations;
import com.thalesgroup.rtti._2015_11_27.ldb.types.ServiceLocation;
import com.thalesgroup.rtti._2017_10_01.ldb.types.CoachData;
import com.thalesgroup.rtti._2017_10_01.ldb.types.FormationData;
import com.thalesgroup.rtti._2017_10_01.ldb.types.ServiceItem;
import com.thalesgroup.rtti._2017_10_01.ldb.types.StationBoard;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.DeparturesRepository;
import com.tramchester.livedata.repository.UpcomingDeparturesCache;
import com.tramchester.livedata.repository.UpcomingDeparturesSource;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@LazySingleton
public class TrainDeparturesRepository implements UpcomingDeparturesSource {
    private static final Logger logger = LoggerFactory.getLogger(TrainDeparturesRepository.class);

    private final UpcomingDeparturesCache departuresCache;
    private final LiveTrainDepartures liveTrainDepartures;
    private final TramchesterConfig config;

    @Inject
    public TrainDeparturesRepository(TrainDeparturesDataFetcher dataFetcher, AgencyRepository agencyRepository,
                                     CRSRepository crsRepository, StationRepository stationRepository, CacheMetrics cacheMetrics,
                                     TramchesterConfig config) {
        this.config = config;
        final Duration cacheDuration = DeparturesRepository.TRAIN_WINDOW;
        long cacheSize = stationRepository.getNumberOfStations(DataSourceID.rail, TransportMode.Train);

        liveTrainDepartures = new LiveTrainDepartures(dataFetcher, agencyRepository, crsRepository);
        departuresCache = new UpcomingDeparturesCache(cacheSize, cacheDuration, cacheMetrics);
    }

    @PostConstruct
    public void start() {
        if (config.liveTrainDataEnabled()) {
            logger.info("starting");
            departuresCache.start();
            logger.info("started");
        } else {
            logger.info("Disabled");
        }
    }

    @Override
    public List<UpcomingDeparture> forStation(final Station station) {
        if (!config.liveTfgmTramDataEnabled()) {
            String message = "Live train data is not enabled";
            logger.error(message);
            throw new RuntimeException(message);
        }
        logger.info("Get departures for " + station.getId());
        return departuresCache.getOrUpdate(station, liveTrainDepartures::forStation);
    }

    private static class LiveTrainDepartures implements UpcomingDeparturesSource {
        private final TrainDeparturesDataFetcher dataFetcher;
        private final AgencyRepository agencyRepository;
        private final CRSRepository crsRepository;

        private LiveTrainDepartures(TrainDeparturesDataFetcher dataFetcher, AgencyRepository agencyRepository, CRSRepository crsRepository) {
            this.dataFetcher = dataFetcher;
            this.agencyRepository = agencyRepository;
            this.crsRepository = crsRepository;
        }

        public List<UpcomingDeparture> forStation(final Station station) {
            logger.debug("Get live departures for " + station.getId());
            List<UpcomingDeparture> result = new ArrayList<>();
            final Optional<StationBoard> maybeBoard = dataFetcher.getFor(station);
            maybeBoard.ifPresent(board -> {
                final LocalDateTime generated = getDate(board);
                result.addAll(board.getTrainServices().getService().stream().
                        map(serviceItem -> map(serviceItem, station, generated)).
                        toList());
            });
            logger.info("Got " + result.size() + " departures for " + station.getId());
            return result;
        }

        private LocalDateTime getDate(final StationBoard board) {
            final XMLGregorianCalendar generated = board.getGeneratedAt();

            final LocalDate date = LocalDate.of(generated.getYear(), generated.getMonth(), generated.getDay());
            final LocalTime time = LocalTime.of(generated.getHour(), generated.getMinute(), generated.getSecond());

            return LocalDateTime.of(date, time);
        }

        private UpcomingDeparture map(final ServiceItem serviceItem, final Station displayLocation, final LocalDateTime generated) {

            final String carridges = carridgesFrom(serviceItem.getFormation());
            final Agency agency = agencyFrom(serviceItem.getOperatorCode());
            final Station destination = destinationFrom(serviceItem.getDestination());
            final String status = getStatus(serviceItem);
            final TramTime when = getWhen(serviceItem);

            return new UpcomingDeparture(generated.toLocalDate(), displayLocation,
                    destination, status, when, carridges, agency, TransportMode.Train);

        }

        private TramTime getWhen(final ServiceItem serviceItem) {
            final String std = serviceItem.getStd();
            logger.debug("Get when from " + std);
            final LocalTime departureTime = LocalTime.parse(std);

            final TramTime rounded = TramTime.ofHourMins(departureTime);

            logger.debug("When Duration is " + departureTime + " and rounded is " + rounded);
            return rounded;
        }

        private String getStatus(final ServiceItem serviceItem) {
            final String etd = serviceItem.getEtd();
            logger.debug("Get status from " + etd);
            // todo if not 'On time', then there is a delay?
            return etd;
        }

        // TODO Difference between wait and when above??
        private Duration getWait(final LocalDateTime generated, final ServiceItem serviceItem) {
            final String std = serviceItem.getStd();
            logger.info("Get wait from " + std);
            LocalTime departureTime = LocalTime.parse(std);

            final Duration duration = Duration.between(generated.toLocalTime(), departureTime);
            // TODO
            // Right now don't store seconds in TramTime so need to round
            long minutes = duration.toMinutes();
            final long remainder = duration.minusMinutes(minutes).getSeconds();
            if (remainder > 30) {
                minutes = minutes + 1;
            }
            final Duration rounded = Duration.ofMinutes(minutes);
            logger.info("Wait Duration is " + duration + " and rounded is " + rounded);
            return rounded;
        }

        private Station destinationFrom(final ArrayOfServiceLocations destination) {
            final List<ServiceLocation> dests = destination.getLocation();
            if (dests.size() > 1) {
                logger.warn("Number of destinations was " + dests.size());
            }
            final String crs = dests.get(0).getCrs();
            logger.debug("Find destination from " + crs);

            if (!crsRepository.hasCrs(crs)) {
                return MutableStation.Unknown(DataSourceID.rail);
            }
            return crsRepository.getFor(crs);
        }

        private Agency agencyFrom(final String operatorCode) {
            logger.debug("Find agency from " + operatorCode);
            return agencyRepository.get(Agency.createId(operatorCode));
        }

        private String carridgesFrom(final FormationData formation) {
            if (formation == null) {
                // common
                logger.debug("Formation missing");
                return "Unknown formation";
            }
            if (formation.getCoaches() == null) {
                logger.info("Unknown formation, coaches");
                return "Unknown formation";
            }
            // TODO Pass more detail here?
            final List<CoachData> coaches = formation.getCoaches().getCoach();
            return "Formed by " + coaches.size() + " coaches";
        }
    }


}
