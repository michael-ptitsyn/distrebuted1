package aws;

import java.util.List;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import general.Constants;
import objects.EcTask;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import javax.annotation.Nullable;

public class QueueManager extends AwsManager {
    private AmazonSQS sqs;
    private List<String> qUrls;

    public QueueManager() {

        super();

        AmazonSQSClientBuilder temp = AmazonSQSClientBuilder.standard()
                .withRegion(Constants.DEFAULT_REGION);
        if(this.credentialsProvider!=null)
            temp.withCredentials(credentialsProvider);
        sqs = temp.build();
        qUrls = sqs.listQueues().getQueueUrls();
    }

    public String getOrCreate(String Creationname, @Nullable String qUrl) {
        if (qUrl!=null && qUrls.contains(qUrl))
            return qUrls.stream().filter(s -> s.equals(qUrl)).collect(Collectors.toList()).get(0);
        // Create a queue
        System.out.println("Creating a new SQS queue called "+Creationname+".\n");
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(Creationname + UUID.randomUUID());
        String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
        qUrls.add(myQueueUrl);
        return myQueueUrl;
    }

    public String getQueue(String qUrl) {
        if (qUrls.contains(qUrl)) {
            return qUrls.stream().filter(s -> s.equals(qUrl)).collect(Collectors.toList()).get(0);
        }
        int tries = Constants.TRIES_TO_GET_QUEUE;
        while(tries>0){
            qUrls = getQueues();
            if (qUrls.contains(qUrl)) {
                return qUrls.stream().filter(s -> s.equals(qUrl)).collect(Collectors.toList()).get(0);
            }
            tries--;
            try {
                Thread.sleep(Constants.THREAD_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("can't create queue");
    }

    public List<Message> getMessage(@Nullable String queueName, @Nullable String queueUrl, boolean create) {
        List<Message> result;
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        receiveMessageRequest.withWaitTimeSeconds(Constants.TIMEOUT).withMessageAttributeNames("All");
        if(queueUrl!=null && create && getOrCreate(queueName, queueUrl)!=null) {
            result = sqs.receiveMessage(receiveMessageRequest).getMessages();
            System.out.println("recieved " + result.size()+" messages");
            return result;
        }
        else {
            result = sqs.receiveMessage(receiveMessageRequest).getMessages();
            System.out.println("recieved " + result.size() + " messages");
            return result;
        }
    }

    public SendMessageResult sendMessage(String message, String url) {
        return sqs.sendMessage(new SendMessageRequest(url, message));
    }

    public SendMessageResult sendMessage(Map<String, MessageAttributeValue> attributes, String url, String body) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.withMessageBody(body);
        sendMessageRequest.withQueueUrl(url);
        sendMessageRequest.withMessageAttributes(attributes);
        return sqs.sendMessage(sendMessageRequest);
    }

    public SendMessageResult sendMessage(EcTask task, String url) {
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.withMessageBody(task.getBody());
        sendMessageRequest.withQueueUrl(url);
        sendMessageRequest.withMessageAttributes(task.getAttributes());
        return sqs.sendMessage(sendMessageRequest);
    }

    public DeleteQueueResult deleteQueue(String url) {
        DeleteQueueResult q = sqs.deleteQueue(new DeleteQueueRequest(url));
        qUrls.remove(url);
        return q;
    }

    public DeleteMessageResult deleteMsg(String queueUrl, Message msg){
        String messageReceiptHandle = msg.getReceiptHandle();
        return sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));
    }



    public List<String> getQueues() {
        return qUrls;
    }

    public List<String> getqUrls() {
        return qUrls;
    }


}