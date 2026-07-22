package com.tramchester.dataimport.rail.records;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.TramTime;
import jakarta.inject.Inject;

import javax.annotation.PreDestroy;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;

@LazySingleton
public class RecordHelper {

    private final TimeCache timeCache;
    private final LocationActivityCode.Parser locationActivityCodeParser;

    @Inject
    public RecordHelper() {
        timeCache = new TimeCache();
        locationActivityCodeParser = new LocationActivityCode.Parser();
        populateTimeCache();
    }

    private void populateTimeCache() {

    }

    @PreDestroy
    void stop() {
//        timeCache.clear();
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
        final byte[] bytes = line.subArray(begin, 4);
        return timeCache.find(bytes);
    }

    public ImmutableEnumSet<LocationActivityCode> parseLocationActivityCode(final Line text, final int begin, final int end) {
        final String substring = extractToString(text, begin, end);
        return locationActivityCodeParser.parse(substring);
    }

    // Rail spec's index from one
    public String extractToString(final Line line, final int begin, final int end) {
        return line.extractToString(begin-1, end-1);
    }

    private static class TimeCache {
        private final TramTime[] times;

        public TimeCache() {
            final int size = 24 * 60;
            times = new TramTime[size];
            for (int hours = 0; hours < 24; hours++) {
                for (int mins = 0; mins < 60; mins++) {
                    final int index = (hours * 60) + mins;
                    // make sure conversion to index from bytes works as expected
                    final byte[] bytes = format("%02d%02d", hours, mins).getBytes(US_ASCII);
                    final int checkKeyIsValid = indexFor(bytes);
                    if (checkKeyIsValid!=index) {
                        throw new RuntimeException(format("SanityCheck failed Mismatch on key %s and index %s", checkKeyIsValid, index));
                    }
                    times[index] = TramTime.of(hours, mins);
                }
            }
        }

        private int indexFor(final byte[] bytes) {
            final int hours = (10 * asNumber(bytes[0])) + asNumber(bytes[1]);
            final int mins =  (10 * asNumber(bytes[2])) + asNumber(bytes[3]);
            return (hours*60) + mins;
        }

        private byte asNumber(final byte b) {
            return (byte) (b & (byte)0x0F);
        }

        public TramTime find(final byte[] bytes) {
            if (isBlank(bytes)) {
                return TramTime.invalid();
            }

            final int key = indexFor(bytes);
            return times[key];
        }

        private boolean isBlank(byte[] a) {
            return a[0]==32 && a[1]==32 && a[2]==32 && a[3]==32;
        }

    }
}
