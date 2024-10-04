package com.tramchester.testSupport.testTags;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Tag("CornbrookClosure2024")
public @interface CornbrookClosure2024 {
    // NOTE: this tests fail if data expires within 7 days
}
