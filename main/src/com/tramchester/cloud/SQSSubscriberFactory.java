package com.tramchester.cloud;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@LazySingleton
public class SQSSubscriberFactory {
    private static final Logger logger = LoggerFactory.getLogger(SQSSubscriberFactory.class);
    public static final int DEFAULT_MSG_RECEIVE_TIMEOUT = 5;

    private SqsClient sqsClient;
    private final SNSPublisher snsPublisher;

    @Inject
    public SQSSubscriberFactory(SNSPublisher snsPublisher) {
        this.snsPublisher = snsPublisher;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        sqsClient = SqsClient.create();
        logger.info("started");
    }

    /***
     * create sqs queue subscriber for a SNS topic , note changing retention period might require delete of the queue
     * @param queueName name of the queue, created if needed
     * @param topicName name of the SNS topic
     * @param retentionPeriodSeconds lifetime of messages in the queue
     * @return the subscriber
     */
    public SQSSubscriber getFor(String queueName, String topicName, long retentionPeriodSeconds) {
        logger.info(format("Get subscriber for queue %s and topic %s retention %s ", queueName, topicName, retentionPeriodSeconds));

        String queueUrl = createQueueIfNeeded(queueName, retentionPeriodSeconds);

        ConfiguredSQLSubscriber subscriber = new ConfiguredSQLSubscriber(sqsClient, queueUrl);

        String topicARN = snsPublisher.getTopicUrnFor(topicName);
        subscriber.updatePolicyFor(topicARN);

        String queueArn = subscriber.getARN();
        snsPublisher.subscribeQueueTo(topicARN, queueArn);

        return subscriber;
    }

    private String createQueueIfNeeded(String queueName, long retentionPeriodSeconds) {
        // idempotent if request same each time

        Map<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, Long.toString(retentionPeriodSeconds));
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(queueName).
                attributes(attributes).build();

        CreateQueueResponse result = sqsClient.createQueue(createQueueRequest);

        String queueUrl = result.queueUrl();
        logger.info(format("Got/Create queue %s url: %s retention %s seconds",queueName, queueUrl, retentionPeriodSeconds));
        return queueUrl;
    }

    private static class ConfiguredSQLSubscriber implements SQSSubscriber {
        private static final Logger logger = LoggerFactory.getLogger(ConfiguredSQLSubscriber.class);

        private final SqsClient sqsClient;
        private final String queueUrl;

        public ConfiguredSQLSubscriber(SqsClient sqsClient, String queueUrl) {
            this.sqsClient = sqsClient;
            this.queueUrl = queueUrl;
        }

        @Override
        public String receiveMessage() {
            ReceiveMessageRequest receiveMsgReq = ReceiveMessageRequest.builder().
                    queueUrl(queueUrl).
                    waitTimeSeconds(DEFAULT_MSG_RECEIVE_TIMEOUT).
                    maxNumberOfMessages(1).
                    build();

            ReceiveMessageResponse receiveResult = sqsClient.receiveMessage(receiveMsgReq);

            List<Message> msgs = receiveResult.messages();

            if (msgs.isEmpty()) {
                logger.info("No messages received for " + queueUrl);
                return "";
            }

            logger.debug("Received " + msgs.size() + " messages for queue " + queueUrl);

            // only process and delete one message at a time due to way rest of live data works
            Message message = msgs.get(0);

            logger.info(format("queue %s received msg %s", queueUrl, message.messageId()));

            String text = extractPayloadFrom(message);

            deleteMessage(message);

            return text;
        }

        private String getARN() {
            GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder().queueUrl(queueUrl).
                    attributeNames(QueueAttributeName.QUEUE_ARN).build();

            GetQueueAttributesResponse attributesResult = sqsClient.getQueueAttributes(getQueueAttributesRequest);

            return attributesResult.attributes().get(QueueAttributeName.QUEUE_ARN);
        }

        private void deleteMessage(Message message) {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().
                    queueUrl(queueUrl).
                    receiptHandle(message.receiptHandle()).build();

            sqsClient.deleteMessage(deleteMessageRequest);

            logger.info(format("Queue: %s deleted message %s", queueUrl, message.messageId()));
        }

        private String extractPayloadFrom(Message message) {
            final String body = message.body();
            JsonObject parsed = Jsoner.deserialize(body, new JsonObject());
            if (parsed.containsKey("Message")) {
                return (String) parsed.get("Message");
            } else {
                logger.error(format("Received message %s did not contain Message key, got %s", message.messageId(), body));
                return "";
            }
        }

        public void updatePolicyFor(String topicArn) {
            String queueARN = getARN();
            logger.info(format("Update policy for queue url: %s ARN: %s sns topic ARN: %s",
                    queueUrl, queueARN, topicArn));

            String policyStatement = createPolicyStatement(queueARN, topicArn);

            Map<QueueAttributeName, String> snsPublishAttributes = new HashMap<>();
            snsPublishAttributes.put(QueueAttributeName.POLICY, policyStatement);
            SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder().queueUrl(queueUrl).
                    attributes(snsPublishAttributes).build();

            sqsClient.setQueueAttributes(setQueueAttributesRequest);
        }

        private static String createPolicyStatement(String queueArn, String topicArn) {
            return String.format("{\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": {\n" +
                    "        \"Service\": \"sns.amazonaws.com\"\n" +
                    "      },\n" +
                    "      \"Action\": \"sqs:SendMessage\",\n" +
                    "      \"Resource\": \"%s\",\n" +
                    "      \"Condition\": {\n" +
                    "        \"ArnEquals\": {\n" +
                    "          \"aws:SourceArn\": \"%s\"\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}", queueArn, topicArn);
        }
    }
}
