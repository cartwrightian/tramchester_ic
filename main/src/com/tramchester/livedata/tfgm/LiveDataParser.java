package com.tramchester.livedata.tfgm;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.LineDirection;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.repository.StationByName;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
    private final Map<String, String> destinationNameMappings;

    private final TramDepartureFactory departureFactory;

    private final Set<String> mappingsUsed;
    private final EnumSet<OverheadDisplayLines> linesSeen;

    // live data api has limit in number of results
    private static final int MAX_DUE_TRAMS = 4;

    public enum LiveDataNamesMapping {
        DeansgateAliasB("Deansgate Castlefield","Deansgate-Castlefield"),
        Firswood("Firswood", "Firswood Station");

// No longer in use?
//        Firswood("Firswood", "Firswood Station"),
//        Ashton("Ashton","Ashton-Under-Lyne"),
//        DeansgateAliasA("Deansgate - Castlefield","Deansgate-Castlefield"),
//        BessesOThBarns("Besses O’ Th’ Barn","Besses o'th'barn"),
//        NewtonHeathAndMoston("Newton Heath and Moston","Newton Heath & Moston"),
//        StWerburgsRoad("St Werburgh’s Road","St Werburgh's Road"),
//        Rochdale("Rochdale Stn", "Rochdale Railway Station"),
//        TraffordCentre("Trafford Centre", "The Trafford Centre"),
//        RochdaleCentre("Rochdale Ctr", "Rochdale Town Centre");

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
    public LiveDataParser(StationByName stationByName, TramDepartureFactory departureFactory) {
        this.stationByName = stationByName;
        this.departureFactory = departureFactory;

        destinationNameMappings = new HashMap<>();

        mappingsUsed = new HashSet<>();
        linesSeen = EnumSet.noneOf(OverheadDisplayLines.class);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        for(LiveDataNamesMapping item : LiveDataNamesMapping.values()) {
            destinationNameMappings.put(item.from, item.too);
        }

        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        final Set<String> unusedMappings = new HashSet<>(destinationNameMappings.keySet());
        unusedMappings.removeAll(mappingsUsed);
        if (!unusedMappings.isEmpty()) {
            logger.warn("The following mappings were not used " + unusedMappings);
        }

        final EnumSet<OverheadDisplayLines> unusedLines = EnumSet.allOf(OverheadDisplayLines.class);
        unusedLines.remove(OverheadDisplayLines.UnknownLine);
        unusedLines.removeAll(linesSeen);
        if (!unusedMappings.isEmpty()) {
            logger.warn("The following lines were not seen " + unusedLines);
        }

        logger.info("stopped");
    }

    public List<TramStationDepartureInfo> parse(final String rawJson) {
        return parse(rawJson, new MonitorParsingWithLogging());
    }

    public List<TramStationDepartureInfo> parse(final String rawJson, final MonitorParsing monitorParsing) {
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

        final OverheadDisplayLines line = getLine(rawLine);
        linesSeen.add(line);
        if (line == OverheadDisplayLines.UnknownLine) {
            monitorParsing.unknownLine(rawLine);
        }

        final TramStationDepartureInfo departureInfo = departureFactory.createStationDeparture(displayId, line, direction,
                atcoCode, message, updateTime);
        if (departureInfo==null) {
            monitorParsing.unknownStation(atcoCode);
            return Optional.empty();
        }

        if (!departureInfo.hasStationPlatform()) {
            monitorParsing.missingPlatform(departureInfo.getStation(), atcoCode);
        } else {
            Station station = departureInfo.getStation();
            if (!station.hasPlatform(departureInfo.getStationPlatform().getId())) {
                monitorParsing.missingPlatform(station, atcoCode);
            }
        }

        parseDueTrams(jsonObject, departureInfo, monitorParsing);

        logger.debug("Parsed live data to " + departureInfo);
        return Optional.of(departureInfo);
    }

    private OverheadDisplayLines getLine(final String text) {
        final OverheadDisplayLines[] valid = OverheadDisplayLines.values();
        for (OverheadDisplayLines line : valid) {
            if (line.getName().equals(text)) {
                return line;
            }
        }
        return OverheadDisplayLines.UnknownLine;
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
                            final int waitInMinutes = Integer.parseInt(waitString);
                            final String carriages = getNumberedField(jsonObject, "Carriages", index);

                            UpcomingDeparture dueTram = departureFactory.createDueTram(departureInfo, status, station, waitInMinutes, carriages);

                            departureInfo.addDueTram(dueTram);

                        },

                        () -> monitorParsing.missingDestinationStation(destinationName, index));
            }
        }
    }

    private Optional<Station> getTramDestination(final String name) {
        if (name.isEmpty())
        {
            logger.warn("Got empty name");
            return Optional.empty();
        }
        if (NotADestination.contains(name)) {
            logger.info(format("Not a destination: '%s'", name));
            return Optional.empty();
        }

        final String destinationName = mapLiveAPIToTimetableDataNames(name);
        return stationByName.getTramStationByName(destinationName);
    }

    private String mapLiveAPIToTimetableDataNames(final String name) {
        String destinationName = name.replace("Via", "via");

        // 'X via Z' => 'X'
        final int viaIndex = destinationName.indexOf(" via");
        if (viaIndex > 0) {
            destinationName = destinationName.substring(0, viaIndex);
        }

        if (destinationNameMappings.containsKey(destinationName)) {
            mappingsUsed.add(destinationName);
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
            // a missing platform does not matter if we found the station ok
            logger.debug(format("Platform '%s' not in timetable data for station %s", atcoCode, station.getId()));
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
