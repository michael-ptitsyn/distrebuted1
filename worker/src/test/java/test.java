import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

import static general.GeneralFunctions.createAttrs;

public class test {
    private static int count =0;
    @Test
    public void toText() throws IOException {
        Message msg = new Message();
        msg.setBody("ToText\thttp://www.africau.edu/images/default/sample.pdf");
        String result = solveTest(msg);
        System.out.println(result);
        Assert.assertNotNull(result);
        count++;
    }

    @Test
    public void toImg() throws IOException {
        Message msg = new Message();
        msg.setBody("ToImage\thttp://www.africau.edu/images/default/sample.pdf");
        String result = solveTest(msg);
        System.out.println(result);
        Assert.assertNotNull(result);
        count++;
    }

    @Test
    public void toHtml() throws IOException {
        Message msg = new Message();
        msg.setBody("ToHTML\thttp://www.africau.edu/images/default/sample.pdf");
        String result = solveTest(msg);
        System.out.println(result);
        Assert.assertNotNull(result);
        count++;
    }

    private String solveTest(Message msg) throws IOException {
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK.name());
        attrebutes.put(Constants.REQUEST_ID_FIELD, "123123");
        attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
        attrebutes.put(Constants.TASK_ID_FIELD, Integer.toString(count));
        msg.setMessageAttributes(createAttrs(attrebutes));
        return WorkerMain.solve(msg);
    }
}
