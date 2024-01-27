package com.tramchester.livedata.tfgm;

import com.google.common.collect.Sets;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.StationNote;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.repository.PlatformMessageSource;
import com.tramchester.livedata.repository.ProvidesNotes;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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

//    private static final int MESSAGE_LIFETIME = 5;

    private final PlatformMessageSource platformMessageSource;
    private final DTOFactory stationDTOFactory;

    @Inject
    public ProvidesTramNotes(PlatformMessageSource platformMessageSource, DTOFactory stationDTOFactory) {
        this.platformMessageSource = platformMessageSource;
        this.stationDTOFactory = stationDTOFactory;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    void start() {
        logger.info("starting");
        if (!platformMessageSource.isEnabled()) {
            logger.warn("Disabled for live data since PlatformMessageSource is disabled");
        }
        logger.info("started");
    }

    @Override
    public List<Note> createNotesForStations(Set<Station> stations, TramDate queryDate, TramTime time) {
        EnumSet<TransportMode> modes = stations.stream().
                flatMap(station -> station.getTransportModes().stream()).
                collect(Collectors.toCollection(() -> EnumSet.noneOf(TransportMode.class)));

        List<Note> notes = new ArrayList<>(getNotesForADate(queryDate, modes));

        if (platformMessageSource.isEnabled()) {
            notes.addAll(createLiveNotesForStations(stations, queryDate, time));
        } else {
            logger.error("Attempted to get notes for departures when live data disabled");
        }

        return notes;
    }

    private List<StationNote> createLiveNotesForStations(final Set<Station> stations, TramDate date, TramTime time) {
        if (stations.isEmpty()) {
            logger.warn("No stations provided");
            return Collections.emptyList();
        }

        Set<PlatformMessage> allMessages = stations.stream().
                flatMap(station -> platformMessageSource.messagesFor(station, date, time).stream()).
                filter(this::notEmpty).collect(Collectors.toSet());

        return createUniqueNotesFor(allMessages);
    }

    @NotNull
    private List<StationNote> createUniqueNotesFor(final Set<PlatformMessage> allMessages) {
        final Map<String, Set<Station>> uniqueText = allMessages.stream().
                collect(Collectors.toMap(PlatformMessage::getMessage,
                        platformMessage -> Collections.singleton(platformMessage.getStation()),
                        Sets::union));

        return uniqueText.entrySet().stream().map(entry -> stationDTOFactory.createStationNote(Live, entry.getKey(), entry.getValue())).toList();
    }

    private List<Note> getNotesForADate(TramDate queryDate, EnumSet<TransportMode> modes) {
        if (!modes.contains(TransportMode.Tram)) {
            logger.info("Did not find tram in supplied modes, return no notes " + modes);
            return Collections.emptyList();
        }

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

    private boolean notEmpty(PlatformMessage msg) {
        String text = msg.getMessage();
        return ! (text.isEmpty() || EMPTY.equals(text));
    }
}
