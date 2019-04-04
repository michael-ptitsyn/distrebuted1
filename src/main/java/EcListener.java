import com.amazonaws.services.sqs.model.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * created to handle exceptions
 * will listen the results queue and created the html
 * **/
public class EcListener implements Runnable {
    private String queueUrl;
    private String result;
    private int doneCounter;
    public EcListener(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    //TODO add semaphore for more than one worker
    //TODO we dont know when to stop
    @Override
    public void run() {
            main.liteningloop(this::handleMsg, queueUrl);
    }

    private void handleMsg(Message msg){
        Map<String, String> msgMapping = msg.getAttributes();
        if(msgMapping.get(Constants.TYPE_FIELD).equals(Constants.MESSAGE_TYPE.STATUS_UPDATE.name())){
            main.ec2StatusMapping.put(msgMapping.get(Constants.ID_FIELD), main.ec2Status.valueOf(msg.getBody()));
        }

        result=result!=null?result+"\n"+msg.getBody():msg.getBody();
    }
}
