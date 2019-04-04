import com.amazonaws.services.sqs.model.Message;

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
    public static String mainQueue;
    public static Boolean isTerminated = false;
    public static HashMap<String,ec2Status> ec2StatusMapping = new HashMap<>();

    public static void main(String [] args){
        mainQueue = getQueue("mainQueue", Constants.MAINQUEUE);
        String workQueue = getQueue("workQueue", Constants.WORKQUEUE);
        String resultQueue = getQueue("resultQueue", Constants.WORKQUEUE);
        queueM = new QueueManager();
        EcManager ecman = new EcManager();
        ec2Count = ecman.getActiveEc2s().size();
        feeder = new EcFeeder(commandsQueue, queueM, s3client, new EcManager(), workQueue);
        Thread t1 = new Thread(feeder);
        t1.start();
        liteningloop(main::handleMessage,mainQueue);
        //ExecutorService executor = Executors.newFixedThreadPool(2);
//        try {
//            String base64UserData = new String( Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        EcManager man = new EcManager();
//        List<Instance> instances =  man.createEc2(2,Constants.JAVA8IMG,null);
//        if(instances!=null) {
//            List<InstanceStateChange> deadInstances = man.terminateEc2(instances.stream().map(Instance::getInstanceId).collect(Collectors.toList()));
//        }

    }

    public static void handleMessage(Message msg){
        System.out.println("*******main: "+msg.getBody());
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

    public static void liteningloop(Consumer<Message> cons, String queueUrl){
        List<Message> msgs = new LinkedList<>();
        while(true){
            try{
            msgs = queueM.getMessage(null,queueUrl,false);
            if(msgs.size()>0) {
                synchronized (commandsQueue) {
                    msgs.forEach(main::handleMessage);
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