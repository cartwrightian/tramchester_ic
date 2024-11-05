package com.tramchester.testSupport.testTags;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("liveDataInfra")
public @interface LiveDataInfraTest {
    // use to exclude tests they rely on the API end-point being available at all, will also want to exclude
    // LiveDataDueTramCategory and LiveDataMessagesCategory if the API is down
    // NOTE: these tests will fail if API key not available in env var TFGMAPIKEY
}
