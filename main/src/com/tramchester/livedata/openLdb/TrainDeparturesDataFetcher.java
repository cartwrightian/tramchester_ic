package com.tramchester.livedata.openLdb;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thalesgroup.rtti._2013_11_28.token.types.AccessToken;
import com.thalesgroup.rtti._2017_10_01.ldb.*;
import com.thalesgroup.rtti._2017_10_01.ldb.types.DeparturesBoardWithDetails;
import com.thalesgroup.rtti._2017_10_01.ldb.types.StationBoard;
import com.tramchester.config.OpenLdbConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.repository.CRSRepository;
import com.tramchester.domain.DestinationAndCallingPoints;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import jakarta.inject.Inject;
import jakarta.xml.ws.WebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.List;
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
            final OpenLdbConfig openLdbConfig = config.getOpenLdb();

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

        if (!guard(station)) {
            return Optional.empty();
        }

        final IdFor<Station> stationId = station.getId();
        final String crs = crsRepository.getCRSCodeFor(stationId);
        logger.info("Get train departures for " + stationId + " with CRS " + crs);

        final GetBoardRequestParams params = new GetBoardRequestParams();
        params.setCrs(crs);
        params.setTimeOffset(0);
        params.setTimeWindow(config.getMaxWait());

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

    public Optional<DeparturesBoardWithDetails> getFor(final Station station, final DestinationAndCallingPoints destinationAndCallingPoints) {
        if (!guard(station)) {
            return Optional.empty();
        }

        final IdFor<Station> stationId = station.getId();
        final String crs = crsRepository.getCRSCodeFor(stationId);
        logger.info("Get train departures for " + stationId + " with Station " + crs + " and " + destinationAndCallingPoints);

        GetDeparturesRequestParams.FilterList filterList = createFilterListFor(destinationAndCallingPoints);

        GetDeparturesRequestParams params = new GetDeparturesRequestParams();
        params.setCrs(crs);
        params.setTimeOffset(0);
        params.setTimeWindow(config.getMaxWait());

        if (!filterList.getCrs().isEmpty()) {
            logger.info("Filter list is CRS '" + filterList.getCrs() + "'");
            params.setFilterList(filterList);
        }

        try {
            DeparturesBoardWithDetailsResponseType departureBoard = soapService.getNextDeparturesWithDetails(params, accessToken);

            DeparturesBoardWithDetails stationBoardResult = departureBoard.getDeparturesBoard();
            logger.info(format("Got detailed departure board %s at %s for %s", stationBoardResult.getLocationName(),
                    stationBoardResult.getGeneratedAt(), crs));

            return Optional.of(stationBoardResult);
        }
        catch(ClientTransportException | ServerSOAPFaultException clientTransportException) {
            final String msg = format("Unable to fetch live rail data for %s and station CRs %s and filter list %s (for %s)",
                    station.getId(), crs, filterList.getCrs(), destinationAndCallingPoints);
            logger.error(msg, clientTransportException);
            return Optional.empty();
        }
    }

    private GetDeparturesRequestParams.FilterList createFilterListFor(final DestinationAndCallingPoints destinationAndCallingPoints) {
        final GetDeparturesRequestParams.FilterList filterList = new GetDeparturesRequestParams.FilterList();

        // docs say this is list to update
        final List<String> stations = filterList.getCrs();

        final IdFor<Station> destination = destinationAndCallingPoints.destination();
        if (crsRepository.hasStation(destination)) {
            final String destCRS = crsRepository.getCRSCodeFor(destination);
            stations.add(destCRS);
        } else {
            logger.info("Destination does not have a CRS " + destination);
        }

        ImmutableIdSet<Station> callingPoints = destinationAndCallingPoints.callingPoints();
        List<IdFor<Station>> hasCRS = callingPoints.stream().
                filter(crsRepository::hasStation).
                toList();

        if (hasCRS.size() != callingPoints.size()) {
            logger.info(format("Did not find calling points for all of %s, only available for %s", callingPoints, hasCRS));
        }

        List<String> callingCRS = hasCRS.stream().
                map(crsRepository::getCRSCodeFor).
                toList();
        stations.addAll(callingCRS);

        return filterList;
    }

    private boolean guard(Station station) {
        final IdFor<Station> stationId = station.getId();

        if (!enabled) {
            logger.error("Attempt to invoke, but not enabled, did start up fail? Station:" + stationId);
            return false;
        }
        if (!started) {
            // restart strategy needed?
            logger.warn("Not started, unable to fetch live data from " + stationId);
            return false;
        }

        if (!station.getTransportModes().contains(TransportMode.Train)) {
            logger.warn("Station is not a train station");
            return false;
        }
        if (!crsRepository.hasStation(stationId)) {
            logger.error("Not CRS Code found for " + stationId);
            return false;
        }
        return true;
    }


}
