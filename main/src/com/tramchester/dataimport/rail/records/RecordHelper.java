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

    public static String trimmedSubstring(final String source, final int begin, final int substringEnd) {
        int actualEnd = substringEnd; // String substring end is defined as index+1, hence -1 below
        do {
            if (actualEnd<begin) {
                return "";
            }
        } while (Character.isWhitespace(source.charAt((actualEnd--)-1)));

        actualEnd++;
        // substring end is spec'ed as required char index + 1
        return source.substring(begin, actualEnd);
//        final int subStringEnd = end +1;
//        if (!Character.isWhitespace(source.charAt(end))) {
//            return source.substring(begin, end);
//        }
//        int remove = end;
//        do {
//            remove--;
//            if (remove<begin) {
//                return "";
//            }
//        } while(Character.isWhitespace(source.charAt(remove)));
//        return source.substring(begin, remove+1);
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
