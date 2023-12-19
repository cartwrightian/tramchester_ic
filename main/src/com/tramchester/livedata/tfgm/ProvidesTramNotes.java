package com.tramchester.livedata.tfgm;

import com.google.common.collect.Sets;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.repository.ProvidesNotes;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.presentation.Note.NoteType.*;

@LazySingleton
public class ProvidesTramNotes implements ProvidesNotes {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesTramNotes.class);

    private static final String EMPTY = "<no message>";
    public static final String website = "Please check <a href=\"https://tfgm.com/travel-updates/travel-alerts#tram\">TFGM</a> for details.";
    public static final String weekend = "At the weekend your journey may be affected by improvement works." + website;
    public static final String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;
    public static final String christmas2023 = "There are changes to services between 24th Dec and 1st January. " +
            "Please check <a = href=\"https://tfgm.com/winter-travel/christmas-operating-times\">TFGM</a> for details.";

    private static final int MESSAGE_LIFETIME = 5;

    private final PlatformMessageSource platformMessageSource;
    private final DTOFactory stationDTOFactory;

    @Inject
    public ProvidesTramNotes(PlatformMessageSource platformMessageSource, DTOFactory stationDTOFactory) {
        this.platformMessageSource = platformMessageSource;
        this.stationDTOFactory = stationDTOFactory;
    }

    @PostConstruct
    void start() {
        logger.info("starting");
        if (!platformMessageSource.isEnabled()) {
            logger.warn("Disabled for live data since PlatformMessageSource is disabled");
        }
        logger.info("started");
    }

    /***
     * From JourneyDTO prep
     */
    @Override
    public List<Note> createNotesForJourney(Journey journey, TramDate queryDate) {
        if (!journey.getTransportModes().contains(TransportMode.Tram)) {
            logger.info("Not a tram journey, providing no notes");
            return Collections.emptyList();
        }

        List<Note> notes = new ArrayList<>(getNotesForADate(queryDate));

        if (platformMessageSource.isEnabled()) {
            List<StationNote> platformNotesFor = getPlatformNotesFor(journey, queryDate);
            notes.addAll(platformNotesFor);
        }

        return notes;
    }

    @Override
    public List<Note> createNotesForStations(List<Station> stations, TramDate queryDate, TramTime time) {
        if (!platformMessageSource.isEnabled()) {
            logger.error("Attempted to get notes for departures when live data disabled");
            return Collections.emptyList();
        }

        List<Note> notes = new ArrayList<>();
        notes.addAll(getNotesForADate(queryDate));
        notes.addAll(createLiveNotesForStations(stations, queryDate, time));
        return notes;
    }

    private List<StationNote> getPlatformNotesFor(Journey journey, TramDate queryDate) {

        Set<PlatformMessage> allMessages = journey.getCallingPlatformIds().stream().
                map(platformId -> getMessageFor(platformId, queryDate, journey.getQueryTime())).
                filter(Optional::isPresent).
                map(Optional::get).
                collect(Collectors.toSet());

        return createUniqueNotesFor(allMessages);

    }

    private List<StationNote> createLiveNotesForStations(List<Station> stations, TramDate date, TramTime time) {

        Set<PlatformMessage> allMessages = stations.stream().
                flatMap(station -> platformMessageSource.messagesFor(station, date, time).stream()).
                filter(this::notEmpty).collect(Collectors.toSet());

        return createUniqueNotesFor(allMessages);
    }

    @NotNull
    private List<StationNote> createUniqueNotesFor(Set<PlatformMessage> allMessages) {
        Map<String, Set<Station>> uniqueText = allMessages.stream().
                collect(Collectors.toMap(PlatformMessage::getMessage,
                        platformMessage -> Collections.singleton(platformMessage.getStation()),
                        Sets::union));

        return uniqueText.entrySet().stream().map(entry -> stationDTOFactory.createStationNote(Live, entry.getKey(), entry.getValue())).toList();
    }

    private List<Note> getNotesForADate(TramDate queryDate) {
        ArrayList<Note> notes = new ArrayList<>();
        if (queryDate.isWeekend()) {
            notes.add(new Note(weekend, Weekend));
        }
        if (queryDate.isChristmasPeriod()) {
            int year = queryDate.getYear();
            if (year==2023 || year==2024) {
                notes.add(new Note(christmas2023, Christmas));
            } else {
                notes.add(new Note(christmas, Christmas));
            }
        }
        return notes;
    }

    private Optional<PlatformMessage> getMessageFor(IdFor<Platform> platformId, TramDate queryDate, TramTime queryTime) {
        Optional<PlatformMessage> maybe = platformMessageSource.messagesFor(platformId, queryDate, queryTime);
        if (maybe.isEmpty()) {
            logger.warn("No messages found for " + platformId + " at " + queryDate +  " " + queryTime);
            return maybe;
        }

        PlatformMessage platformMessage = maybe.get();
        LocalDateTime lastUpdate = platformMessage.getLastUpdate();
        TramDate lastUpdateDate = TramDate.from(lastUpdate);
        if (!lastUpdateDate.isEqual(queryDate)) {
            // message is not for journey time, perhaps journey is a future date or live data is stale
            logger.info("No messages available for " + queryDate + " last up date was " + lastUpdate);
            return Optional.empty();
        }

        TramTime updateTime = TramTime.ofHourMins(lastUpdate.toLocalTime());
        // 1 minutes here as time sync on live api has been out by 1 min
        TimeRange range = TimeRange.of(updateTime, Duration.ofMinutes(1), Duration.ofMinutes(MESSAGE_LIFETIME));
        if (!range.contains(queryTime)) {
            logger.info("No data available for " + queryTime + " as not within " + range);
            return Optional.empty();
        }

        return maybe;
    }

    private boolean notEmpty(PlatformMessage msg) {
        String text = msg.getMessage();
        return ! (text.isEmpty() || EMPTY.equals(text));
    }
}
