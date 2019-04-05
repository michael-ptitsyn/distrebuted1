import aws.EcManager;
import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import general.Constants;
import objects.EcRunnble;
import objects.EcTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static general.GeneralFunctions.createAttrs;

public class EcFeeder extends EcRunnble implements Runnable {
    private Queue<Message> commandsQueue; //main thread to feed queue
    private QueueManager qManager;
    private S3Client s3Client;
    private String workQueue;
    private double numberOfMsgs;
    private EcManager ecManager;
    private ConcurrentHashMap<String, List<EcTask>> taskMapping;

    public EcFeeder(Queue<Message> commandsQueue, QueueManager manager, S3Client client, EcManager ecManager, String queueUrl, ConcurrentHashMap<String, List<EcTask>> tasks) {
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
                    if(!commandsQueue.isEmpty()){
                        handleTask(commandsQueue.remove());
                    }
                    else {
                        try {
                            System.out.println("befor wait");
                            commandsQueue.wait();
                            System.out.println("after wait");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
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
                    packAndSend(s, workQueue, cmd.getMessageAttributes().get(Constants.MAC_FIELD).getStringValue());
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
            main.ec2StatusMapping.put(incs.getInstanceId(), Constants.ec2Status.IDLE);
        }
    }

    private void packAndSend(String msgLine, String queueUrl, String clientId) {
        String[] parts = msgLine.split("\t");
        int index = this.taskMapping.get(clientId)!=null?this.taskMapping.get(clientId).size():0;
        HashMap<String, MessageAttributeValue> attributes = new HashMap<>();
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK.name());
        attrebutes.put(Constants.MAC_FIELD, clientId);
        attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
        attrebutes.put(Constants.TASK_ID_FIELD, Integer.toString(index));
        qManager.sendMessage(createAttrs(attrebutes), queueUrl, msgLine);
        this.taskMapping.computeIfAbsent(clientId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new EcTask(clientId, null, null,index));
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