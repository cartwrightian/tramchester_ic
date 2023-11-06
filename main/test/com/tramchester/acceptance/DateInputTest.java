package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.ProvidesChromeDateInput;
import com.tramchester.acceptance.infra.ProvidesFirefoxDateInput;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.testSupport.testTags.LocaleTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@LocaleTest
class DateInputTest {

    private final ProvidesDateInput firefoxProvider = new ProvidesFirefoxDateInput();
    private final ProvidesDateInput chromeProvider = new ProvidesChromeDateInput();


    // NOTE: issues with selenium and web driver if locale not set as expected, so check

    @Test
    void shouldHaveExpectedLocale() {
        Locale current = Locale.getDefault();

        assertEquals("en", current.getLanguage());

        assertEquals("GB", current.getCountry());
    }

    @Test
    void shouldGetFirefoxDateCorrectly() {
        LocalDate date = LocalDate.of(2019, 11, 30);
        String result = firefoxProvider.createDateInput(date);

        // UK is30112019
        assertEquals(10, result.length(), result);
        assertTrue(result.contains("30"));
        assertTrue(result.contains("11"));
        assertTrue(result.contains("2019"));
    }

    @Test
    void shouldGetChromeDateCorrectly() {
        LocalDate date = LocalDate.of(2019, 11, 30);
        String result = chromeProvider.createDateInput(date);

        // actual ordering is locale specific, which is needed to support browser running in other locals i.e. on CI box
        assertEquals(8, result.length(), result);
        assertTrue(result.contains("30"));
        assertTrue(result.contains("11"));
        assertTrue(result.contains("2019"));
    }

    @Test
    void shouldFirefoxTimeCorrecly() {
        // checking for 24h clock
        String result = firefoxProvider.createTimeFormat(LocalTime.of(14,45));
        assertTrue(result.startsWith("14"), "expected start of " + result);
        assertTrue(result.endsWith("45"), "expected end of " + result);
    }

    @Test
    void shouldGetChromeTimeCorrectly() {
        // checking for 24h clock
        String result = chromeProvider.createTimeFormat(LocalTime.of(16,55));
        assertTrue(result.startsWith("16"), "expected start of " + result);
        assertTrue(result.endsWith("55"), "expected end of " +result);
    }



}
