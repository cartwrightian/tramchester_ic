package com.tramchester.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenRailDataAppConfig implements OpenRailDataConfig {


    private final String username;
    private final String password;
    private final String authURL;

    public OpenRailDataAppConfig(@JsonProperty(value ="username", required = true) String username,
                                 @JsonProperty(value="password", required = true) String password,
                                 @JsonProperty(value="authURL", required = true) String authURL) {

        this.username = username;
        this.password = password;
        this.authURL = authURL;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getAuthURL() {
        return authURL;
    }
}
