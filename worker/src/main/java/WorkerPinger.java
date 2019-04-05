import aws.QueueManager;
import general.Constants;
import objects.EcRunnble;

import java.util.HashMap;

import static general.GeneralFunctions.createAttrs;
import static java.lang.Thread.sleep;

public class WorkerPinger extends EcRunnble implements Runnable {
    private QueueManager qManager;
    private String resultQueue;
    public WorkerPinger(QueueManager qManager, String resultQueue) {
        this.qManager = qManager;
        this.resultQueue = resultQueue;
    }

    @Override
    public void run() {
        synchronized (kill) {
            while (!kill.booleanValue()) {
                HashMap<String, String> attrebutes = new HashMap<>();
                attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.STATUS_UPDATE.name());
                attrebutes.put(Constants.STATUS_FIELD, Constants.ec2Status.IDLE.name());
                attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
                attrebutes.put(Constants.MAC_FIELD, "NO MAC");
                attrebutes.put(Constants.TASK_ID_FIELD, "2");
                qManager.sendMessage(createAttrs(attrebutes), resultQueue, "status");
                System.out.println("sleeping...");
                try {
                    sleep(Constants.KEEP_ALIVE_INTERVAL*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }
}