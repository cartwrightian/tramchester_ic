package com.tramchester.testSupport.reference;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.UpcomingDates.*;

public class KnownTramRoute {

    public static final TramDate marchCutoverC = TramDate.of(2025,3,25);

    // missing from tfgm data
    public static final String MISSING_ROUTE = "";

    /***
     * @return Yellow route
     */
    public static @NotNull TestRoute getPiccadillyVictoria(TramDate date) {
        return findFor(KnownLines.Yellow, date);
    }

    /***
     * @return Red route
     */
    public static @NotNull TestRoute getCornbrookTheTraffordCentre(TramDate date) {
        return findFor(KnownLines.Red, date);
    }

    /***
     * @return Purple route
     */
    public static @NotNull TestRoute getEtihadPiccadillyAltrincham(TramDate date) {
        return findFor(KnownLines.Purple, date);
    }

    /***
     * @return Pink route
     */
    public static @NotNull TestRoute getShawandCromptonManchesterEastDidisbury(TramDate date) {
        return findFor(KnownLines.Pink, date);
    }

    /***
     * @return Navy route
     */
    public static @NotNull TestRoute getDeansgateManchesterAirport(TramDate date) {
        return findFor(KnownLines.Navy, date);
    }

    /***
     * @return Green route
     */
    public static @NotNull TestRoute getBuryManchesterAltrincham(TramDate date) {
        return findFor(KnownLines.Green, date);
    }

    /***
     * @return Blue route
     */
    public static @NotNull TestRoute getEcclesAshton(TramDate date) {
        return findFor(KnownLines.Blue, date);
    }

    /***
     * @return Brown route
     */

    private static TestRoute getFirswoodEastDidsbury(TramDate date) {
        return findFor(KnownLines.Brown, date);
    }

    public static TestRoute findFor(final KnownLines line, final TramDate date) {
        List<KnownTramRouteEnum> find = Arrays.stream(KnownTramRouteEnum.values()).
                filter(knownTramRoute -> knownTramRoute.line().equals(line)).
                filter(knownTramRoute -> date.isEqual(knownTramRoute.getValidFrom()) || date.isAfter(knownTramRoute.getValidFrom())).
                sorted(Comparator.comparing(KnownTramRouteEnum::getValidFrom)).
                toList();
        if (find.isEmpty()) {
            throw new RuntimeException("No match for " + line.getShortName() + " on " + date);
        }
        final KnownTramRouteEnum matched = find.getLast();
        if (!matched.getId().isValid()) {
            throw new RuntimeException(matched + " is not valid for date " + date);
        }
        return matched;
     }

    public static Set<TestRoute> find(final Set<Route> routes) {
        return routes.stream().
                map(Route::getId).
                map(KnownTramRoute::fromId).
                collect(Collectors.toSet());
    }

    private static TestRoute fromId(final IdFor<Route> routeId) {
        Optional<KnownTramRouteEnum> find = Arrays.stream(KnownTramRouteEnum.values()).
                filter(knownTramRoute -> knownTramRoute.getId().equals(routeId)).findFirst();
        if (find.isPresent()) {
            return find.get();
        } else {
            throw new RuntimeException("Could not find a known tram route with ID " + routeId);
        }
    }

    public static int numberOn(final TramDate date) {
        return getFor(date).size();
    }

    public static Set<TestRoute> getFor(final TramDate date) {

        final Set<TestRoute> routes = new HashSet<>();

        routes.add(getBuryManchesterAltrincham(date));

        TramDate april2025CutOver = TramDate.of(2025, 4, 24);

        if (!(DeansgateTraffordBarWorks.contains(date) || EndMarchNotOnTFGMSite.contains(date)
                || HighStreetAndChurchStreetWorks.contains(date) || date.isAfter(april2025CutOver))) {
            routes.add(getEtihadPiccadillyAltrincham(date));
        }

        if (DeansgateTraffordBarWorks.contains(date) ) {
            routes.add(getFirswoodEastDidsbury(date));
        }

        if (!(HighStreetAndChurchStreetWorks.contains(date) || date.isAfter(april2025CutOver))) {
            routes.add(getPiccadillyVictoria(date));
        }

        routes.add(getEcclesAshton(date));
        routes.add(getCornbrookTheTraffordCentre(date));
        routes.add(getDeansgateManchesterAirport(date));
        routes.add(getShawandCromptonManchesterEastDidisbury(date));

        return routes;
    }

    public static TestRoute[] values() {
        return KnownTramRouteEnum.values();
    }
}
