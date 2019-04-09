import aws.EcManager;
import aws.QueueManager;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.*;
import java.util.stream.Collectors;

import static general.GeneralFunctions.createAttrs;
import static general.GeneralFunctions.listeningloop;

public class Main {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static QueueManager queueM = new QueueManager();
    public static String mainQueue;
    public static boolean toTerminate;
    public static MutableBoolean stop;
    public static EcManager ecman = new EcManager();

    public static void main(String[] args) {
        mainQueue = queueM.getOrCreate("mainQueue", Constants.MAINQUEUE);
        stop = new MutableBoolean(false);
        if (args.length > 1 && args[1].equals("n")) {
            toTerminate = true;
        }
        List<Instance> ecs = ecman.getActiveEc2s();
        List<Instance> managers = ecs.stream().filter(Main::isManager).collect(Collectors.toList());
        if (managers.isEmpty()) {
            List<Instance> newManns = ecman.createEc2(1, Constants.UBUNTU16_BLANK, Constants.MANAGER_USER_SCRIPT + " " + mainQueue);
            if (newManns == null) {
                throw new RuntimeException("PROBLEM CREATING MANAGER");
            }
            System.out.println(ANSI_GREEN + "CREATED NEW MANAGER" + ANSI_GREEN);
        } else if (managers.size() == 1) {
            System.out.println(ANSI_GREEN + "found manager" + ANSI_GREEN);
        } else {
            throw new RuntimeException("INTERNAL PROBLEM MORE THEN ONE MANAGER");
        }
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK.name());
        attrebutes.put(Constants.REQUEST_ID_FIELD, Constants.REQUEST_ID);
        attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
        queueM.sendMessage(createAttrs(attrebutes), mainQueue, args[0]);
        listeningloop(Main::handleMessage, mainQueue, stop, queueM, () -> System.out.println(ANSI_GREEN + "WAIT TO RESPONSE" + ANSI_GREEN));
        if (toTerminate) {
            attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TERMINATION.name());
            queueM.sendMessage(createAttrs(attrebutes), mainQueue, "blank on purpose");
        }
    }

    private static void handleMessage(Message msg) {
        System.out.println("*******CLIENT APLICATION: " + msg.getBody());
        try {
            if (msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.TASK_RESULT.name())) {
                stop.setTrue();
            }
            if (msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.ERROR.name())) {
                System.out.println("ANSI_RED*******CLIENT APLICATION ERROR:ANSI_RED" + msg.getBody());
                stop.setTrue();
            }
        } catch (
                Exception ex)

        {
            ex.printStackTrace();
        } finally

        {
            queueM.deleteMsg(mainQueue, msg);
        }

    }

    private static boolean isManager(Instance ec) {
        return ec.getTags().stream().anyMatch(t -> t.getKey().equals(Constants.MANAGER_TAG) && t.getValue().equals("true"));
    }
}