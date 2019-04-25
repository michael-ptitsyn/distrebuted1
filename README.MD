# Distrebuted - task1
## Quick Start 
1. make sure you have a valid aws acount 
2. make sure you have aws credentials installed on your local mechine 
> c:/user/YOURNAME/.aws/credentials
3. in your aws acount create an IAM role named "worker" with the following policies:
  - AmazonEC2FullAccess 
  - AmazonSQSFullAccess 
  - AmazonS3FullAccess 
  - IAMReadOnlyAccess 
  - custom policy named attach-role:
```
  {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "iam:PassRole",
                "iam:ListInstanceProfiles",
                "ec2:*"
            ],
            "Resource": "*"
        }
    ]
}
```
4. create an ec2 ubuntu 16 image with java8 installed:
  - create an ec2, log on and run the following commands:
  ```
  sudo apt update -y
  sudo apt install openjdk-8-jdk -y
  ```
  - logout, go to aws console, right click on the relevant mechine and create the Image 
  - find out the image ami (aws console->ec2->AMIs)
  - copy the ami and paste into Constants file under JAVA8IMG
  - alternative way to avoid creating an image - paste the default ubuntu 16 ami and update the relevant userscripts to install java on bootstrap:
  ```
  public static final String WORKER_USER_SCRIPT_SHORT = "#!/bin/bash\n" +
            "apt update -y\n"+
            "apt install openjdk-8-jdk -y\n" +
            "mkdir /home/ubuntu/michael\n" +
            "cd /home/ubuntu/michael\n"+
            "wget https://s3.amazonaws.com/michael-dror-distrebuted/public/worker.jar\n"+
            "java -jar worker.jar $1 $2 > output";
  
  public static final String MANAGER_USER_SCRIPT_SHORT = "#!/bin/bash\n" +
            "apt update -y\n"+
            "apt install openjdk-8-jdk -y\n" +
            "mkdir /home/ubuntu/distrebuted\n" +
            "cd /home/ubuntu/distrebuted\n"+
            "wget https://s3.amazonaws.com/michael-dror-distrebuted/public/manager.jar\n"+
            "java -jar manager.jar > output";
  ```
5. in aws console make sure you have a valid keyPair or crete one.
  - update project's Constants file under KEY_PAIR set value to YOUR_KEYPAIR_NAME
  >PATHTOPROJECT\utils\src\main\java\general\Constants.java 
6. create a SQS queue name "mianQueue":
  - copy queue url and paste in project's Constants file under MAINQUEUE variable
  >PATHTOPROJECT\utils\src\main\java\general\Constants.java
7. Build the project, the created jars will appear in:
  >PATHTOPROJECT\out\artifacts
8. in your aws account create a s3 bucket named: michael-dror-distrebuted
  - create a directory named public
  - make the directory public (right click -> make public)
  - upload the project artifacts(manager.jar, worker.jar) to public folder
>to configure the name above please update the Constants file in the project
9. from your local mechine (with aws credentials) run the following comand:
  - *n is worker to msg ratio*
  - *terminate not mendatory*
> java -jar yourjar.jar inputFileName outputFileName n terminate


## Debug:
1. manager's run log will be avaliable in your s3 bucket (root dir, not public) after manager trminates
2. for workers you can log on to ec2 using putty (or ssh on linux) and your private key (5), cd to ~/distrebuted/ , and read the output file   

## Design
1. Queues:
    - mainQueue static queue, must run all the time, through this queue clients astablish heandShake and send their requests
    - workqueue created/terminated by the manager, we use this queue to post tasks to thw workers
    - result queue created/terminated by the manaer, we use is to recieve status updates, results and tasks Inits  from workers
    - clientResultQueue created/terminated by each client, we use it to recieve the results from the manager
2. the client flow:
    - checks whether a manager is exist is not creates a new one (we mark each manager with a special tag)
    - creates a new queue (client result queue)
    - sends the client result queue url via the mainQueue to the manager
    - after hendshak, working as insructed in the task
3. the manager flow:
    - has 4 running threads - mainthread, ECfeeder, EClistener, ECrefreshe for more info see **important questions** section
    - apone termination waits for all tasks to be done, and for all workers to send IDLE STATUS
    - task managment - each client/worker have a unique request id, each msg marked with this unique request id.
    The manager stores all the taskes in a hash map where the key is the request id and the val is a list of all the subtasks. 
    Each subtask is an ECtask object, each Ectask object is translated into a msg with the following attrebutes:
      - client request id 
      - task id - the task index in the list under the request id key
      - MSG TYPE - TASK
4. the worker flow:
    - has 2 running threads - mainthread, pinger thread.
    - mainthread incharge of doing the actual job
    - pinger sends a status updates to the manager 
    - the statuses are: WORKING, IDLE, DEAD
5. the heandShake:
    - when a client app starts the app, sends a msg via the main queue, the msg contains the clients Result queue url (known only to the client)
    - when a manger recieves the msg the manager sendes INIT MSG using the clients result queue (by the url recived)
    - when a client recieves the INIT msg using the clients result queue the handshake is finished
  
  
## important questions:
1. security: we use only AMI ROLES under any circumstances don't upload you credentials using user scripts or aws bucket
2. scalability: we address scalabily in our manager design, the main thread is incharge on the hend shake procedure,
this is very light procidure, all the hard "manager work" is done by other threads:
  - ECfeeder :
    - recieves the client msg (collected by main thread) 
    - download the input file
    - breaks down the input file into tasks, sends each task to the workers using the WORKQUEUE
  - EClistener:
    - waits for results or status updates from the workers
    - listens to the RESULTQUEUE
    - parses the results, apone complition generates summery file and sends to the cliens
  - ECRefresher:
    - runs in interval configured in Constans file
    - incharge to find "broken" workers, resend thire messages and create new workers instead of the dead
 3. persistence ECRefresher thread uses the following algorithm:
    - checks if one of the workers is in not running states (reboot, stoped, terminated)
      - for each not running, find out what was the task he was working on, resend the task
    - checks if one the workers is stoped/terminated
      - according to the list size creates new workers.
    > NOTE: for each worker we know what is the currently proccessed message by the following logic:
    > - before deleting msg from work queue each worker send his Id and the msg he collected from queue
    > - afterwards he delets the msg from the queue
    > - the EClistener recives the msg and updates a special hashmap 
