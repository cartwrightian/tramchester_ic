package com.tramchester.integration.cloud;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.cloud.SQSSubscriber;
import com.tramchester.cloud.SQSSubscriberFactory;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.cloud.SnsAndSqsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQSSubscriberFactoryTest {
    private static ComponentContainer componentContainer;
    private static SnsClient snsClient;
    private static IntegrationTramTestConfig config;
    private static SqsClient sqsClient;
    private static String queueName;
    private SQSSubscriberFactory factory;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.EnabledWithSNS);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        snsClient = SnsClient.create();
        sqsClient = SqsClient.create();

        queueName = TestEnv.getTestQueueName();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        factory = componentContainer.get(SQSSubscriberFactory.class);
        SnsAndSqsSupport.clearQueueByName(sqsClient, queueName);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
        SnsAndSqsSupport.clearQueueByName(sqsClient, queueName);
    }

    @Test
    void shouldCreateSubscriberAndReceiveMsg() {
        String text = "someTextForInsideTheMessage";

        long retentionPeriodSeconds = 60;
        SQSSubscriber subscriber = factory.getFor(queueName, config.getLiveDataSNSTopic(), retentionPeriodSeconds);

        String topicArn = SnsAndSqsSupport.createOrGetTopic(snsClient, config.getLiveDataSNSTopic());

        PublishRequest publishRequest = PublishRequest.builder().
                topicArn(topicArn).
                message(text).build();
        snsClient.publish(publishRequest);

        String result = subscriber.receiveMessage();

        assertEquals(text, result);

    }

    @Test
    void shouldCreateSubscriberAndReceiveNewestMessage() throws InterruptedException {

        long retentionPeriodSeconds = 60;
        SQSSubscriber subscriber = factory.getFor(queueName, config.getLiveDataSNSTopic(), retentionPeriodSeconds);

        String topicArn = SnsAndSqsSupport.createOrGetTopic(snsClient, config.getLiveDataSNSTopic());

        for (int i = 0; i < 10; i++) {
            String text = "messageNumber"+i;
            PublishRequest publishRequest = PublishRequest.builder().
                    topicArn(topicArn).
                    message(text).build();
            snsClient.publish(publishRequest);
            Thread.sleep(10);
        }

        String result = subscriber.receiveMessage();

        assertEquals("messageNumber9", result);

    }
}
