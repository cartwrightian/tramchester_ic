package com.tramchester.repository;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.DateRangeAndVersion;
import com.tramchester.domain.dates.ServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static java.time.ZoneOffset.UTC;

public class DataSourceInfoRepository {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceInfoRepository.class);

    private final Map<DataSourceID, DataSourceInfo> theMap;
    private final ProvidesNow providesNow;

    private DataSourceInfoRepository(Map<DataSourceID, DataSourceInfo> map, ProvidesNow providesNow) {
        this.theMap = map;
        this.providesNow = providesNow;
    }

    public DataSourceInfoRepository(DataSourceInfoRepository original) {
        this(original.theMap, original.providesNow);
    }

    public DataSourceInfoRepository(ProvidesNow providesNow) {
        this(new HashMap<>(), providesNow);
    }

    /***
     * @param mode transport mode
     * @return Time in UTC zone
     */
    public ZonedDateTime getNewestModTimeFor(final TransportMode mode) {
        Optional<ZonedDateTime> result = theMap.values().stream().
                filter(info -> info.getModes().contains(mode)).
                map(DataSourceInfo::getLastModTime).
                max(Comparator.naturalOrder());
        if (result.isEmpty()) {
            logger.error("Cannot find latest mod time for transport mode " + mode);
            return ZonedDateTime.of(providesNow.getDateTime(), UTC);
        } else {
            final ZonedDateTime localDateTime = result.get();
            logger.info("Newest mode time for " + mode.name() + " is " + localDateTime);
            return localDateTime;
        }
    }

    public void add(DataSourceInfo dataSourceInfo) {
        DataSourceID id = dataSourceInfo.getID();
        if (theMap.containsKey(id)) {
            throw new RuntimeException("Cannot add multiple instances of dataSourceId, already had " + id);
        }
        theMap.put(id, dataSourceInfo);
    }

    public boolean has(DataSourceID dataSourceID) {
        return theMap.containsKey(dataSourceID);
    }

    public DateRangeAndVersion getDateRangeAndVersionFor(DataSourceID dataSourceID, Set<ServiceCalendar> serviceCalendars) {
        DataSourceInfo dataSourceInfo = theMap.get(dataSourceID);
        TramDate expiryDate = findLastExpiryDate(dataSourceID, serviceCalendars);
        return new RangeAndVersion(dataSourceInfo.getVersion(), dataSourceInfo.getLastModTime().toLocalDate(), expiryDate.toLocalDate());
    }

    private TramDate findLastExpiryDate(final DataSourceID dataSourceId, final Set<ServiceCalendar> serviceCalendars) {

        if (serviceCalendars.isEmpty()) {
            logger.info("Found no services for " + dataSourceId);
        }

        final Optional<TramDate> last = serviceCalendars.stream().map(serviceCalendar -> serviceCalendar.getDateRange().getEndDate()).
                max(TramDate::compareTo);
        if (last.isEmpty()) {
            throw new RuntimeException("Cannot compute expiry date for " + dataSourceId + " with calendaers " + serviceCalendars);
        }
        return last.get();

    }

    public Set<DataSourceInfo> getAll() {
        return new HashSet<>(theMap.values());
    }

    public boolean isEmpty() {
        return theMap.isEmpty();
    }

    public DataSourceInfo get(DataSourceID dataSourceID) {
        return theMap.get(dataSourceID);
    }

    @Override
    public String toString() {
        return "DataSourceInfoRepository{" +
                "theMap=" + theMap +
                ", providesNow=" + providesNow +
                '}';
    }

    private record RangeAndVersion(String version, LocalDate validFrom,
                                   LocalDate validUntil) implements DateRangeAndVersion {

    }
}
