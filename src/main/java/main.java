import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;

import java.util.List;
import java.util.stream.Collectors;

public class main {
    public static void main(String [] args){
        EcManager man = new EcManager();
        List<Instance> instances =  man.createEc2(2,EcManager.JAVA8IMG,null);
        if(instances!=null) {
            List<InstanceStateChange> deadInstances = man.terminateEc2(instances.stream().map(Instance::getInstanceId).collect(Collectors.toList()));
        }
        System.out.println("ready !!!!!");

    }
}
