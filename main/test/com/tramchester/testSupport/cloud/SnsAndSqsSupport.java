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

    public static String createOrGetTopic(SnsClient snsClient, String topicName) {
        CreateTopicRequest createTopicRequest = CreateTopicRequest.builder().name(topicName).build();

        CreateTopicResponse topicResult = snsClient.createTopic(createTopicRequest);

        return topicResult.topicArn();
    }
    
    public static QueueUrlAndArn createQueueIfNeeded(SqsClient sqsClient, String queueName) {

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

    public static void addPolicyForSNS(SqsClient sqsClient, QueueUrlAndArn urlAndArn, String topicArn) {
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

    public static void clearQueue(SqsClient sqsClient, QueueUrlAndArn urlAndArn) {
        // NOTE: can't use purge since can only be done every 60 seconds
//        PurgeQueueRequest purgeQueueRequest = PurgeQueueRequest.builder().queueUrl(urlAndArn.queueUrl).build();
//        sqsClient.purgeQueue(purgeQueueRequest);

        ReceiveMessageRequest receiveMsgRequest = ReceiveMessageRequest.builder().queueUrl(urlAndArn.queueUrl).waitTimeSeconds(2).build();
        ReceiveMessageResponse results = sqsClient.receiveMessage(receiveMsgRequest);

        List<Message> msgs = results.messages();

        if (msgs.isEmpty()) {
            return;
        }

        List<DeleteMessageBatchRequestEntry> entries = msgs.stream().
                map(msg -> DeleteMessageBatchRequestEntry.builder().id(msg.messageId()).build()).toList();
        DeleteMessageBatchRequest batchDeleteReq = DeleteMessageBatchRequest.builder().queueUrl(urlAndArn.queueUrl()).entries(entries).build();
        sqsClient.deleteMessageBatch(batchDeleteReq);
    }


    public record QueueUrlAndArn(String queueUrl, String queueArn) {
    }
}
