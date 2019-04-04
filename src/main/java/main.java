import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;

public class main {
    enum ec2Status{
        WORKING,
        IDLE
    }
    public static QueueManager queueM = new QueueManager();
    public static Queue<Message> retryQueue;
    final Semaphore ec2CountLock = new Semaphore(1, true);
    public static int ec2Count=0;
    public static S3Client s3client = new S3Client();
    public static Queue<Message> commandsQueue = new ConcurrentLinkedQueue<>();
    public static EcFeeder feeder;
    public static EcListener listener;
    public static String mainQueue;
    public static MutableBoolean isTerminated = new MutableBoolean();
    public static HashMap<String,ec2Status> ec2StatusMapping = new HashMap<>();

    public static void main(String [] args){
        mainQueue = getQueue("mainQueue", Constants.MAINQUEUE);
        String workQueue = getQueue("workQueue", Constants.WORKQUEUE);
        String resultQueue = getQueue("resultQueue", Constants.RESULT_QUEUE);
        queueM = new QueueManager();
        isTerminated.setFalse();
        EcManager ecman = new EcManager();
        List<Instance> ecs = ecman.getActiveEc2s();
        ecs.forEach(s->ec2StatusMapping.put(s.getInstanceId(),ec2Status.IDLE));
        ec2Count = ecs.size();
        listener = new EcListener(resultQueue, queueM);
        feeder = new EcFeeder(commandsQueue, queueM, s3client, new EcManager(), workQueue);
        Thread t1 = new Thread(feeder);
        t1.start();
        Thread listen = new Thread(listener);
        listen.start();
        listeningloop(main::handleMessage,mainQueue, isTerminated);
        //ExecutorService executor = Executors.newFixedThreadPool(2);

//        EcManager man = new EcManager();
//        List<Instance> instances =  man.createEc2(2,Constants.JAVA8IMG,null);
//        if(instances!=null) {
//            List<InstanceStateChange> deadInstances = man.terminateEc2(instances.stream().map(Instance::getInstanceId).collect(Collectors.toList()));
//        }

    }

    private static void handleMessage(Message msg){
        System.out.println("*******main: "+msg.getBody());
        if(msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.TERMINATION.name())){
            isTerminated.setTrue();
        }
        commandsQueue.add(msg);
        queueM.deleteMsg(mainQueue, msg);
    }

    public static String getQueue(String name, String url){
//        int index;
//        if((index = queueM.getqUrls().indexOf(url))>-1){
//            return queueM.getqUrls().get(index);
//        }
        return queueM.getOrCreate(name,url);
    }

    public static void listeningloop(Consumer<Message> cons, String queueUrl, MutableBoolean stopIndicator){
        List<Message> msgs = new LinkedList<>();
        while(!stopIndicator.booleanValue()){
            try{
            msgs = queueM.getMessage(null,queueUrl,false);
            if(msgs.size()>0) {
                synchronized (commandsQueue) {
                    msgs.forEach(cons);
                    commandsQueue.notify();
                }
            }
            else{
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        }
    }
}