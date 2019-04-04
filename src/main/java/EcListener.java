import com.amazonaws.services.sqs.model.Message;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Map;

/**
 * created to handle exceptions
 * will listen the results queue and created the html
 * **/
public class EcListener implements Runnable {
    private String queueUrl;
    private String result;
    private int doneCounter;
    private QueueManager queueM;
    private MutableBoolean kill;
    public EcListener(String queueUrl, QueueManager qManager) {
        queueM = qManager;
        kill = new MutableBoolean();
        kill.setFalse();
        this.queueUrl = queueUrl;
    }

    //TODO add semaphore for more than one worker
    //TODO we dont know when to stop
    @Override
    public void run() {
            main.listeningloop(this::handleMsg, queueUrl, kill);
    }

    private void handleMsg(Message msg){
        try {
            Map<String, String> msgMapping = msg.getAttributes();
            String msgType = msgMapping.get(Constants.TYPE_FIELD);
            if (msgType.equals(Constants.MESSAGE_TYPE.STATUS_UPDATE.name())) {
                main.ec2StatusMapping.put(msgMapping.get(Constants.ID_FIELD), main.ec2Status.valueOf(msg.getBody()));
            } else if (msgType.equals(Constants.MESSAGE_TYPE.TASK_RESULT.name())) {
                result = result != null ? result + "\n" + msg.getBody() : msg.getBody();
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