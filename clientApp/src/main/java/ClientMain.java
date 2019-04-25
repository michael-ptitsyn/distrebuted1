import aws.EcManager;
import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static general.GeneralFunctions.createAttrs;
import static general.GeneralFunctions.listeningloop;

public class ClientMain {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static QueueManager queueM = new QueueManager();
    public static String mainQueue;
    public static String resultQueue;
    public static boolean toTerminate;
    public static String outputName;
    public static Integer ratio;
    public static EcManager ecman = new EcManager();
    public static S3Client s3client = new S3Client();
    public static MutableBoolean stop = new MutableBoolean(false);

    public static void main(String[] args) {
        List<Instance> ecs = ecman.getNotStoped();
        List<Instance> managers = ecs.stream().filter(EcManager::isManager).collect(Collectors.toList());
        if (managers.isEmpty()) {
            List<Instance> newManns = ecman.createEc2(1, Constants.JAVA8IMG, Constants.MANAGER_USER_SCRIPT_SHORT);
            if (newManns == null) {
                throw new RuntimeException("PROBLEM CREATING MANAGER");
            }
            HashMap<String, String> tagsMap = new HashMap<>();
            tagsMap.put(Constants.MANAGER_TAG, "true");
            ecman.createTags(newManns.stream().map(Instance::getInstanceId).collect(Collectors.toList()), tagsMap);
            System.out.println(ANSI_GREEN + "CREATED NEW MANAGER" + ANSI_GREEN);
        } else if (managers.size() == 1) {
            System.out.println(ANSI_GREEN + "found manager" + ANSI_GREEN);
        } else {
            throw new RuntimeException("INTERNAL PROBLEM MORE THEN ONE MANAGER");
        }
        File inputFile = new File(args[0]);
        System.out.println(ANSI_GREEN + "uploading file to s3" + ANSI_GREEN);
        s3client.uploadFile(Constants.BUCKET_NAME, Constants.PUBLIC_FOLDER + args[0], inputFile);
        String inputUrl = s3client.getUrl(Constants.BUCKET_NAME, Constants.PUBLIC_FOLDER + args[0]).toString();
        outputName = args[1];
        System.out.println(ANSI_GREEN + "the url is:" + inputUrl + ANSI_GREEN);
        try {
            mainQueue = queueM.getMainQueueUrl();
        } catch (TimeoutException e) {
            throw new RuntimeException("CANT GET MAIN QUEUE!!!!!!!!");
        }
        if (args.length > 3 && args[3].equals("terminate")) {
            toTerminate = true;
        }
        initSequence();
        listeningloop(ClientMain::handleHandShake, resultQueue, stop, queueM, () -> System.out.println(ANSI_GREEN + "WAIT TO HAND-SHAKE RESPONSE" + ANSI_GREEN));
        stop.setFalse();
        sendTask(inputUrl + "\t" + args[2]);
        listeningloop(ClientMain::handleMessage, resultQueue, stop, queueM, () -> System.out.println(ANSI_GREEN + "WAIT TO RESPONSE" + ANSI_GREEN));
        if (toTerminate) {
            sendMessage(Constants.MESSAGE_TYPE.TERMINATION, Constants.REQUEST_ID, Constants.instanceId, "null", mainQueue);
        }
        queueM.deleteQueue(resultQueue);
    }

    private static void initSequence() {
        resultQueue = queueM.getOrCreate(Constants.REQUEST_ID, null);
        sendMessage(Constants.MESSAGE_TYPE.INIT, Constants.REQUEST_ID, Constants.instanceId, resultQueue, mainQueue);
        System.out.println(ANSI_GREEN + "created and sent result queue! " + resultQueue + ANSI_GREEN);
    }

    private static void sendTask(String task) {
        sendMessage(Constants.MESSAGE_TYPE.TASK, Constants.REQUEST_ID, Constants.instanceId, task, mainQueue);
        System.out.println(ANSI_GREEN + "sent task to queue:" + mainQueue + ANSI_GREEN);
    }

    private static void sendMessage(Constants.MESSAGE_TYPE type, String requestId, String instanceId, String body, String queueUrl) {
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, type.name());
        attrebutes.put(Constants.REQUEST_ID_FIELD, requestId);
        attrebutes.put(Constants.ID_FIELD, instanceId);
        queueM.sendMessage(createAttrs(attrebutes), queueUrl, body);
    }

    private static void handleHandShake(Message msg) {
        System.out.println("*******HANDSHAKE: " + msg.getBody());
        try {
            if (msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.INIT.name())) {
                stop.setTrue();
            } else {
                System.out.println(ANSI_RED + "*******BAD HAND-SHAKE ERROR:" + ANSI_RED + msg.getBody());
                stop.setTrue();
            }
        } catch (
                Exception ex) {
            ex.printStackTrace();
        } finally

        {
            queueM.deleteMsg(resultQueue, msg);
        }
    }

    public static void handleMessage(Message msg) {
        System.out.println("*******CLIENT APLICATION: " + msg.getBody());
        try {
            if (msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.TASK_RESULT.name())) {
                Iterable<String> lines = () -> s3client.downloadItemAsLines(Constants.BUCKET_NAME, msg.getBody());
                String htmlString = Constants.htmlTemplate;
                String title = "RESULTS";
                htmlString = htmlString.replace("$title", title);
                htmlString = htmlString.replace("$content", StreamSupport.stream(lines.spliterator(), false)
                        .reduce((s, c) -> s + "</br>"+ c + "</br>" ).get());
                File newHtmlFile = new File(outputName + ".html");
                FileUtils.writeStringToFile(newHtmlFile, htmlString, "utf-8");
                stop.setTrue();
            }
            if (msg.getMessageAttributes().get(Constants.TYPE_FIELD).getStringValue().equals(Constants.MESSAGE_TYPE.ERROR.name())) {
                System.out.println("ANSI_RED*******CLIENT APLICATION ERROR:ANSI_RED" + msg.getBody());
                stop.setTrue();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            queueM.deleteMsg(resultQueue, msg);
        }
    }
}