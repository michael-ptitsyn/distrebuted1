import aws.EcManager;
import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import objects.EcTask;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

public class ManagerMain {
    public static QueueManager queueM;
    final Semaphore ec2CountLock = new Semaphore(1, true);
    public static int ec2Count = 0;
    public static S3Client s3client = new S3Client();
    public static final Queue<Message> commandsQueue = new ConcurrentLinkedQueue<>();
    public static EcFeeder feeder;
    public static EcRefresher refresher;
    public static EcListener listener;
    public static String mainQueue;
    public static MutableBoolean isTerminated = new MutableBoolean();
    public static HashMap<String, Constants.ec2Status> ec2StatusMapping = new HashMap<>();
    public static ConcurrentHashMap<String, List<EcTask>> taskMapping = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> queueMapping = new ConcurrentHashMap<>();
    public static EcManager ecman = new EcManager();
    public static String workQueue;
    public static String resultQueue;

    public static void main(String[] args) {
        try {
            try {
                File yourFile = new File("output");
                yourFile.createNewFile(); // if file already exists will do nothing
                PrintStream o = new PrintStream(yourFile);
               // System.setOut(o);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            queueM = new QueueManager();
            mainQueue = queueM.getOrCreateMainQueue();//getQueue("mainQueue", Constants.MAINQUEUE);
            workQueue = getQueue("workQueue", Constants.WORKQUEUE);
            resultQueue = getQueue("resultQueue", Constants.RESULT_QUEUE);
            isTerminated.setFalse();
            List<Instance> ecs = ecman.getActiveEc2s();
            ecs.forEach(s -> ec2StatusMapping.put(s.getInstanceId(), Constants.ec2Status.IDLE));
            ec2Count = ecs.size();
            listener = new EcListener(resultQueue, queueM, taskMapping);
            feeder = new EcFeeder(commandsQueue, queueM, s3client, ecman, workQueue, taskMapping);
            refresher = new EcRefresher(ecman,ec2StatusMapping,taskMapping,commandsQueue,queueM,workQueue);
            System.out.println("start feeder");
            Thread t1 = new Thread(feeder);
            t1.start();
            Thread listen = new Thread(listener);
            listen.start();
            Thread refresherTread = new Thread(refresher);
            refresherTread.start();
            listeningloop(ManagerMain::handleMessage, mainQueue, isTerminated, queueM, null);
            terminatingLoop();
            System.out.println("wait to t1");
            t1.join();
            System.out.println("wait to listener");
            listen.join();
            refresherTread.join();
            System.out.println("exiting");
        } catch (InterruptedException e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
        } finally {
            File file = new File("output");
            s3client.uploadFile(Constants.BUCKET_NAME,"manager_log", file);
            ec2StatusMapping.clear();
            List<Instance> ecs = ecman.getNotStoped();
            List<Instance> notManagers = ecs.stream().filter(c->!EcManager.isManager(c)).collect(Collectors.toList());
            ecman.terminateEc2(notManagers.stream().map(Instance::getInstanceId).collect(Collectors.toList()));
            System.out.println("killed workers!!!");
            System.out.println("killing queues");
            queueM.deleteQueue(workQueue);
            queueM.deleteQueue(resultQueue);
            ecman.terminateAll();
        }
    }

    private static void terminatingLoop() {
        System.out.println("feeder.setKill()");
        feeder.setKill();
        while (taskMapping.keySet().stream().anyMatch(k -> taskMapping.get(k).stream().anyMatch(EcTask::notDone))
                && ec2StatusMapping.values().stream().allMatch(s -> s == Constants.ec2Status.IDLE)) {
            try {
                sleep(Constants.THREAD_SLEEP);
            } catch (InterruptedException e) {
                System.out.println(ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
            }
        }
        //ecman.terminateAll();
        taskMapping.keySet()
                .forEach(k -> System.out.println("\n+" + k + ":" + taskMapping.get(k)
                        .stream()
                        .map(EcTask::getBody)
                        .collect(Collectors.toList())));
        listener.setKill();
        System.out.println("refresher.setKill()");
        refresher.setKill();
    }

    public static void handleExcpetion(QueueManager qManager, String queueUrl, Exception ex, String rId, String iId) {
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.ERROR.name());
        attrebutes.put(Constants.REQUEST_ID_FIELD, rId);
        attrebutes.put(Constants.ID_FIELD, iId);
        qManager.sendMessage(createAttrs(attrebutes), queueUrl, ex.getMessage());
    }

    private static void handleMessage(Message msg) {
        System.out.println("*******ManagerMain: " + msg.getBody());
        String requestId;
        try {
            Constants.MESSAGE_TYPE type = Constants.MESSAGE_TYPE.valueOf(msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue());
            requestId = msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue();
            if (type == Constants.MESSAGE_TYPE.TERMINATION) {
                isTerminated.setTrue();
                System.out.println("isTerminated.setTrue()");
                synchronized (commandsQueue) {
                    feeder.setKill();
                    commandsQueue.notifyAll();
                }
                System.out.println("isTerminated end of if");
            } else if (type == Constants.MESSAGE_TYPE.INIT) {
                String resultQueue = msg.getBody();
                sendHandShake(requestId, resultQueue);
                queueMapping.put(requestId, resultQueue);
            } else if (type == Constants.MESSAGE_TYPE.TASK) {
                synchronized (commandsQueue) {
                    System.out.println("task put to queue");
                    commandsQueue.add(msg);
                    commandsQueue.notifyAll();
                }
            }
        } catch (Exception ex) {
            System.out.println(ExceptionUtils.getStackTrace(ex));
            String clientId = msg.getMessageAttributes().get(Constants.ID_FIELD).getStringValue();
            requestId = msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue();
            handleExcpetion(queueM, queueMapping.get(requestId), ex, requestId, clientId);
        } finally {
            queueM.deleteMsg(mainQueue, msg);
        }
    }

    private static void sendHandShake(String clientId, String queueUrl) {
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.INIT.name());
        attrebutes.put(Constants.REQUEST_ID_FIELD, clientId);
        attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
        queueM.sendMessage(createAttrs(attrebutes), queueUrl, "OK");
    }

    public static String returnResultSequence(List<EcTask> results, String clientId) {
        try {
            String responseString = "";
            responseString = results.stream()
                    .map(task -> String.format("%s\t%s\t%s\n", task.getOperation(), task.getBody(), task.getResult_url()))
                    .reduce((s, c) -> s + c).get();
            File newHtmlFile = new File(clientId);
            FileUtils.writeStringToFile(newHtmlFile, responseString, "utf-8");
            String key = Constants.PUBLIC_FOLDER + clientId + "res";
            s3client.uploadFile(Constants.BUCKET_NAME, key, newHtmlFile);
            newHtmlFile.deleteOnExit();
            HashMap<String, String> attrebutes = new HashMap<>();
            attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK_RESULT.name());
            attrebutes.put(Constants.REQUEST_ID_FIELD, Constants.REQUEST_ID);
            attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
            queueM.sendMessage(createAttrs(attrebutes), queueMapping.get(clientId), key);
            return key;
        } catch (Exception ex) {
            System.out.println(ExceptionUtils.getStackTrace(ex));
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