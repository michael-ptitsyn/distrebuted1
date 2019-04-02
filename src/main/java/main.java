import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

public class main {
    public static void main(String [] args){
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
        QueueManager queueM = new QueueManager();
        queueM.getOrCreate("michaelDrorQ1",null);
        System.out.println(queueM.getQueues());
        queueM.deleteQueue(queueM.getQueues().get(0), "queue removed");
    }
}