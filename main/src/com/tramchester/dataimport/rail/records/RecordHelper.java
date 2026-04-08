package com.tramchester.dataimport.rail.records;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

@LazySingleton
public class RecordHelper {
    private static final Logger logger = LoggerFactory.getLogger(RecordHelper.class);

    private final ConcurrentMap<String, TramTime> timeCache;
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

    /***
     *
     * @param text string to extract record from
     * @param begin counting from 1, as per docs
     * @param end counting from 1, as per docs
     * @return the extracted record
     */
    public String extract(final String text, final int begin, final int end) {
        final int totalLength = text.length();

        // change to zero-indexed, rail standards are index'ed from 1
        final int realBegin = begin - 1;
        final int realEnd = end - 1;

        if (realBegin > totalLength) {
//            throw new RuntimeException("Index out of range " + realBegin + "'" + text + "'");
            logger.warn(format("Record length too short (begin) was %s but looking for substring(%s,%s) in '%s'",
                    totalLength, begin, end, text));
            return "";
        }
        if (realEnd > totalLength) {
//            throw new RuntimeException("Index out of range " + realEnd + "'" + text + "'");
            logger.warn(format("Record length too short (end) was %s but looking for substring(%s,%s) in '%s'",
                    totalLength, begin, end, text));
            // trim to length
            return trimmedSubstring(text, realBegin, totalLength-1);
        }

        return trimmedSubstring(text, realBegin, realEnd);
    }

    public String trimmedSubstring(final String source, final int begin, final int substringEnd) {
        int length = substringEnd - begin;
        final int offset = begin - 1;
        do {
            if (length<0) {
                return "";
            }
        } while (Character.isWhitespace(source.charAt((offset +(length--)))));

        length++;
        // substring end is spec'ed as required char index + 1
        return new String(source.getBytes(), begin, length);
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
        return timeCache.computeIfAbsent(new String(timeText), key -> TramTime.parseBasicFormat(timeText));
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
