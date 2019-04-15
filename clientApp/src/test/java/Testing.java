import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import java.io.IOException;
import java.util.HashMap;

import static general.GeneralFunctions.createAttrs;
import org.testng.annotations.Test;

public class Testing {
    private static int count = 0;

    @Test
    public void testFinalize() throws IOException {
        Message msg = new Message();
        msg.setBody("public/11f358f2-c786-4bb4-95c6-b84dc24ce3f3res");
        ClientMain.outputName="output";
        sendMsg(msg);
        count++;
    }

    private void sendMsg(Message msg) throws IOException {
        HashMap<String, String> attrebutes = new HashMap<>();
        attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK_RESULT.name());
        msg.setMessageAttributes(createAttrs(attrebutes));
        ClientMain.handleMessage(msg);
    }

}
