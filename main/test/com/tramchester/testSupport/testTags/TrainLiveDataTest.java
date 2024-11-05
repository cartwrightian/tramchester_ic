package com.tramchester.testSupport.testTags;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Tag("TrainLiveData")
public @interface TrainLiveDataTest {
    // NOTE: these tests will fail if API key not available in env var OPENLDB_APIKEY
}
