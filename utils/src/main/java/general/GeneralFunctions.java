package general;

import aws.QueueManager;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;

public class GeneralFunctions {

    public static void listeningloop(Consumer<Message> cons, String queueUrl, MutableBoolean stopIndicator, QueueManager queueM, @Nullable Runnable beforeSleep){
        List<Message> msgs = new LinkedList<>();
        while(!stopIndicator.booleanValue()){
            try{
                msgs = queueM.getMessage(null,queueUrl,false);
                if(msgs.size()>0) {
                    msgs.forEach(cons);
                }
                else{
                    try {
                        if(beforeSleep!=null){
                            beforeSleep.run();
                        }
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

    public static boolean validateMsg(Message msg){
        if(msg.getMessageAttributes()==null)
            return false;
        return Constants.REQUIRED.stream().allMatch(s -> msg.getMessageAttributes().get(s) != null);
    }

    public static Map<String, MessageAttributeValue> createAttrs(HashMap<String,String> attrebutes){
        HashMap<String, MessageAttributeValue> attributes = new HashMap<>();
        for(String key: attrebutes.keySet()){
            MessageAttributeValue attr = new MessageAttributeValue();
            attr.withDataType("String");
            attr.setStringValue(attrebutes.get(key));
            attributes.put(key,attr);
        }
        return attributes;
    }
}
