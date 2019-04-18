import objects.EcRunnble;

import java.util.Queue;

public class EcRefresher extends EcRunnble implements Runnable {
    private final Queue<String> ec2ToRefresh;

    public EcRefresher(Queue<String> ec2ToRefresh) {
        this.ec2ToRefresh = ec2ToRefresh;
    }

    @Override
    public void run() {
        synchronized (ec2ToRefresh) {
            while (!kill.booleanValue()) {
                if (ec2ToRefresh.isEmpty()){
                    try {
                        ec2ToRefresh.wait();
                    } catch (InterruptedException e) {
                        System.out.println("problem running EC-REFRESHER" + e.toString());
                        e.printStackTrace();
                    }
                }
                else{
                    ec2ToRefresh.remove();
                }
            }
        }
    }
}
