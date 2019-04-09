import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import static general.GeneralFunctions.createAttrs;
import static general.GeneralFunctions.listeningloop;
import static general.GeneralFunctions.validateMsg;

public class Main {
    public static Constants.ec2Status status;
    private static QueueManager queueM = new QueueManager();
    private static String resultQueue = queueM.getOrCreate("resultQueue", Constants.RESULT_QUEUE);;
    private static String workQueue = queueM.getOrCreate("workQueue",Constants.WORKQUEUE);
    private static S3Client s3 = new S3Client();
    public static void main(String [] args){
        System.out.println("DEBUG: MY INSTANCE ID:"+Constants.instanceId+"\n");
        final MutableBoolean isTerminated = new MutableBoolean();
        status = Constants.ec2Status.IDLE;
        WorkerPinger wPinger = new WorkerPinger(queueM, resultQueue);
        Thread pinger = new Thread(wPinger);
        pinger.start();
        listeningloop(Main::handleMessage, workQueue, isTerminated, queueM, ()->status = Constants.ec2Status.IDLE);
    }

    private static void handleMessage(Message msg){
        HashMap<String, String> attrebutes = new HashMap<>();
        try {
            if(validateMsg(msg)) {
                System.out.println("*******worker: " + msg.getBody());
                String url = fake(msg.getBody());
                attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK_RESULT.name());
                attrebutes.put(Constants.REQUEST_ID_FIELD, msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue());
                attrebutes.put(Constants.TASK_ID_FIELD, msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
                queueM.sendMessage(createAttrs(attrebutes), resultQueue, url);
                status = Constants.ec2Status.WORKING;
            }
            else{
                queueM.sendMessage(createAttrs(attrebutes), resultQueue, "BAD MSG FORMAT");
            }
        }
        catch(Exception ex){
            status = Constants.ec2Status.IDLE;
            attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.ERROR.name());
            attrebutes.put(Constants.REQUEST_ID_FIELD, msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue());
            queueM.sendMessage(createAttrs(attrebutes), resultQueue, "BAD MSG FORMAT:\n\n"+ExceptionUtils.getStackTrace(ex));
        }
        finally {
            queueM.deleteMsg(workQueue,msg);
        }
    }



    public static String fake(String toWrite) throws IOException {
        try {
            // Create temp file.
            File temp = File.createTempFile(toWrite.substring(8), ".txt");
            // Delete temp file when program exits.
            temp.deleteOnExit();
            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(toWrite);
            out.close();
            String fileName = toWrite.substring(8);
            s3.uploadFile(Constants.BUCKET_NAME,toWrite.substring(8), temp);
            return s3.getUrl(Constants.BUCKET_NAME, fileName).toString();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}