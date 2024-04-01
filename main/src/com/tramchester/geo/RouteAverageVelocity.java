package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.Durations;
import com.tramchester.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

import javax.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.measure.quantity.Speed;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static tech.units.indriya.unit.Units.KILOMETRE_PER_HOUR;
import static tech.units.indriya.unit.Units.METRE_PER_SECOND;

@LazySingleton
public class RouteAverageVelocity {
    private static final Logger logger = LoggerFactory.getLogger(RouteAverageVelocity.class);

    private final RouteRepository routeRepository;

    private static final Duration minLegTime = Duration.ofMinutes(1);

    private double metersPerSecond;

    @Inject
    public RouteAverageVelocity(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @PostConstruct
    public void start() {
        logger.info("started");
        metersPerSecond = calculateAverageVelocityInMS();
        final ComparableQuantity<Speed> asQuantity = Quantities.getQuantity(metersPerSecond, METRE_PER_SECOND);
        final ComparableQuantity<Speed> khp = asQuantity.to(KILOMETRE_PER_HOUR);
        logger.info("started, average velocity across all routes is " + asQuantity + " (" +khp + ")");
    }


    public double getVelocityInMetersPerSecond() {
        return metersPerSecond;
    }

    double calculateAverageVelocityInMS() {
        Optional<Double> find = routeRepository.getRoutes().stream().
                map(this::getVelocityFor).
                filter(value -> value>0).
                max(Double::compare);
        return find.orElse(-1D);
    }

    private double getVelocityFor(final Route route) {
        Optional<Double> find = route.getTrips().stream().
                map(this::getVelocityForTrip).
                filter(value -> value>0).
                max(Double::compare);
        return find.orElse(-1D);
    }

    private double getVelocityForTrip(final Trip trip) {
        final List<StopCalls.StopLeg> legs = trip.getStopCalls().getLegs(false);
        OptionalDouble findAverage = legs.stream().
                filter(leg -> Durations.greaterThan(leg.getCost(), minLegTime)).
                mapToDouble(this::velocityForLeg).
                filter(value -> value>0).
                average();
        if (findAverage.isPresent()) {
            return findAverage.getAsDouble();
        } else {
            return -1;
        }
    }

    private double velocityForLeg(final StopCalls.StopLeg leg) {
        final Station stationA = leg.getFirstStation();
        final Station stationB = leg.getSecondStation();

        final GridPosition gridA = stationA.getGridPosition();
        final GridPosition gridB = stationB.getGridPosition();

        if (gridA.isValid() && gridB.isValid()) {
            final long distanceInMeters = GridPositions.distanceTo(gridA, gridB);
            final Duration timing = leg.getCost();
            return (double) distanceInMeters / timing.toSeconds();
        } else {
            return -1;
        }
    }

}
