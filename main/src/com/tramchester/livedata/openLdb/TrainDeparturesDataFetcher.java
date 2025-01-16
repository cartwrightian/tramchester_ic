package com.tramchester.livedata.openLdb;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.sun.xml.ws.client.ClientTransportException;
import com.thalesgroup.rtti._2013_11_28.token.types.AccessToken;
import com.thalesgroup.rtti._2017_10_01.ldb.GetBoardRequestParams;
import com.thalesgroup.rtti._2017_10_01.ldb.LDBServiceSoap;
import com.thalesgroup.rtti._2017_10_01.ldb.Ldb;
import com.thalesgroup.rtti._2017_10_01.ldb.StationBoardResponseType;
import com.thalesgroup.rtti._2017_10_01.ldb.types.StationBoard;
import com.tramchester.config.OpenLdbConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import jakarta.inject.Inject;
import jakarta.xml.ws.WebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.Optional;

import static java.lang.String.format;

@LazySingleton
public class TrainDeparturesDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(TrainDeparturesDataFetcher.class);

    private final CRSRepository crsRepository;
    private final TramchesterConfig config;

    private LDBServiceSoap soapService;
    private final boolean enabled;
    private AccessToken accessToken;

    private boolean started;

    @Inject
    public TrainDeparturesDataFetcher(TramchesterConfig config, CRSRepository crsRepository) {
        this.crsRepository = crsRepository;
        this.config = config;
        enabled = config.liveTrainDataEnabled();
        started = false;
    }

    @PostConstruct
    public void start() {
        if (enabled) {
            logger.info("starting");
            final OpenLdbConfig openLdbConfig = config.getOpenldbwsConfig();

            started = createSOAPService(openLdbConfig.getWSDLLocation());

            if (started) {
                accessToken = new AccessToken();
                accessToken.setTokenValue(openLdbConfig.getAccessToken());
                logger.info("started");
            }
            else {
                logger.error("Failed to start");
            }
        } else {
            logger.info("Disabled");
        }
    }

    private boolean createSOAPService(final URL wsdlLocation) {
        logger.info("Start SOAP service for " + wsdlLocation);
        try {
            final Ldb soap = new Ldb(wsdlLocation);
            soapService = soap.getLDBServiceSoap12();
            return true;
        }
        catch(WebServiceException webServiceException) {
            logger.error("Unable to start", webServiceException);
            return false;
        }
    }

    public Optional<StationBoard> getFor(final Station station) {
        if (!enabled) {
            logger.error("Attempt to invoke, but not enabled, did start up fail? Station:" + station.getId());
            return Optional.empty();
        }
        if (!started) {
            // restart strategy needed?
            logger.warn("Not started, unable to fetch live data from " + station.getId());
            return Optional.empty();
        }

        if (!station.getTransportModes().contains(TransportMode.Train)) {
            logger.warn("Station is not a train station");
            return Optional.empty();
        }
        if (!crsRepository.hasStation(station)) {
            logger.error("Not CRS Code found for " + station.getId());
            return Optional.empty();
        }
        final String crs = crsRepository.getCRSFor(station);
        logger.info("Get train departures for " + station.getId() + " with CRS " + crs);

        final GetBoardRequestParams params = new GetBoardRequestParams();
        params.setCrs(crs);

        try {
            final StationBoardResponseType departureBoard = soapService.getDepartureBoard(params, accessToken);

            final StationBoard stationBoardResult = departureBoard.getGetStationBoardResult();
            logger.info(format("Got departure board %s at %s for %s", stationBoardResult.getLocationName(),
                    stationBoardResult.getGeneratedAt(), crs));

            return Optional.of(stationBoardResult);
        }
        catch(ClientTransportException clientTransportException) {
            logger.error("Unable to fetch live rail data", clientTransportException);
            return Optional.empty();
        }

    }
}
