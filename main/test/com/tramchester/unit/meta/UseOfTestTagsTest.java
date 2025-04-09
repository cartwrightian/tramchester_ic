package com.tramchester.unit.meta;

import com.tramchester.App;
import com.tramchester.testSupport.testTags.DualTest;
import com.tramchester.testSupport.testTags.GMTest;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UseOfTestTagsTest {

    private static Reflections reflections;

    @BeforeAll
    public static void onceBeforeAnyTestsRun() {
        reflections = new Reflections(App.class.getPackageName());
    }

    @Test
    void shouldNotHaveGMTestAndDualTestOnOneTest() {
        Set<String> withGMTest = reflections.getTypesAnnotatedWith(GMTest.class).stream().map(Class::getCanonicalName).collect(Collectors.toSet());
        Set<String> withDualTest = reflections.getTypesAnnotatedWith(DualTest.class).stream().map(Class::getCanonicalName).collect(Collectors.toSet());

        SetUtils.SetView<String> intersection = SetUtils.intersection(withGMTest, withDualTest);

        assertTrue(intersection.isEmpty(), intersection.toString());

    }
}
