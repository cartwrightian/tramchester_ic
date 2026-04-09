package com.tramchester.dataimport.rail.records;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import jakarta.inject.Inject;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@LazySingleton
public class RecordHelper {

    private final ConcurrentMap<Line, TramTime> timeCache;
    private final LocationActivityCode.Parser locationActivityCodeParser;

    @Inject
    public RecordHelper() {
        timeCache = new ConcurrentHashMap<>();
        locationActivityCodeParser = new LocationActivityCode.Parser();
    }

    @PreDestroy
    void stop() {
        timeCache.clear();
    }

    public TramDate extractTramDate(final Line text, final int begin, final int century) {
        return TramDate.parseSimple(text, century, begin);
    }

    /***
     * Parse time in format HHMM embedded within larger string
     * @param line the text to extract time from
     * @param begin begin index of time
     * @return TramTime or TramTime.Invalid
     */
    public TramTime extractTime(final Line line, final int begin) {
        char[] timeText = line.subArray(begin, 4);
        //return TramTime.parseBasicFormat(timeText);
        return timeCache.computeIfAbsent(Line.of(timeText), key -> TramTime.parseBasicFormat(timeText));
    }

    public ImmutableEnumSet<LocationActivityCode> parseLocationActivityCode(final Line text, final int begin, final int end) {
        final String substring = extractToString(text, begin, end);
        return locationActivityCodeParser.parse(substring);
    }

    // Rail spec's index from one
    public String extractToString(final Line line, final int begin, final int end) {
        return line.extractToString(begin-1, end-1);
    }
}
