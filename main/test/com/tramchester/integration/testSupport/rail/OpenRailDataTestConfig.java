package com.tramchester.integration.testSupport.rail;

import com.tramchester.config.OpenRailDataConfig;

public class OpenRailDataTestConfig implements OpenRailDataConfig {
    @Override
    public String getUsername() {
        return System.getenv("OPENRAILDATA_USERNAME");
    }

    @Override
    public String getPassword() {
        return System.getenv("OPENRAILDATA_PASSWORD");
    }

    @Override
    public String getAuthURL() {
        return "https://opendata.nationalrail.co.uk/authenticate";
    }
}
