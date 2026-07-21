package com.tramchester.dataimport.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.MutableNormalServiceCalendar;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.repository.WriteableTransportData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RailServiceGroups {
    private static final Logger logger = LoggerFactory.getLogger(RailServiceGroups.class);

    private final WriteableTransportData container;

    private final ServiceGroups serviceGroups;
    private final Set<String> skippedSchedules; // services skipped due to train category etc.
    private final Set<String> unmatchedCancellations; // attempted to cancel but could not find
    private final Set<String> unmatchedOverlay; // overlay encountered, but no existing service

    public RailServiceGroups(WriteableTransportData container) {
        this.container = container;
        skippedSchedules = new HashSet<>();
        serviceGroups = new ServiceGroups();
        unmatchedCancellations = new HashSet<>();
        unmatchedOverlay = new HashSet<>();
    }

    public void applyCancellation(final BasicSchedule cancellationSchedule) {
        final String uniqueTrainId = cancellationSchedule.getUniqueTrainId();
        final List<MutableService> matchingServices = serviceGroups.servicesFor(uniqueTrainId);
        if (matchingServices.isEmpty()) {
            if (!skippedSchedules.contains(uniqueTrainId)) {
                unmatchedCancellations.add(uniqueTrainId);
            }
            return;
        }

        final Set<MutableService> cancellationApplies = filterByScheduleDates(cancellationSchedule, matchingServices);

        if (cancellationApplies.isEmpty()) {
            unmatchedCancellations.add(uniqueTrainId);
            return;
        }

        cancellationApplies.forEach(service -> {
                    MutableServiceCalendar calendar = service.getMutableCalendar();
                    recordOverlapAsSuperseding(calendar, cancellationSchedule.getDateRange(), cancellationSchedule.getDaysOfWeek());
                });

    }

    @NotNull
    private Set<MutableService> filterByScheduleDates(final BasicSchedule basicSchedule, final List<MutableService> existingServices) {

        final DateRange dateRange = basicSchedule.getDateRange();

        return existingServices.stream().
                filter(service -> dateRange.overlapsWith(service.getCalendar().getDateRange())).
                collect(Collectors.toSet());
    }

    MutableService getOrCreateService(final BasicSchedule schedule, final boolean isOverlay, final DataSourceID dataSourceId) {
        final String uniqueTrainId = schedule.getUniqueTrainId();

        final IdFor<Service> serviceId = getServiceIdFor(schedule, isOverlay);

        final MutableService service = new MutableService(serviceId, dataSourceId);
        final DateRange scheduleDateRange = schedule.getDateRange();
        final MutableNormalServiceCalendar calendar = new MutableNormalServiceCalendar(scheduleDateRange, schedule.getDaysOfWeek());
        service.setCalendar(calendar);

        final List<MutableService> existingServices = serviceGroups.servicesFor(uniqueTrainId);

        if (isOverlay) {
            final boolean cancelled = unmatchedCancellations.contains(uniqueTrainId);

            final Set<MutableService> matchedServices;
            if (existingServices.isEmpty()) {
                if (!cancelled) {
                    unmatchedOverlay.add(uniqueTrainId);
                }
                matchedServices = Collections.emptySet();
            } else {
                matchedServices = filterByScheduleDates(schedule, existingServices);
                if (matchedServices.isEmpty() && !cancelled) {
                    if (!unmatchedOverlay.contains(uniqueTrainId)) {
                        logger.warn(format("Overlap: No existing services overlapped on date range (%s) for %s ",
                                scheduleDateRange, uniqueTrainId));
                        unmatchedOverlay.add(uniqueTrainId);
                    }
                }
            }

            // the new overlay
            matchedServices.forEach(impactedService -> {
                final IdFor<Service> impactedServiceId = impactedService.getId();
                if (impactedServiceId.equals(serviceId)) {
                    // only happens if over dates exactly match existing service
                    logger.info(format("Overlap: Marking existing service %s as cancelled", impactedServiceId));
                    impactedService.markCancelled();
                } else {
                    // mark overlay dates as no longer applying
                    logger.debug(format("Overlap: Marking existing service %s as cancelled for %s %s",
                            impactedServiceId, scheduleDateRange, schedule.getDaysOfWeek()));
                    final MutableServiceCalendar impactedCalendar = impactedService.getMutableCalendar();
                    final DateRange impactedCalendarDateRange = impactedCalendar.getDateRange();

                    if (impactedCalendarDateRange.overlapsWith(scheduleDateRange)) {
                        recordOverlapAsSuperseding(impactedCalendar, scheduleDateRange, schedule.getDaysOfWeek());
                    } else {
                        String message = format("Overlay schedule dates %s do not overlap with date range %s for " +
                                        "schedule: %s impacted service id %s impacted calendar %s",
                                scheduleDateRange, impactedCalendarDateRange, schedule, serviceId, impactedCalendar);
                        logger.error(message);
                        throw new RuntimeException(message);
                    }
                }
            });
        }

        container.addService(service);
        serviceGroups.addService(uniqueTrainId, service);

        return service;
    }

    private void recordOverlapAsSuperseding(final MutableServiceCalendar calendarToUpdate, final DateRange rangeForUpdates,
                                            final EnumSet<DayOfWeek> excludedDays) {
        // TODO Need proper testing on this
        final TramDate toUpdateState = calendarToUpdate.getDateRange().getStartDate();
        final TramDate toUpdateEnd = calendarToUpdate.getDateRange().getEndDate();

        final TramDate updateStart = rangeForUpdates.getStartDate();
        final TramDate updateEnd = rangeForUpdates.getEndDate();
        final TramDate endDate = updateEnd.isAfter(toUpdateEnd) ? toUpdateEnd : updateEnd;

        TramDate current = updateStart.isBefore(toUpdateState) ? toUpdateState : updateStart;

        while (!current.isAfter(endDate)) {
            if (excludedDays.contains(current.getDayOfWeek())) {
                calendarToUpdate.excludeDate(current);
            }
            current = current.plusDays(1);
        }
    }

    public void recordSkip(final BasicSchedule basicSchedule) {
        skippedSchedules.add(basicSchedule.getUniqueTrainId());
    }

    private IdFor<Service> getServiceIdFor(final BasicSchedule schedule, final boolean isOverlay) {
        final DateRange dateRange = schedule.getDateRange();
        final String startDate = dateRange.getStartDate().format(RailTimetableMapper.dateFormatter);
        final String endDate = dateRange.getEndDate().format(RailTimetableMapper.dateFormatter);
        
        String text = schedule.getUniqueTrainId() +":"+ startDate +":"+ endDate;
        if (isOverlay) {
            text = text + "OVERLAY";
        }
        return Service.createId(text);
    }

    public void reportDiagnostics() {
        if (!unmatchedCancellations.isEmpty()) {
            logger.warn("The following " + unmatchedCancellations.size() +
                    " cancellations records were unmatched. unique train ids:" + unmatchedCancellations);;
        }
        if (!unmatchedOverlay.isEmpty()) {
            logger.warn("Failed to finding existing services for " + unmatchedOverlay.size() + " overlays. unique train ids:" + unmatchedOverlay);
        }

    }

    private static class ServiceGroups {
        private final Map<String, List<MutableService>> groups;

        private ServiceGroups() {
            groups = new HashMap<>();
        }

        public List<MutableService> servicesFor(String scheduleId) {
            if (!groups.containsKey(scheduleId)) {
                return Collections.emptyList();
            }
            return groups.get(scheduleId);
        }

        public void addService(final String scheduleId, final MutableService service) {
            if (!groups.containsKey(scheduleId)) {
                groups.put(scheduleId, new ArrayList<>());
            }
            groups.get(scheduleId).add(service);
        }
    }
}
