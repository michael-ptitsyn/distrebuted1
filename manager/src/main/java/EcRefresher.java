import aws.EcManager;
import aws.QueueManager;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import objects.EcRunnble;
import objects.EcTask;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class EcRefresher extends EcRunnble implements Runnable {
    private final EcManager ecman;
    private final HashMap<String, Constants.ec2Status> ec2StatusMapping;
    private final ConcurrentHashMap<String, List<EcTask>> taskMapping;
    private final Queue<Message> commandsQueue;
    private final String workQueue;
    private final QueueManager qMan;

    public EcRefresher(EcManager ecman, HashMap<String, Constants.ec2Status> ec2StatusMapping, ConcurrentHashMap<String, List<EcTask>> taskMapping, Queue<Message> commandsQueue, QueueManager qMan, String workQueue) {
        this.ecman = ecman;
        this.ec2StatusMapping = ec2StatusMapping;
        this.taskMapping = taskMapping;
        this.commandsQueue = commandsQueue;
        this.qMan = qMan;
        this.workQueue = workQueue;
    }

    /**
     * 1.  ec2List all ec2s that are not terminated OR killed for example instance that rebooting and not doing any job
     * 2.  notStopedids ids of ec2List
     * 3.  idsToRun all the ids that should be in notstoped mode (working, reboot, initializing) but are not in ec2List
     *      for example if I terminate some worker he wont be in ec2List so we will add it to idsToRun
     *      in that case we will count them and create new ec2s accordingly
     * 4.  ids - all the ids that should be in running mode, if ec2 is rebooting he wont be here
     * 5.  notRunningids all the ids that should be in running mode but are not in ids list
     *     in that case we will find out what was the last task each such ec2 was working on and resend it
     * **/
    @Override
    public void run() {
        while (!kill.booleanValue()) {
            try {
                String userScript = Constants.WORKER_USER_SCRIPT_SHORT;
                userScript = userScript.replace("$1", workQueue);
                userScript = userScript.replace("$2", ManagerMain.resultQueue);
                List<Instance> ec2List = ecman.getNotStoped();
                List<String> notStopedids = ec2List.stream().map(Instance::getInstanceId).collect(Collectors.toList());
                System.out.println("notStopedids:" + notStopedids);
                List<String> idsToRun =ec2StatusMapping
                        .keySet()
                        .stream()
                        .filter(s -> ec2StatusMapping.get(s)!=Constants.ec2Status.DEAD)
                        .filter(s -> !notStopedids.contains(s))
                        .collect(Collectors.toList());
                List<String> ids = ec2List.stream()
                        .filter(s -> s.getPublicIpAddress() != null)
                        .map(Instance::getInstanceId)
                        .collect(Collectors.toList());
                System.out.println("ACTIVE IDS:" + ids);
                List<String> notRunningids = ec2StatusMapping
                        .keySet()
                        .stream()
                        .filter(s -> ec2StatusMapping.get(s)!=Constants.ec2Status.DEAD)
                        .filter(s -> !ids.contains(s))
                        .collect(Collectors.toList());
                if (!notRunningids.isEmpty()) {
                    System.out.println("THE FOLLOWING EC2s ARE NOT RUNNING:" + notRunningids);
                    List<EcTask> toresend = taskMapping.values()
                            .stream()
                            .flatMap(List::stream)
                            .filter(task -> notRunningids.contains(task.getWorker()) && task.notDone())
                            .peek(task -> task.setWorker(null))
                            .peek(task -> qMan.sendMessage(task, workQueue))
                            .collect(Collectors.toList());
                }
                if(idsToRun!=null&&idsToRun.size()>0){
                    System.out.println("THE FOLLOWING EC2s ARE DEAD:" + idsToRun);
                    List<Instance> newInsts = ecman.createEc2(idsToRun.size(), Constants.JAVA8IMG, userScript);
                    idsToRun.forEach(id->ec2StatusMapping.put(id,Constants.ec2Status.DEAD));
                    if(newInsts!=null){
                        newInsts.forEach(inst->ec2StatusMapping.put(inst.getInstanceId(),Constants.ec2Status.IDLE));
                    }
                    else{
                        System.out.println("REFRESH CANT CREATE NEW INSTANCE:");
                    }
                }
//                else if(ManagerMain.ec2Count> notStopedids.size()-1){
//                     ecman.createEc2(ManagerMain.ec2Count-notStopedids.size()+1, Constants.JAVA8IMG, userScript);
//                }
                sleep(Constants.REFRESH_PERIOD);
            } catch (InterruptedException e) {
                System.out.println("refresher:"+ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
            }
        }
        System.out.println("refresher is dead");
    }
}