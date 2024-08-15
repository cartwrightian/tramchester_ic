package com.tramchester.unit.dataimport.data;


import com.tramchester.domain.FeedInfo;
import com.tramchester.unit.dataimport.ParserTestCSVHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedInfoDataParserTest extends ParserTestCSVHelper<FeedInfo> {
    private static final String feedInfo = "feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,20160530,20160615,feed_version";

    @BeforeEach
    void beforeEach() {
        super.before(FeedInfo.class, "feed_publisher_name,feed_publisher_url,feed_timezone,feed_lang,feed_valid_from,feed_valid_to,feed_version");
    }

    @Test
    void shouldParserFeedInfo() {

        FeedInfo info = parse(feedInfo);
        assertEquals(info.getPublisherName() , "feed_publisher_name");
        assertEquals(info.getPublisherUrl() , "feed_publisher_url");
        assertEquals(info.getTimezone() , "feed_timezone");
        assertEquals(info.getLang() , "feed_lang");
        assertEquals(info.version() , "feed_version");
        assertEquals(info.validFrom() , LocalDate.of(2016,5,30));
        assertEquals(info.validUntil() , LocalDate.of(2016,6,15));
    }
}
