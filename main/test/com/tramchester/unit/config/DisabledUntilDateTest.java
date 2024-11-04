package com.tramchester.unit.config;

import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.conditional.DisabledUntilDate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("WIP")
public class DisabledUntilDateTest {

    private TramDate today;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        today = TramDate.from(TestEnv.LocalNow());
    }

    @Test
    void shouldNotHaveAnyExpiredDisabledTests() {

        List<Boolean> expired = getExpired(App.class.getPackageName(), today);

        assertTrue(expired.isEmpty(), expired.toString());
    }

    @DisabledUntilDate(year = 2010, month = 11, day = 30)
    @Test
    void shouldBeExpired() {
        @NotNull List<Boolean> expired = getExpired(DisabledUntilDateTest.class.getPackageName(), today);

        assertEquals(1, expired.size());
    }

//    @Test
//    void shouldFindAnnotations() {
//
//        Package pack = DisabledUntilDateTest.class.getPackage();
//
//        pack.
//
//        List<Method> tests = Arrays.stream(DisabledUntilDateTest.class.getDeclaredMethods()).
//                filter(method -> method.getAnnotation(Test.class) != null).
//                toList();
//
//        assertEquals(3, tests.size(), tests.toString());
//
////        Set<Method> haveAnnotation = reflections.getMethodsAnnotatedWith(DisabledUntilDate.class);
////
////        assertEquals(1, haveAnnotation.size());
//
//    }

    private @NotNull List<Boolean> getExpired(String packageName, TramDate now) {
        Reflections reflections = new Reflections(packageName);

        Set<Class<?>> havePath = reflections.getTypesAnnotatedWith(DisabledUntilDate.class);

        List<Boolean> expired = havePath.stream().
                map(klass -> klass.getAnnotation(DisabledUntilDate.class)).
                map(annotation -> expired(now, annotation)).
                toList();
        return expired;
    }

    private boolean expired(TramDate now, DisabledUntilDate annotation) {
        TramDate annotationDate = TramDate.of(annotation.year(), annotation.month(), annotation.day());
        return annotationDate.isAfter(now);
    }
}
