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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

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

        String topicARN = snsPublisher.getTopicUrnFor(topicName);
        ConfiguredSQLSubscriber subscriber = new ConfiguredSQLSubscriber(sqsClient, queueUrl, topicARN);

        subscriber.updatePolicyFor();

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
        private final String topicARN;

        public ConfiguredSQLSubscriber(SqsClient sqsClient, String queueUrl, String topicARN) {
            this.sqsClient = sqsClient;
            this.queueUrl = queueUrl;
            this.topicARN = topicARN;
        }

        private List<Message> receiveMessageBatch(final int maxNumber, Duration timeout) {
            final List<Message> buffer = new LinkedList<>();
            LocalDateTime stopTime = LocalDateTime.now().plus(timeout);
            int countDown = maxNumber;
            Integer timeoutLong = Math.toIntExact(timeout.getSeconds());

            while(countDown>0 && LocalDateTime.now().isBefore(stopTime)) {
                ReceiveMessageRequest receiveMsgReq = ReceiveMessageRequest.builder().
                        queueUrl(queueUrl).
                        waitTimeSeconds(timeoutLong).
                        maxNumberOfMessages(10). // 10 is the max
                        build();

                ReceiveMessageResponse receiveResult = sqsClient.receiveMessage(receiveMsgReq);

                if (receiveResult.hasMessages()) {
                    List<Message> messages = receiveResult.messages();
                    buffer.addAll(messages);
                    countDown = countDown - messages.size();
                }
            }

            return buffer;
        }

        @Override
        public String receiveMessage() {
            List<Message> msgs = receiveMessageBatch(100, Duration.ofSeconds(5));

            if (msgs.isEmpty()) {
                logger.info("No messages received for " + queueUrl);
                return "";
            } else {
                logger.info(format("queue %s received %s messages", queueUrl, msgs.size()));
            }

            // only process and delete one message at a time due to way rest of live data works
            String text = extractRequiredBody(msgs);

            deleteMessages(msgs);

            return text;
        }

        private String extractRequiredBody(List<Message> msgs) {
            // discard if not SNS or if not expected topic
            List<JsonObject> parsedMessages = msgs.stream().
                    map(msg -> Jsoner.deserialize(msg.body(), new JsonObject())).
                    filter(json -> json.containsKey("TopicArn")).
                    filter(json -> topicARN.equals(json.get("TopicArn"))).
                    filter(json -> json.containsKey("Message")).
                    filter(json -> json.containsKey("Timestamp")).
                    toList();

            if (parsedMessages.isEmpty()) {
                logger.warn("No messages parsed successfully");
                return "";
            }
            List<JsonObject> sorted = parsedMessages.stream().sorted(this::compare).toList();
            if (sorted.isEmpty()) {
                logger.warn("No messages remaining after sorting");
            } else {
                logger.info("Successful got target message");
            }
            return extractPayloadFrom(sorted.get(0));
        }

        private int compare(JsonObject a, JsonObject b) {
            Instant timeA = Instant.from(ISO_INSTANT.parse((String) a.get("Timestamp")));
            Instant timeB = Instant.from(ISO_INSTANT.parse((String) b.get("Timestamp")));
            return timeB.compareTo(timeA);
        }

        private String getARN() {
            GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder().queueUrl(queueUrl).
                    attributeNames(QueueAttributeName.QUEUE_ARN).build();

            GetQueueAttributesResponse attributesResult = sqsClient.getQueueAttributes(getQueueAttributesRequest);

            return attributesResult.attributes().get(QueueAttributeName.QUEUE_ARN);
        }

        private void deleteMessages(List<Message> messages) {

            List<DeleteMessageBatchRequestEntry> entries = messages.stream().
                    map(msg -> DeleteMessageBatchRequestEntry.builder().id(msg.messageId()).build()).
                    toList();
            DeleteMessageBatchRequest batchRequest = DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(entries).build();
            sqsClient.deleteMessageBatch(batchRequest);

            logger.info(format("Queue: %s deleted %s messages", queueUrl, messages.size()));
        }

        private String extractPayloadFrom(JsonObject jsonObject) {
            if (jsonObject.containsKey("Message")) {
                return (String) jsonObject.get("Message");
            } else {
                logger.error(format("Matched message did not contain Message %s", jsonObject));
                return "";
            }
        }

        public void updatePolicyFor() {
            String queueARN = getARN();
            logger.info(format("Update policy for queue url: %s ARN: %s sns topic ARN: %s",
                    queueUrl, queueARN, topicARN));

            String policyStatement = createPolicyStatement(queueARN, topicARN);

            Map<QueueAttributeName, String> snsPublishAttributes = new HashMap<>();
            snsPublishAttributes.put(QueueAttributeName.POLICY, policyStatement);
            SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder().queueUrl(queueUrl).
                    attributes(snsPublishAttributes).build();

            sqsClient.setQueueAttributes(setQueueAttributesRequest);
        }

        private static String createPolicyStatement(String queueArn, String topicArn) {
            return String.format("""
                    {
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": {
                            "Service": "sns.amazonaws.com"
                          },
                          "Action": "sqs:SendMessage",
                          "Resource": "%s",
                          "Condition": {
                            "ArnEquals": {
                              "aws:SourceArn": "%s"
                            }
                          }
                        }
                      ]
                    }""", queueArn, topicArn);
        }
    }
}