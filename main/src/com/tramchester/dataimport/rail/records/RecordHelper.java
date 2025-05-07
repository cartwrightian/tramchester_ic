package com.tramchester.dataimport.rail.records;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class RecordHelper {
    private static final Logger logger = LoggerFactory.getLogger(RecordHelper.class);

    /***
     *
     * @param text string to extract record from
     * @param begin counting from 1, as per docs
     * @param end counting from 1, as per docs
     * @return the extracted record
     */
    public static String extract(final String text, final int begin, final int end) {
        final int length = text.length();
        final int realBegin = begin - 1;
        if (realBegin > length) {
            logger.warn(format("Record length too short (begin) was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return "";
        }
        final int realEnd = end - 1;
        if (realEnd > length) {
            logger.warn(format("Record length too short (end) was %s but looking for substring(%s,%s) in '%s'",
                    length, begin, end, text));
            return text.substring(realBegin, length-1).trim();
        }

        return text.substring(realBegin, realEnd).trim();
    }

    public static TramDate extractTramDate(final String text, final int begin, final int century) {
        return TramDate.parseSimple(text, century, begin);
    }

    /***
     * Parse time in format HHMM embedded within larger string
     * @param text the text to extract time from
     * @param begin begin index of time
     * @return TramTime or TramTime.Invalid
     */
    public static TramTime extractTime(final String text, final int begin) {
        if (text.isBlank()) {
            return TramTime.invalid();
        }
        return TramTime.parseBasicFormat(text, begin);

    }
}
