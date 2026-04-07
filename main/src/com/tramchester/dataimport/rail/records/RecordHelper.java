package com.tramchester.dataimport.rail.records;

import com.netflix.governator.guice.lazy.LazySingleton;
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

    @Inject
    public RecordHelper() {
        timeCache = new ConcurrentHashMap<>();
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
        final int length = text.length();

        // change to zero-indexed, rail standards are index'ed from 1
        final int realBegin = begin - 1;
        final int realEnd = end - 1;

        if (realBegin > length) {
//            throw new RuntimeException("Index out of range " + realBegin + "'" + text + "'");
            logger.warn(format("Record length too short (begin) was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return "";
        }
        if (realEnd > length) {
//            throw new RuntimeException("Index out of range " + realEnd + "'" + text + "'");
            logger.warn(format("Record length too short (end) was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return trimmedSubstring(text, realBegin, length-1);
        }

        return trimmedSubstring(text, realBegin, realEnd);
    }

    public String trimmedSubstring(final String source, final int begin, final int substringEnd) {
        int actualEnd = substringEnd; // String substring end is defined as index+1, hence -1 below
        do {
            if (actualEnd<begin) {
                return "";
            }
        } while (Character.isWhitespace(source.charAt((actualEnd--)-1)));

        actualEnd++;
        // substring end is spec'ed as required char index + 1
        return source.substring(begin, actualEnd);

    }

    public TramDate extractTramDate(final String text, final int begin, final int century) {
        return TramDate.parseSimple(text, century, begin);
    }

    /***
     * Parse time in format HHMM embedded within larger string
     * @param text the text to extract time from
     * @param begin begin index of time
     * @return TramTime or TramTime.Invalid
     */
    public TramTime extractTime(final String text, final int begin) {
        if (text.isBlank()) {
            return TramTime.invalid();
        }
        final String timeText = text.substring(begin, begin+4);
        return timeCache.computeIfAbsent(timeText, x -> TramTime.parseBasicFormat(text, begin));
        //return TramTime.parseBasicFormat(text, begin);

    }
}
