import aws.QueueManager;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import general.Constants;
import objects.EcRunnble;
import objects.EcTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * created to handle exceptions
 * will listen the results queue and created the html
 * **/
public class EcListener extends EcRunnble implements Runnable {
    private String queueUrl;
    private String result;
    private int doneCounter;
    private QueueManager queueM;
    private ConcurrentHashMap<String, List<EcTask>> tasksMap;
    public EcListener(String queueUrl, QueueManager qManager, ConcurrentHashMap<String, List<EcTask>> globalTable) {
        super();
        queueM = qManager;
        tasksMap = globalTable;
        this.queueUrl = queueUrl;
    }

    //TODO add semaphore for more than one worker
    //TODO we dont know when to stop
    @Override
    public void run() {
            main.listeningloop(this::handleMsg, queueUrl, kill, queueM, null);
    }

    private void handleMsg(Message msg){
        try {
            Map<String, MessageAttributeValue> msgMapping = msg.getMessageAttributes();
            String msgType = msgMapping.get(Constants.TYPE_FIELD).getStringValue();
            if (msgType.equals(Constants.MESSAGE_TYPE.STATUS_UPDATE.name())) {
                String status = msgMapping.get(Constants.STATUS_FIELD).getStringValue();
                main.ec2StatusMapping.put(msgMapping.get(Constants.ID_FIELD).getStringValue(), Constants.ec2Status.valueOf(status));
            } else if (msgType.equals(Constants.MESSAGE_TYPE.TASK_RESULT.name())) {
                result = result != null ? result + "\n" + msg.getBody() : msg.getBody();
                int index = Integer.valueOf(msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                EcTask tempTask =  tasksMap.get(msg.getMessageAttributes().get(Constants.MAC_FIELD).getStringValue())
                        .get(index);
                tempTask.setResult_url(msg.getBody());
                tempTask.setDone(true);
            }
        }
        catch (Exception ex){
            throw ex;
        }
        finally {
            queueM.deleteMsg(queueUrl, msg);
        }
    }
}