package com.tramchester.testSupport.testTags;


import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("liveDataMessages")
public @interface LiveDataMessagesTest {
    // use to exclude tests they rely on up-to-date messages being present in the tfgm live API
    // NOTE: these tests will fail if API key not available in env var TFGMAPIKEY
}
