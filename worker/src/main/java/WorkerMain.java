import aws.QueueManager;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.HashMap;

import static general.GeneralFunctions.createAttrs;
import static general.GeneralFunctions.listeningloop;
import static general.GeneralFunctions.validateMsg;

public class WorkerMain {
    public static Constants.ec2Status status;
    private static QueueManager queueM = new QueueManager();
    private static String resultQueue = queueM.getOrCreate("resultQueue", Constants.RESULT_QUEUE);;
    private static String workQueue = queueM.getOrCreate("workQueue",Constants.WORKQUEUE);
    public static void main(String [] args){
        System.out.println("DEBUG: MY INSTANCE ID:"+Constants.instanceId+"\n");
        final MutableBoolean isTerminated = new MutableBoolean();
        status = Constants.ec2Status.IDLE;
        WorkerPinger wPinger = new WorkerPinger(queueM, resultQueue);
        Thread pinger = new Thread(wPinger);
        pinger.start();
        listeningloop(WorkerMain::handleMessage, workQueue, isTerminated, queueM, ()->status = Constants.ec2Status.IDLE);
    }

    private static void handleMessage(Message msg){
        HashMap<String, String> attrebutes = new HashMap<>();
        try {
            if(validateMsg(msg)) {
                System.out.println("*******worker: " + msg.getBody());
                String resultURL = fake(msg.getBody());
                attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK_RESULT.name());
                attrebutes.put(Constants.MAC_FIELD, msg.getMessageAttributes().get(Constants.MAC_FIELD).getStringValue());
                attrebutes.put(Constants.TASK_ID_FIELD, msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
                queueM.sendMessage(createAttrs(attrebutes), resultQueue, "hello world result");
                status = Constants.ec2Status.WORKING;
            }
            else{
                queueM.sendMessage(createAttrs(attrebutes), resultQueue, "BAD MSG FORMAT");
            }
        }
        catch(Exception ex){
            status = Constants.ec2Status.IDLE;
            attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.ERROR.name());
            attrebutes.put(Constants.MAC_FIELD, msg.getMessageAttributes().get(Constants.MAC_FIELD).getStringValue());
            queueM.sendMessage(createAttrs(attrebutes), resultQueue, "BAD MSG FORMAT:\n\n"+ExceptionUtils.getStackTrace(ex));
        }
        finally {
            queueM.deleteMsg(workQueue,msg);
        }
    }



    public static String fake(String url){
        return "some URL";
    }
}
