public class Constants {
    public static String JAVA8IMG = "ami-0b8962a37670b6d05";
    public static String JAVA8IMG_WITH_CREDS = "ami-0b8962a37670b6d05";
    public static final String DEFAULT_REGION="us-east-1";
    public static final int TIMEOUT=10;
    public static final String MAINQUEUE="https://sqs.us-east-1.amazonaws.com/993541871317/MyQueue662d7826-4f15-409b-97ef-654c2b005011";
    public static final String WORKQUEUE="https://sqs.us-east-1.amazonaws.com/993541871317/MyQueue32e6f304-f5e2-453e-adc6-c687428d438b";
    public static final String RESULT_QUEUE="https://sqs.us-east-1.amazonaws.com/993541871317/MyQueue119b949f-1461-4130-a018-7d4a0ebe6aac";
    public static final int THREAD_SLEEP=1000;
    public static final String BUCKET_NAME="michael-dror-distrebuted";
    public static final int DEFAULT_MSG_COMP_RATION=50;
    public static final String KEY_PAIR = "ptitsyn_bgu";
    public static final String TYPE_FIELD = "type";
    public static final String ID_FIELD = "instanceId";
    public enum  MESSAGE_TYPE {
        STATUS_UPDATE,
        TASK,
        TERMINATION,
        TASK_RESULT
    }
    public static final String WORKER_USER_SCRIPT = "#!/bin/bash\n" +
            "yum update -y\n" +
            "amazon-linux-extras install -y lamp-mariadb10.2-php7.2 php7.2\n" +
            "yum install -y httpd mariadb-server\n" +
            "systemctl start httpd\n" +
            "systemctl enable httpd\n" +
            "usermod -a -G apache ec2-user\n" +
            "chown -R ec2-user:apache /var/www\n" +
            "chmod 2775 /var/www\n" +
            "find /var/www -type d -exec chmod 2775 {} \\;\n" +
            "find /var/www -type f -exec chmod 0664 {} \\;\n" +
            "echo \"<?php phpinfo(); ?>\" > /var/www/html/phpinfo.php";
}
