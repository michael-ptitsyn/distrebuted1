import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import javax.annotation.Nullable;

public class QueueManager extends AwsManager {
    private AmazonSQS sqs;
    private List<String> qUrls;

    public QueueManager() {

        super();
        sqs = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Constants.DEFAULT_REGION)
                .build();
        qUrls = sqs.listQueues().getQueueUrls();
    }

    public String getOrCreate(String Creationname, @Nullable String qUrl) {
        if (qUrl!=null && qUrls.contains(qUrl))
            return qUrls.stream().filter(s -> s.equals(qUrl)).collect(Collectors.toList()).get(0);
        // Create a queue
        System.out.println("Creating a new SQS queue called MyQueue.\n");
        CreateQueueRequest createQueueRequest = new CreateQueueRequest("MyQueue" + UUID.randomUUID());
        String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
        qUrls.add(myQueueUrl);
        return myQueueUrl;

    }

    public List<Message> getMessage(@Nullable String queueName, @Nullable String queueUrl, boolean create) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        receiveMessageRequest.withWaitTimeSeconds(Constants.TIMEOUT);
        if(queueUrl!=null && create && getOrCreate(queueName, queueUrl)!=null) {
            return sqs.receiveMessage(receiveMessageRequest).getMessages();
        }
        else
            return sqs.receiveMessage(receiveMessageRequest).getMessages();
    }

    public SendMessageResult sendMessage(String message, String url) {
        return sqs.sendMessage(new SendMessageRequest(url, message));
    }

    public DeleteQueueResult deleteQueue(String url, String message) {
        sqs.deleteQueue(new DeleteQueueRequest(url));
        qUrls.remove(url);
        return sqs.deleteQueue(new DeleteQueueRequest(url));
    }

    public List<String> getQueues() {
        return qUrls;
    }

    public List<String> getqUrls() {
        return qUrls;
    }


}