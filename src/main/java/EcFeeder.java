import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class EcFeeder implements Runnable {
    private Queue<Message> commandsQueue; //main thread to feed queue
    private QueueManager qManager;
    private S3Client s3Client;
    private String workQueue;
    private double numberOfMsgs;
    private EcManager ecManager;

    public EcFeeder(Queue<Message> commandsQueue, QueueManager manager, S3Client client, EcManager ecManager, String queueUrl) {
        this.commandsQueue = commandsQueue;
        numberOfMsgs = 0;
        qManager = manager;
        this.ecManager=ecManager;
        workQueue = queueUrl;
        this.s3Client = client;
    }

    public synchronized void addTask(Message command) {
        this.commandsQueue.add(command);
    }

    @Override
    public void run() {
        String cmd;
        synchronized (commandsQueue) {
            while (true) {
                try {
                    if (commandsQueue.isEmpty()) {
                        try {
                            System.out.println("befor wait");
                            commandsQueue.wait();
                            System.out.println("after wait");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    handleTask(commandsQueue.remove());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    private void handleTask(Message cmd) throws IllegalArgumentException {
        //TODO syncronize isTerminated
        List<Instance> newEcs;
        if(!main.isTerminated) {
            numberOfMsgs = 0;
            String[] splited = cmd.getBody().split("\t");
            main.isTerminated = splited.length > 2 && splited[2].equals("terminate");
            String inputUrl = splited[0];
            String outputFileName = splited[1];
            downloadFromUrl(inputUrl)
                    .forEachRemaining(s -> {
                        packAndSend(s, workQueue);
                    });
            int neededEcs = (int) Math.ceil(numberOfMsgs / Constants.DEFAULT_MSG_COMP_RATION) - main.ec2Count;
            if (numberOfMsgs > 0 && neededEcs > 0) {
                newEcs = ecManager.createEc2(neededEcs, Constants.JAVA8IMG, null);
                if(newEcs!=null){
                    updateStatusMap(newEcs);
                }
                main.ec2Count += neededEcs;
            }
        }
    }

    private void updateStatusMap(List<Instance>newEc2s){
        for(Instance incs:newEc2s){
            main.ec2StatusMapping.put(incs.getInstanceId(), main.ec2Status.IDLE);
        }
    }

    private void packAndSend(String msgLine, String queueUrl) {
        String[] parts = msgLine.split("\t");
        HashMap<String, MessageAttributeValue> attributes = new HashMap<>();
        MessageAttributeValue action = new MessageAttributeValue();
        action.withDataType("String");
        action.setStringValue(parts[0]);
        MessageAttributeValue url = new MessageAttributeValue();
        url.setStringValue(parts[1]);
        url.withDataType("String");
        attributes.put("action", action);
        attributes.put("url", url);
        qManager.sendMessage(attributes, queueUrl);
        numberOfMsgs++;
    }

    private Iterator<String> downloadFromUrl(String url) {
        try {
            return S3Client.iteratorFromReader(new BufferedReader(new InputStreamReader(new BufferedInputStream(new URL(url).openStream()))));
        } catch (IOException ex) {
            // handle exception
            return new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return false;
                }
                @Override
                public String next() {
                    return null;
                }
            };
        }
    }
}