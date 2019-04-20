package objects;

import com.amazonaws.services.sqs.model.MessageAttributeValue;

import java.util.Map;

public class EcTask {
    private String clientName;
    private String result_url;
    private String body;
    private String operation;
    private int id;
    private boolean done;
    private Map<String, MessageAttributeValue> attributes;


    private String worker;

    public String getOperation() {
        return operation;
    }

public EcTask(String clientName, String operation, String body, String result_url, int id, Map<String, MessageAttributeValue> attrs) {
        this.clientName = clientName;
        this.result_url = result_url;
        this.operation = operation;
        this.body = body;
        this.id = id;
        this.attributes=attrs;
        done = false;
    }

public EcTask(String clientName, String operation, String body, String result_url, int id, boolean done) {
        this(clientName, operation, body, result_url, id, null);
        this.done=done;
}

    public boolean notDone() {
        return !done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public Map<String, MessageAttributeValue> getAttributes() {
        return attributes;
    }

    public String getClientName() {
        return clientName;
    }

    public String getResult_url() {
        return result_url;
    }

    public String getBody() {
        return body;
    }

    public int getId() {
        return id;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setResult_url(String result_url) {
        this.result_url = result_url;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    @Override
    public String toString() {
        return "EcTask{" +
                ", result_url='" + result_url + '\'' +
                ", body='" + body + '\'' +
                ", operation='" + operation + '\'' +
                ", id=" + id +
                ", done=" + done +
                ", attributes=" + attributes +
                ", worker='" + worker + '\'' +
                '}';
    }
}
