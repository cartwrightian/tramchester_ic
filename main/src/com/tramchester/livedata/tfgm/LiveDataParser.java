package com.tramchester.livedata.tfgm;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
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
import com.tramchester.livedata.repository.StationByName;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.*;
import java.util.*;

import static com.tramchester.livedata.domain.liveUpdates.LineDirection.Both;
import static com.tramchester.livedata.domain.liveUpdates.LineDirection.Unknown;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;



// TODO Split parse and create concerns here

@LazySingleton
public class LiveDataParser {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataParser.class);

    private static final String DIRECTION_BOTH = "Incoming/Outgoing";
    private static final String TERMINATES_HERE = "Terminates Here";
    private static final String NOT_IN_SERVICE = "Not in Service";
    private static final String SEE_TRAM_FRONT = "See Tram Front";
    private static final List<String> NotADestination = Arrays.asList(SEE_TRAM_FRONT, NOT_IN_SERVICE);

    private final TimeZone timeZone = TimeZone.getTimeZone(TramchesterConfig.TimeZoneId);

    private final StationByName stationByName;
    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final AgencyRepository agencyRepository;
    private final Map<String, String> destinationNameMappings;

    // live data api has limit in number of results
    private static final int MAX_DUE_TRAMS = 4;
    private Agency agency;

    public enum LiveDataNamesMapping {
        Firswood("Firswood", "Firswood Station"),
        Ashton("Ashton","Ashton-Under-Lyne"),
        DeansgateAliasA("Deansgate - Castlefield","Deansgate-Castlefield"),
        DeansgateAliasB("Deansgate Castlefield","Deansgate-Castlefield"),
        BessesOThBarns("Besses O’ Th’ Barn","Besses o'th'barn"),
        NewtonHeathAndMoston("Newton Heath and Moston","Newton Heath & Moston"),
        StWerburgsRoad("St Werburgh’s Road","St Werburgh's Road"),
        Rochdale("Rochdale Stn", "Rochdale Railway Station"),
        TraffordCentre("Trafford Centre", "The Trafford Centre"),
        RochdaleCentre("Rochdale Ctr", "Rochdale Town Centre");

        private final String from;
        private final String too;

        LiveDataNamesMapping(String from, String too) {
            this.from = from;
            this.too = too;
        }

        public String getToo() {
            return too;
        }
    }

    @Inject
    public LiveDataParser(StationByName stationByName, StationRepository stationRepository,
                          PlatformRepository platformRepository, AgencyRepository agencyRepository) {
        this.stationByName = stationByName;
        this.platformRepository = platformRepository;
        this.stationRepository = stationRepository;
        this.agencyRepository = agencyRepository;

        destinationNameMappings = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        List<LiveDataNamesMapping> referenceData = Arrays.asList(LiveDataNamesMapping.values());
        referenceData.forEach(item -> destinationNameMappings.put(item.from, item.too));
        agency = agencyRepository.get(MutableAgency.METL);
        logger.info("started");
    }

    public List<TramStationDepartureInfo> parse(String rawJson) {
        return parse(rawJson, new MonitorParsingWithLogging());
    }

    public List<TramStationDepartureInfo> parse(String rawJson, MonitorParsing monitorParsing) {
        final List<TramStationDepartureInfo> result = new LinkedList<>();

        final JsonObject parsed = Jsoner.deserialize(rawJson, new JsonObject());
        if (parsed.containsKey("value")) {
            JsonArray infoList = (JsonArray ) parsed.get("value");

            if (infoList!=null) {
                for (Object anInfoList : infoList) {
                    Optional<TramStationDepartureInfo> item = parseItem((JsonObject) anInfoList, monitorParsing);
                    item.ifPresent(result::add);
                }
            }
        } else {
            logger.error("Unable to deserialize received json: "+rawJson);
        }

        return result;
    }

    private Optional<TramStationDepartureInfo> parseItem(final JsonObject jsonObject, final MonitorParsing monitorParsing) {
        logger.debug(format("Parsing JSON '%s'", jsonObject));
        monitorParsing.currentJson(jsonObject);

        final BigDecimal displayId = (BigDecimal) jsonObject.get("Id");
        monitorParsing.currentDisplayId(displayId);
        
        final String rawLine = (String) jsonObject.get("Line");
        final String atcoCode = (String) jsonObject.get("AtcoCode");
        final String message = (String) jsonObject.get("MessageBoard");
        final String dateString = (String) jsonObject.get("LastUpdated");
        final String rawDirection = (String)jsonObject.get("Direction");
        final LocalDateTime updateTime = getStationUpdateTime(dateString);

        final LineDirection direction = getDirection(rawDirection);
        if (direction == Unknown) {
            monitorParsing.unknownDirection(rawDirection);
        }

        final Lines line = getLine(rawLine);
        if (line == Lines.UnknownLine) {
            monitorParsing.unknownLine(rawLine);
        }

        final Optional<Station> maybeStation = getStationByAtcoCode(atcoCode);
        if (maybeStation.isEmpty()) {
            monitorParsing.unknownStation(atcoCode);
            return Optional.empty();
        }
        final Station station = maybeStation.get();

        final TramStationDepartureInfo departureInfo = new TramStationDepartureInfo(displayId.toString(), line, direction,
                station, message, updateTime);

        final IdFor<Platform> platformId = getPlatformIdFor(station, atcoCode);
        if (platformRepository.hasPlatformId(platformId)) {
            final Platform platform = platformRepository.getPlatformById(platformId);
            departureInfo.setStationPlatform(platform);
            if (!station.hasPlatform(platformId)) {
                // NOTE: some single platform stations (i.e. navigation road) appear to have
                // two platforms in the live data feed...but not in the station reference data
                monitorParsing.missingPlatform(station, atcoCode);
            }
        } else {
            monitorParsing.missingPlatform(station, atcoCode);
        }

        parseDueTrams(jsonObject, departureInfo, monitorParsing);

        logger.debug("Parsed live data to " + departureInfo);
        return Optional.of(departureInfo);
    }

    private static @NotNull IdFor<Platform> getPlatformIdFor(final Station station, String atcoCode) {
        if ("9400ZZMATRC1".equals(atcoCode)) {
            // trafford park platform workaround
            atcoCode = "9400ZZMATRC2";
        }
        return TransportEntityFactoryForTFGM.createPlatformId(station.getId(), atcoCode);
    }

    private Lines getLine(final String text) {
        final Lines[] valid = Lines.values();
        for (Lines line : valid) {
            if (line.getName().equals(text)) {
                return line;
            }
        }
        return Lines.UnknownLine;
    }

    private LineDirection getDirection(final String text) {
        if (DIRECTION_BOTH.equals(text)) {
            return Both;
        }
        try {
            return LineDirection.valueOf(text);
        }
        catch (IllegalArgumentException unexpectedValueInTheApi) {
            logger.warn("Unable to parse direction " + text);
        }
        return Unknown;
    }

    private LocalDateTime getStationUpdateTime(final String dateString) {
        final Instant instanceOfUpdate = Instant.from(ISO_INSTANT.parse(dateString));
        final ZonedDateTime zonedDateTime = instanceOfUpdate.atZone(TramchesterConfig.TimeZoneId);

        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

        // WORKAROUND - feed always contains 'Z' at end of date/time even though feed actually switches to BST
        final boolean dst = timeZone.inDaylightTime(Date.from(instanceOfUpdate));
        if (dst) {
            int seconds_offset = timeZone.getDSTSavings() / 1000;
            localDateTime = localDateTime.minusSeconds(seconds_offset);
        }

        return localDateTime;
    }

    private void parseDueTrams(final JsonObject jsonObject, final TramStationDepartureInfo departureInfo, MonitorParsing monitorParsing) {
        for (int i = 0; i < MAX_DUE_TRAMS; i++) {
            final int index = i;
            final String destinationName = getNumberedField(jsonObject, "Dest", index);
            if (destinationName.isEmpty()) {
                // likely not present in json
                logger.debug("Skipping destination '" + destinationName + "' for " + jsonObject + " and index " + i);
            } else if (NotADestination.contains(destinationName)) {
                monitorParsing.tramNotInService(destinationName, index);
            } else {
                final Optional<Station> maybeDestStation;
                if (TERMINATES_HERE.equals(destinationName)) {
                    // replace "terminates here" with the station where this message is displayed
                    maybeDestStation = Optional.of(departureInfo.getStation());
                } else {
                    // try to look up destination station based on the destination text....
                    maybeDestStation = getTramDestination(destinationName);
                }

                maybeDestStation.ifPresentOrElse(station -> {
                            final String status = getNumberedField(jsonObject, "Status", index);
                            final String waitString = getNumberedField(jsonObject, "Wait", index);
                            int waitInMinutes = Integer.parseInt(waitString);
                            final String carriages = getNumberedField(jsonObject, "Carriages", index);
                            final LocalTime lastUpdate = departureInfo.getLastUpdate().toLocalTime();
                            final LocalDate date = departureInfo.getLastUpdate().toLocalDate();
                            final Station displayLocation = departureInfo.getStation();

                            final TramTime when = TramTime.ofHourMins(lastUpdate.plusMinutes(waitInMinutes));

                            final UpcomingDeparture dueTram = new UpcomingDeparture(date, displayLocation, station, status,
                                    when, carriages, agency, TransportMode.Tram);
                            if (departureInfo.hasStationPlatform()) {
                                dueTram.setPlatform(departureInfo.getStationPlatform());
                            }
                            departureInfo.addDueTram(dueTram);

                        },

                        () -> monitorParsing.missingDestinationStation(destinationName, index));
            }
        }
    }

    private Optional<Station> getStationByAtcoCode(String atcoCode) {
        IdFor<Station> stationId = TransportEntityFactoryForTFGM.getStationIdFor(atcoCode);
        if (stationRepository.hasStationId(stationId)) {
            return Optional.of(stationRepository.getStationById(stationId));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Station> getTramDestination(String name) {
        if (name.isEmpty())
        {
            logger.warn("Got empty name");
            return Optional.empty();
        }
        if (NotADestination.contains(name)) {
            logger.info(format("Not a destination: '%s'", name));
            return Optional.empty();
        }

        String destinationName = mapLiveAPIToTimetableDataNames(name);
        return stationByName.getTramStationByName(destinationName);
    }

    private String mapLiveAPIToTimetableDataNames(String destinationName) {
        destinationName = destinationName.replace("Via", "via");

        // assume station name is valid.....
        int viaIndex = destinationName.indexOf(" via");
        if (viaIndex > 0) {
            destinationName = destinationName.substring(0, viaIndex);
        }

        if (destinationNameMappings.containsKey(destinationName)) {
            return destinationNameMappings.get(destinationName);
        }

        return destinationName;
    }

    private String getNumberedField(JsonObject jsonObject, String name, final int i) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }

    public interface MonitorParsing {
        void currentJson(JsonObject jsonObject);
        void currentDisplayId(BigDecimal displayId);
        void unknownDirection(String rawDirection);
        void unknownLine(String rawLine);
        void unknownStation(String atcoCode);
        void missingPlatform(Station station, String atcoCode);
        void missingDestinationStation(String destinationName, int index);
        void tramNotInService(String destinationName, int index);
    }

    private static class MonitorParsingWithLogging implements MonitorParsing {

        private JsonObject jsonObject;
        private BigDecimal displayId;
        
        @Override
        public void currentJson(JsonObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        @Override
        public void currentDisplayId(BigDecimal displayId) {
            this.displayId = displayId;
        }
        
        @Override
        public void unknownDirection(String rawDirection) {
            String text = "Unable to map direction code name'" + rawDirection + "'";
            logWarning(text);
        }

        @Override
        public void unknownLine(String rawLine) {
            logWarning("Unable to map line name '" + rawLine + "'");
        }

        @Override
        public void unknownStation(String atcoCode) {
            logWarning("Unable to map atco code to station '" + atcoCode + "'");
        }

        @Override
        public void missingPlatform(Station station, String atcoCode) {
            logWarning(format("Platform '%s' not in timetable data for station %s", atcoCode, station.getId()));
        }

        @Override
        public void missingDestinationStation(String destinationName, int index) {
            logWarning("Unable to match due tram destination '" + destinationName + "' index: " + index);
        }

        @Override
        public void tramNotInService(String destinationName, int index) {
            logger.debug(prefix() + " Skipping destination '" + destinationName + " for index: " + index + postfix());

        }

        private void logWarning(String text) {
            logger.warn(prefix() + text + postfix());
        }

        private @NotNull String prefix() {
            return "Display '" + displayId + "' ";
        }

        private String postfix() {
            return " for JSON '" + jsonObject + "'";
        }
    }

}
