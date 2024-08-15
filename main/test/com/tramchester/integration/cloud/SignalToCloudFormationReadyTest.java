package com.tramchester.integration.cloud;

import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SignalToCloudformationReady;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalToCloudFormationReadyTest {

    @Test
    void shouldSignalWhenUserDataContainsCallbackURL() throws Exception {

        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "# WAITURL=http://localhost:8080/callbackURL");
        providesConfig.start();
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPutData();
        assertTrue(sentData.contains("\"Status\": \"SUCCESS\""));
        assertTrue(sentData.contains("\"Reason\": \"Web Server started\""));

        assertTrue(stubbedServer.getContentHeader().isEmpty()); // aws requires this header is empty or not set
    }

    @Test
    void shouldNotSignalWhenUserDataDoesNotContainCallbackURL() throws Exception {
        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "# NOTHERE=http://localhost:8080/callbackURL");
        providesConfig.start();
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPutData();
        assertNull(sentData);
    }

    @Test
    void shouldNotSignalWhenNoUserDataAvailable() throws Exception {
        StubbedAWSServer stubbedServer = new StubbedAWSServer();
        stubbedServer.run();

        ConfigFromInstanceUserData providesConfig = new ConfigFromInstanceUserData(() -> "");
        providesConfig.start();
        SignalToCloudformationReady signaller = new SignalToCloudformationReady(providesConfig);

        signaller.send();
        stubbedServer.stopServer();

        String sentData = stubbedServer.getPutData();
        assertNull(sentData);

    }
}
