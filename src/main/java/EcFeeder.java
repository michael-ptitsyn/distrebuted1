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

public class EcFeeder extends EcRunnble implements Runnable {
    private Queue<Message> commandsQueue; //main thread to feed queue
    private QueueManager qManager;
    private S3Client s3Client;
    private String workQueue;
    private double numberOfMsgs;
    private EcManager ecManager;
    private HashMap<String, List<EcTask>> taskMapping;

    public EcFeeder(Queue<Message> commandsQueue, QueueManager manager, S3Client client, EcManager ecManager, String queueUrl, HashMap<String, List<EcTask>> tasks) {
        this.commandsQueue = commandsQueue;
        numberOfMsgs = 0;
        qManager = manager;
        this.ecManager = ecManager;
        workQueue = queueUrl;
        this.s3Client = client;
        this.taskMapping = tasks;
    }

    public synchronized void addTask(Message command) {
        this.commandsQueue.add(command);
    }

    @Override
    public void run() {
        String cmd;
        synchronized (commandsQueue) {
            while (!kill.booleanValue()) {
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
        numberOfMsgs = 0;
        String[] splited = cmd.getBody().split("\t");
        main.isTerminated.setValue(splited.length > 2 && splited[2].equals("terminate"));
        String inputUrl = splited[0];
        String outputFileName = splited[1];
        downloadFromUrl(inputUrl)
                .forEachRemaining(s -> {
                    packAndSend(s, workQueue, cmd.getMessageAttributes().get(Constants.MAC_FIELD).toString());
                });
        int neededEcs = (int) Math.ceil(numberOfMsgs / Constants.DEFAULT_MSG_COMP_RATION) - main.ec2Count;
        if (numberOfMsgs > 0 && neededEcs > 0) {
            newEcs = ecManager.createEc2(neededEcs, Constants.JAVA8IMG, Constants.WORKER_USER_SCRIPT);
            if (newEcs != null) {
                updateStatusMap(newEcs);
            }
            main.ec2Count += neededEcs;
        }
    }

    private void updateStatusMap(List<Instance> newEc2s) {
        for (Instance incs : newEc2s) {
            main.ec2StatusMapping.put(incs.getInstanceId(), main.ec2Status.IDLE);
        }
    }

    private void packAndSend(String msgLine, String queueUrl, String clientId) {
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
        int index = this.taskMapping.get(clientId)!=null?this.taskMapping.get(clientId).size():0;
        this.taskMapping.get(clientId).add(new EcTask(clientId, null, null,index));
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