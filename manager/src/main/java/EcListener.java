import aws.QueueManager;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import general.Constants;
import objects.EcRunnble;
import objects.EcTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static general.GeneralFunctions.listeningloop;

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
            listeningloop(this::handleMsg, queueUrl, kill, queueM, null);
    }

    private void handleMsg(Message msg){
        try {
            Map<String, MessageAttributeValue> msgMapping = msg.getMessageAttributes();
            int index ;
            String msgType = msgMapping.get(Constants.TYPE_FIELD).getStringValue();
            String requestId = msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue();
            List<EcTask> tasksById =tasksMap.get(requestId);
            if (msgType.equals(Constants.MESSAGE_TYPE.STATUS_UPDATE.name())) {
                String status = msgMapping.get(Constants.STATUS_FIELD).getStringValue();
                ManagerMain.ec2StatusMapping.put(msgMapping.get(Constants.ID_FIELD).getStringValue(), Constants.ec2Status.valueOf(status));
            }
            else{
                if (msgType.equals(Constants.MESSAGE_TYPE.TASK_RESULT.name())) {
                    result = result != null ? result + "\n" + msg.getBody() : msg.getBody();
                    index = Integer.valueOf(msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                    EcTask tempTask = tasksById.get(index);
                    tempTask.setResult_url(msg.getBody());
                    tempTask.setDone(true);
                } else if (msgType.equals(Constants.MESSAGE_TYPE.TASK_START.name())) {
                    index = Integer.valueOf(msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                    tasksById.get(index).setWorker(msg.getMessageAttributes().get(Constants.ID_FIELD).getStringValue());
                } else if (msgType.equals(Constants.MESSAGE_TYPE.ERROR.name())) {
                    index = Integer.valueOf(msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                    EcTask tempTask = tasksById.get(index);
                    tempTask.setResult_url(msg.getBody());
                    tempTask.setDone(true);
                } else {
                    System.out.println("wierd");
                }
                if (tasksById != null && tasksById.stream().noneMatch(EcTask::notDone)) {
                    ManagerMain.returnResultSequence(tasksById, requestId);
                }
            }

        } catch (Exception ex) {
            System.out.println("Error in handle MSG ECListener" + ex.toString());
            throw ex;
        } finally {
            queueM.deleteMsg(queueUrl, msg);
        }
    }
}