package com.tramchester.testSupport.cloud;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnsAndSqsSupport {

    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final Map<String, String> nameToUrl;
    private final Map<String, String> nameToArn;

    public SnsAndSqsSupport(SnsClient snsClient, SqsClient sqsClient) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        nameToUrl = new HashMap<>();
        nameToArn = new HashMap<>();
    }

    public String createOrGetTopic(final String topicName) {
        if (nameToArn.containsKey(topicName)) {
            return nameToArn.get(topicName);
        }

        CreateTopicRequest createTopicRequest = CreateTopicRequest.builder().name(topicName).build();

        CreateTopicResponse topicResult = snsClient.createTopic(createTopicRequest);

        String arn = topicResult.topicArn();
        nameToArn.put(topicName, arn);
        return arn;
    }
    
    public QueueUrlAndArn createQueueIfNeeded(final String queueName) {

        // idempotent if attributes the same
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().
                queueName(queueName).
                build();

        CreateQueueResponse queueResult = sqsClient.createQueue(createQueueRequest);
        String queueUrl = queueResult.queueUrl();

        GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder().queueUrl(queueUrl).
                attributeNames(QueueAttributeName.QUEUE_ARN).build();

        GetQueueAttributesResponse attributesResult = sqsClient.getQueueAttributes(getQueueAttributesRequest);

        String queueArn = attributesResult.attributes().get(QueueAttributeName.QUEUE_ARN);
        
        return new QueueUrlAndArn(queueUrl, queueArn);
    }

    public void addPolicyForSNS(QueueUrlAndArn urlAndArn, String topicArn) {
        Map<QueueAttributeName, String> snsPublishAttributes = new HashMap<>();
        String policy = createPolicyStatement(urlAndArn, topicArn);
        snsPublishAttributes.put(QueueAttributeName.POLICY, policy);
        SetQueueAttributesRequest setQueueAttributesRequest = SetQueueAttributesRequest.builder().queueUrl(urlAndArn.queueUrl()).
                attributes(snsPublishAttributes).build();

        sqsClient.setQueueAttributes(setQueueAttributesRequest);

    }

    private static String createPolicyStatement(SnsAndSqsSupport.QueueUrlAndArn urlAndArn, String topicArn) {
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
                "}", urlAndArn.queueArn(), topicArn);
    }

    public void clearQueue(QueueUrlAndArn urlAndArn) {
        // NOTE: can't use purge since can only be done every 60 seconds
//        PurgeQueueRequest purgeQueueRequest = PurgeQueueRequest.builder().queueUrl(urlAndArn.queueUrl).build();
//        sqsClient.purgeQueue(purgeQueueRequest);

        clearQueueByURL(urlAndArn.queueUrl());
    }

    public void clearQueueByName(final String queueName) {
        if (nameToUrl.containsKey(queueName)) {
            clearQueueByURL(nameToUrl.get(queueName));
            return;
        }

        // idempotent, to get the queue URL
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().
                queueName(queueName).
                build();

        CreateQueueResponse queueResult = sqsClient.createQueue(createQueueRequest);

        String queueUrl = queueResult.queueUrl();

        nameToUrl.put(queueName, queueUrl);

        clearQueueByURL(queueUrl);

    }

    private void clearQueueByURL(final String queueUrl) {

        // 10 is max number for the delete
        final ReceiveMessageRequest receiveMsgRequest = ReceiveMessageRequest.builder().
                queueUrl(queueUrl).
                maxNumberOfMessages(10).
                waitTimeSeconds(1).build();

        ReceiveMessageResponse results = sqsClient.receiveMessage(receiveMsgRequest);
        List<Message> msgs = results.messages();

        while (!msgs.isEmpty()) {

            List<DeleteMessageBatchRequestEntry> entries = msgs.stream().
                    map(msg -> DeleteMessageBatchRequestEntry.builder().id(msg.messageId()).build()).
                    toList();
            DeleteMessageBatchRequest batchDeleteReq = DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(entries).build();
            sqsClient.deleteMessageBatch(batchDeleteReq);

            results = sqsClient.receiveMessage(receiveMsgRequest);
            msgs = results.messages();
        }
    }

    public record QueueUrlAndArn(String queueUrl, String queueArn) {
    }
}
