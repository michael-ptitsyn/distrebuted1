import aws.EcManager;
import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import objects.EcTask;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static general.GeneralFunctions.createAttrs;
import static general.GeneralFunctions.listeningloop;
import static java.lang.Thread.sleep;

public class main {
    public static QueueManager queueM = new QueueManager();
    final Semaphore ec2CountLock = new Semaphore(1, true);
    public static int ec2Count = 0;
    public static S3Client s3client = new S3Client();
    public static final Queue<Message> commandsQueue = new ConcurrentLinkedQueue<>();
    public static EcFeeder feeder;
    public static EcListener listener;
    public static String mainQueue;
    public static MutableBoolean isTerminated = new MutableBoolean();
    public static HashMap<String, Constants.ec2Status> ec2StatusMapping = new HashMap<>();
    public static ConcurrentHashMap<String, List<EcTask>> taskMapping = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> queueMapping = new ConcurrentHashMap<>();
    public static EcManager ecman = new EcManager();

    public static void main(String[] args) {
        mainQueue = getQueue("mainQueue", Constants.MAINQUEUE);
        String workQueue = getQueue("workQueue", Constants.WORKQUEUE);
        String resultQueue = getQueue("resultQueue", Constants.RESULT_QUEUE);
        queueM = new QueueManager();
        isTerminated.setFalse();
        List<Instance> ecs = ecman.getActiveEc2s();
        ecs.forEach(s -> ec2StatusMapping.put(s.getInstanceId(), Constants.ec2Status.IDLE));
        ec2Count = ecs.size();
        listener = new EcListener(resultQueue, queueM, taskMapping);
        feeder = new EcFeeder(commandsQueue, queueM, s3client, ecman, workQueue, taskMapping);
        Thread t1 = new Thread(feeder);
        t1.start();
        Thread listen = new Thread(listener);
        listen.start();
        listeningloop(main::handleMessage, mainQueue, isTerminated, queueM, null);
        terminatingLoop();
        try {
            t1.join();
            listen.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void terminatingLoop() {
        String result = "";
        feeder.setKill();
        while (taskMapping.keySet().stream().anyMatch(k -> taskMapping.get(k).stream().anyMatch(EcTask::notDone))
                && ec2StatusMapping.values().stream().allMatch(s -> s == Constants.ec2Status.IDLE)) {
            try {
                sleep(Constants.THREAD_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ecman.terminateAll();
        taskMapping.keySet().stream().map(s -> returnResultSequence(taskMapping.get(s), s)).collect(Collectors.toList());
        taskMapping.keySet()
                .forEach(k -> System.out.println("\n+" + k + ":" + taskMapping.get(k)
                        .stream()
                        .map(EcTask::getBody)
                        .collect(Collectors.toList())));
        listener.setKill();
    }

    private static void handleMessage(Message msg) {
        System.out.println("*******main: " + msg.getBody());
        try {
            Constants.MESSAGE_TYPE type = Constants.MESSAGE_TYPE.valueOf(msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue());
            String requestId = msg.getMessageAttributes().get(Constants.REQUEST_ID).getStringValue();
            if (type == Constants.MESSAGE_TYPE.TERMINATION) {
                isTerminated.setTrue();
                synchronized (commandsQueue) {
                    feeder.setKill();
                    commandsQueue.notifyAll();
                }
            } else if (type == Constants.MESSAGE_TYPE.INIT) {
                String newQueue = queueM.getOrCreate(requestId, null);
                sendQueueUrl(requestId, newQueue, mainQueue);
                queueMapping.put(requestId, newQueue);
            } else if (type == Constants.MESSAGE_TYPE.TASK) {
                synchronized (commandsQueue) {
                    commandsQueue.add(msg);
                    commandsQueue.notifyAll();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            queueM.deleteMsg(mainQueue, msg);
        }
    }

    private static void sendQueueUrl(String clientId, String newQueueUrl, String queueUrl) {
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.INIT.name());
        attrebutes.put(Constants.REQUEST_ID_FIELD, clientId);
        attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
        queueM.sendMessage(createAttrs(attrebutes), queueUrl, newQueueUrl);
    }

    public static String returnResultSequence(List<EcTask> results, String clientId) {
        try {
            File htmlTemplateFile = new File("manager/src/main/resources/tamplate.html");
            String htmlString = FileUtils.readFileToString(htmlTemplateFile, "utf-8");
            String title = "New Page";
            htmlString = htmlString.replace("$title", title);
            htmlString = htmlString.replace("$content", results.stream().map(EcTask::getResult_url)
                    .reduce((s, c) -> s + c + "</br>").get());
            File newHtmlFile = new File(clientId + ".html");
            FileUtils.writeStringToFile(newHtmlFile, htmlString, "utf-8");
            s3client.uploadFile(Constants.BUCKET_NAME, clientId, newHtmlFile);
            newHtmlFile.deleteOnExit();
            String result = s3client.getUrl(Constants.BUCKET_NAME, clientId).toString();
            HashMap<String, String> attrebutes = new HashMap<>();
            attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK_RESULT.name());
            attrebutes.put(Constants.REQUEST_ID_FIELD, Constants.REQUEST_ID);
            attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
            queueM.sendMessage(createAttrs(attrebutes), queueMapping.get(clientId), result);
            return result;
        } catch (Exception ex) {
            return "unable to created input file";
        }
    }

    public static String getQueue(String name, String url) {
//        int index;
//        if((index = queueM.getqUrls().indexOf(url))>-1){
//            return queueM.getqUrls().get(index);
//        }
        return queueM.getOrCreate(name, url);
    }
}