package com.tramchester.testSupport.testTags;

import com.tramchester.testSupport.conditional.DisabledUntilDate;
import com.tramchester.testSupport.conditional.DisabledUntilDateCondition;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledUntilDateCondition.class)
@DisabledUntilDate(year = 2026, month = 10, day = 12)
public @interface ClosuresAutumn2026 {
    // revisit this, bus route trips seems to be missing at the moment
}
