package general;

import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.collect.ImmutableList;

import java.util.UUID;

public class Constants {
    public static String JAVA8IMG = "ami-0b8962a37670b6d05";
    public static String UBUNTU16_WITH_CREDS = "ami-0996439223e1cfa01";
    public static String JAVA8IMG_WITH_CREDS = "ami-0b8962a37670b6d05";
    public static String UBUNTU16_BLANK = "ami-0565af6e282977273";
    public static final String DEFAULT_REGION="us-east-1";
    public static final int TIMEOUT=20;
    public static final String MAINQUEUE="https://sqs.us-east-1.amazonaws.com/993541871317/MyQueue662d7826-4f15-409b-97ef-654c2b005011";
    public static final String WORKQUEUE="https://sqs.us-east-1.amazonaws.com/993541871317/MyQueue32e6f304-f5e2-453e-adc6-c687428d438b";
    public static final String RESULT_QUEUE="https://sqs.us-east-1.amazonaws.com/993541871317/MyQueue119b949f-1461-4130-a018-7d4a0ebe6aac";
    public static final int THREAD_SLEEP=3000;
    public static final String MANAGER_TAG="manager";
    public static final String BUCKET_NAME="michael-dror-distrebuted";
    public static final int DEFAULT_MSG_COMP_RATION=50;
    public static final int KEEP_ALIVE_INTERVAL = 5;
    public static final String KEY_PAIR = "ptitsyn_bgu";
    public static final String TASK_ID_FIELD = "task_id";
    public static final String TYPE_FIELD = "type";
    public static final String STATUS_FIELD = "status";
    public static final String REQUEST_ID_FIELD = "request_id"; //the uniqe client request id (each application got unique id
    public static final String ID_FIELD = "instanceId"; //the unique id of the ec2 machine
    public static final String instanceId = EC2MetadataUtils.getInstanceId()!=null?EC2MetadataUtils.getInstanceId():UUID.randomUUID().toString();
    public static final String REQUEST_ID = UUID.randomUUID().toString();
    public enum  MESSAGE_TYPE {
        STATUS_UPDATE,
        TASK,
        TERMINATION,
        TASK_RESULT,
        INIT,
        ERROR
    }
    public enum ec2Status{
        WORKING,
        IDLE
    }
    public static final ImmutableList<String> REQUIRED = ImmutableList.of(REQUEST_ID_FIELD,ID_FIELD,TYPE_FIELD);
    public static final String WORKER_USER_SCRIPT = "#!/bin/bash\n" +
            "apt update -y\n"+
            "apt install openjdk-8-jdk -y\n" +
            "mkdir /home/ubuntu/michael\n" +
            "cd /home/ubuntu/michael\n"+
            "wget https://s3.amazonaws.com/michael-dror-distrebuted/worker.jar\n"+
            "java -jar worker.jar";
    public static final String MANAGER_USER_SCRIPT = "#!/bin/bash\n" +
            "apt update -y\n"+
            "apt install openjdk-8-jdk -y\n" +
            "mkdir /home/ubuntu/michael\n" +
            "cd /home/ubuntu/michael\n"+
            "wget https://s3.amazonaws.com/michael-dror-distrebuted/manager.jar\n"+
            "java -jar worker.jar";
}
