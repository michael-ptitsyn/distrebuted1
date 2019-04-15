package objects;

public class EcTask {
    private String clientName;
    private String result_url;
    private String body;
    private String operation;
    private int id;
    private boolean done;

    public String getOperation() {
        return operation;
    }

public EcTask(String clientName, String operation, String body, String result_url, int id) {
        this.clientName = clientName;
        this.result_url = result_url;
        this.operation = operation;
        this.body = body;
        this.id = id;

        done = false;
    }

    public boolean notDone() {
        return !done;
    }

    public void setDone(boolean done) {
        this.done = done;
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
}
