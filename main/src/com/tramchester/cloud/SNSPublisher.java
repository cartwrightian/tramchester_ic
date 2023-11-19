package com.tramchester.cloud;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@LazySingleton
public class SNSPublisher {
    private static final Logger logger = LoggerFactory.getLogger(SNSPublisher.class);

    private final Map<String, String> topicMap;
    private SnsClient snsClient;

    @Inject
    public SNSPublisher() {
        topicMap = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        snsClient = SnsClient.create();
        logger.info("started");
    }

    public void send(String topic, String text) {
        String arn = createTopicIfRequire(topic);
        publish(arn, text);
    }

    private void publish(String arn, String text) {
        PublishRequest publishRequest = PublishRequest.builder().topicArn(arn).message(text).build();

        PublishResponse result = snsClient.publish(publishRequest);

        logger.info(format("Published message arn: %s messageId: %s", arn, result.messageId()));
    }

    public void subscribeQueueTo(String topicARN, String queueARN) {

        SubscribeRequest subscribeRequest = SubscribeRequest.builder().
                topicArn(topicARN).
                endpoint(queueARN).
                returnSubscriptionArn(true).
                protocol("sqs").
                build();

        SubscribeResponse result = snsClient.subscribe(subscribeRequest);

        String subscriptionArn = result.subscriptionArn();

        logger.info(format("Subscribed queue %s to sns topic %s with subscription %s", queueARN, topicARN, subscriptionArn ));

    }

    private String createTopicIfRequire(String requestedTopic) {
        if (!topicMap.containsKey(requestedTopic)) {
            logger.debug("need to fetch/create topic for " + requestedTopic);
            CreateTopicRequest createTopicRequest = CreateTopicRequest.builder().
                    name(requestedTopic).build();

            // idempotent
            CreateTopicResponse result = snsClient.createTopic(createTopicRequest);

            topicMap.put(requestedTopic, result.topicArn());
        }

        String topicArn = topicMap.get(requestedTopic);

        logger.info(format("topic: %s arn: %s", requestedTopic, topicArn));
        return topicArn;
    }

    public String getTopicUrnFor(String topicName) {
        return createTopicIfRequire(topicName);
    }
}
