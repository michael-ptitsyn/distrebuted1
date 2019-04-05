import aws.EcManager;
import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import objects.EcTask;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static general.GeneralFunctions.listeningloop;
import static java.lang.Thread.sleep;

public class main {
    public static QueueManager queueM = new QueueManager();
    public static Queue<Message> retryQueue;
    final Semaphore ec2CountLock = new Semaphore(1, true);
    public static int ec2Count=0;
    public static S3Client s3client = new S3Client();
    public static final Queue<Message> commandsQueue = new ConcurrentLinkedQueue<>();
    public static EcFeeder feeder;
    public static EcListener listener;
    public static String mainQueue;
    public static MutableBoolean isTerminated = new MutableBoolean();
    public static HashMap<String, Constants.ec2Status> ec2StatusMapping = new HashMap<>();
    public static ConcurrentHashMap<String, List<EcTask>> taskMapping = new ConcurrentHashMap<>();

    public static void main(String [] args){
        mainQueue = getQueue("mainQueue", Constants.MAINQUEUE);
        String workQueue = getQueue("workQueue", Constants.WORKQUEUE);
        String resultQueue = getQueue("resultQueue", Constants.RESULT_QUEUE);
        queueM = new QueueManager();
        isTerminated.setFalse();
        EcManager ecman = new EcManager();
        List<Instance> ecs = ecman.getActiveEc2s();
        ecs.forEach(s->ec2StatusMapping.put(s.getInstanceId(), Constants.ec2Status.IDLE));
        ec2Count = ecs.size();
        listener = new EcListener(resultQueue, queueM, taskMapping);
        feeder = new EcFeeder(commandsQueue, queueM, s3client, new EcManager(), workQueue, taskMapping);
        Thread t1 = new Thread(feeder);
        t1.start();
        Thread listen = new Thread(listener);
        listen.start();
        listeningloop(main::handleMessage,mainQueue, isTerminated, queueM,null);
        terminatingLoop();
        try {
            t1.join();
            listen.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void terminatingLoop(){
        feeder.setKill();
        while(taskMapping.keySet().stream().anyMatch(k-> taskMapping.get(k).stream().anyMatch(EcTask::notDone))
                && ec2StatusMapping.values().stream().allMatch(s->s==Constants.ec2Status.IDLE)){
            try {
                sleep(Constants.THREAD_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        taskMapping.keySet()
                .forEach(k-> System.out.println("\n+"+k+":"+taskMapping.get(k)
                        .stream()
                        .map(EcTask::getBody)
                        .collect(Collectors.toList())));
        listener.setKill();
    }

    private static void handleMessage(Message msg){
        System.out.println("*******main: "+msg.getBody());
        if(msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.TERMINATION.name())){
            isTerminated.setTrue();
            synchronized (commandsQueue) {
                feeder.setKill();
                commandsQueue.notifyAll();
            }
        }
        else if(msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.TASK.name())){
            synchronized (commandsQueue) {
                commandsQueue.add(msg);
                commandsQueue.notifyAll();
            }
        }
        queueM.deleteMsg(mainQueue, msg);
    }

    public static String getQueue(String name, String url){
//        int index;
//        if((index = queueM.getqUrls().indexOf(url))>-1){
//            return queueM.getqUrls().get(index);
//        }
        return queueM.getOrCreate(name,url);
    }
}