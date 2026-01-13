package com.tramchester.integration.cloud;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.cloud.SQSSubscriber;
import com.tramchester.cloud.SQSSubscriberFactory;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.cloud.SnsAndSqsSupport;
import com.tramchester.testSupport.conditional.RequiresNetwork;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresNetwork
public class SQSSubscriberFactoryTest {
    private static ComponentContainer componentContainer;
    private static SnsClient snsClient;
    private static IntegrationTramTestConfig config;
    private static SqsClient sqsClient;
    private static String queueName;
    private static SnsAndSqsSupport snsAndSqsSupport;
    private SQSSubscriberFactory factory;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.EnabledWithSNS);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        snsClient = SnsClient.create();
        sqsClient = SqsClient.create();

        queueName = TestEnv.getTestQueueName();

        snsAndSqsSupport = new SnsAndSqsSupport(snsClient, sqsClient);

        // rate limit on this call, once every 60 seconds
        purgeQueue();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        factory = componentContainer.get(SQSSubscriberFactory.class);
        snsAndSqsSupport.clearQueueByName(queueName);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
        snsAndSqsSupport.clearQueueByName(queueName);

    }

    @Test
    void shouldCreateSubscriberAndReceiveMsg() {
        String text = "someTextForInsideTheMessage";

        long retentionPeriodSeconds = 60;
        SQSSubscriber subscriber = factory.getFor(queueName, config.getLiveDataSNSPublishTopic(), retentionPeriodSeconds);

        String topicArn = snsAndSqsSupport.createOrGetTopic(config.getLiveDataSNSPublishTopic());

        PublishRequest publishRequest = PublishRequest.builder().
                topicArn(topicArn).
                message(text).build();
        snsClient.publish(publishRequest);

        String result = subscriber.receiveMessage();

        assertEquals(text, result);

        ReceiveMessageRequest checkIfDeleted = ReceiveMessageRequest.builder().
                queueUrl(queueName).
                waitTimeSeconds(2).
                maxNumberOfMessages(10). // 10 is the max
                        build();

        ReceiveMessageResponse response = sqsClient.receiveMessage(checkIfDeleted);

        List<Message> anyMessages = response.messages();

        assertTrue(anyMessages.isEmpty(), "Unexpected messages " + anyMessages);

    }

    private static void purgeQueue() {
        PurgeQueueRequest purgeRequest = PurgeQueueRequest.builder().
                queueUrl(queueName).
                build();

        try {
            sqsClient.purgeQueue(purgeRequest);
        } catch (PurgeQueueInProgressException ignored) {
            // rate limited, failure will be in logs
        }
    }

    @Test
    void shouldCreateSubscriberAndReceiveNewestMessage() throws InterruptedException {

        long retentionPeriodSeconds = 60;
        SQSSubscriber subscriber = factory.getFor(queueName, config.getLiveDataSNSPublishTopic(), retentionPeriodSeconds);

        String topicArn = snsAndSqsSupport.createOrGetTopic(config.getLiveDataSNSPublishTopic());

        // not batch because testing the timestamp ordering in the receiver here
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
