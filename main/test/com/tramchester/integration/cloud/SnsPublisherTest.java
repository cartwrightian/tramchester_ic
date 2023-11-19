package com.tramchester.integration.cloud;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.cloud.SNSPublisher;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.cloud.SnsAndSqsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

import static com.tramchester.testSupport.TestEnv.TEST_SQS_QUEUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnsPublisherTest {
    private static ComponentContainer componentContainer;
    private static SqsClient sqsClient;
    private static SnsClient snsClient;
    private static String subscriptionArn;
    private static IntegrationTramTestConfig config;
    private static SnsAndSqsSupport.QueueUrlAndArn urlAndArn;
    private SNSPublisher snsPublisher;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig(IntegrationTramTestConfig.LiveData.EnabledWithSNS);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        sqsClient = SqsClient.create();
        snsClient = SnsClient.create();

        subscribeToTestMessages();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        unsubscribeFromTestMessages();
        componentContainer.close();
    }


    @BeforeEach
    void onceBeforeEachTestRuns() {
        snsPublisher = componentContainer.get(SNSPublisher.class);
    }

    @Test
    void shouldPublishAMessage() {
        ReceiveMessageRequest receiveMsgReq = ReceiveMessageRequest.builder().queueUrl(urlAndArn.queueUrl()).
                waitTimeSeconds(20).
                build();

        snsPublisher.send(config.getLiveDataSNSTopic(), "soLongAndThanksForAllTheFish");

        ReceiveMessageResponse receiveResult = sqsClient.receiveMessage(receiveMsgReq);

        List<Message> msgs = receiveResult.messages();

        assertEquals(1, msgs.size());

    }

    static void subscribeToTestMessages() {
        String topicName = config.getLiveDataSNSTopic();

        String topicArn = SnsAndSqsSupport.createOrGetTopic(snsClient, topicName);

        urlAndArn = SnsAndSqsSupport.createQueueIfNeeded(sqsClient, TEST_SQS_QUEUE);

        SnsAndSqsSupport.addPolicyForSNS(sqsClient, urlAndArn, topicArn);

        SubscribeRequest subscribeRequest = SubscribeRequest.builder().
                topicArn(topicArn).
                endpoint(urlAndArn.queueArn()).
                returnSubscriptionArn(true).
                protocol("sqs").
                build();

        SubscribeResponse result = snsClient.subscribe(subscribeRequest);

        subscriptionArn = result.subscriptionArn();

    }


    private static void unsubscribeFromTestMessages() {
        UnsubscribeRequest unsubscribeRequest = UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build();

        snsClient.unsubscribe(unsubscribeRequest);

        SnsAndSqsSupport.clearQueue(sqsClient, urlAndArn);
    }
}
