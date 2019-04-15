import aws.QueueManager;
import aws.S3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.sqs.model.Message;
import general.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;

import static general.GeneralFunctions.createAttrs;
import static general.GeneralFunctions.listeningloop;
import static general.GeneralFunctions.validateMsg;

public class WorkerMain {
    public static Constants.ec2Status status;
    private static QueueManager queueM = new QueueManager();
    private static String resultQueue = queueM.getOrCreate("resultQueue", Constants.RESULT_QUEUE);
    private static String workQueue = queueM.getOrCreate("workQueue", Constants.WORKQUEUE);
    private static S3Client s3 = new S3Client();

    public static void main(String[] args) {
        final MutableBoolean isTerminated = new MutableBoolean();
        status = Constants.ec2Status.IDLE;
        WorkerPinger wPinger = new WorkerPinger(queueM, resultQueue);
        Thread pinger = new Thread(wPinger);
        pinger.start();
        listeningloop(WorkerMain::handleMessage, workQueue, isTerminated, queueM, () -> status = Constants.ec2Status.IDLE);
    }

    private static void handleMessage(Message msg) {
        HashMap<String, String> attrebutes = new HashMap<>();
        try {
            if (validateMsg(msg)) {
                System.out.println("*******worker: " + msg.getBody());
                String url = solve(msg);
                attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.TASK_RESULT.name());
                attrebutes.put(Constants.REQUEST_ID_FIELD, msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue());
                attrebutes.put(Constants.TASK_ID_FIELD, msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
                attrebutes.put(Constants.ID_FIELD, Constants.instanceId);
                queueM.sendMessage(createAttrs(attrebutes), resultQueue, url);
                status = Constants.ec2Status.WORKING;
            } else {
                queueM.sendMessage(createAttrs(attrebutes), resultQueue, "BAD MSG FORMAT");
            }
        } catch (Exception ex) {
            status = Constants.ec2Status.IDLE;
            attrebutes.put(Constants.TYPE_FIELD, Constants.MESSAGE_TYPE.ERROR.name());
            attrebutes.put(Constants.TASK_ID_FIELD, msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue());
            attrebutes.put(Constants.REQUEST_ID_FIELD, msg.getMessageAttributes().get(Constants.REQUEST_ID_FIELD).getStringValue());
            queueM.sendMessage(createAttrs(attrebutes), resultQueue,   ExceptionUtils.getStackTrace(ex));
        } finally {
            queueM.deleteMsg(workQueue, msg);
        }
    }


    public static String solve(Message msg) throws IOException {
        String toSolve = msg.getBody();
        String taskId = msg.getMessageAttributes().get(Constants.TASK_ID_FIELD).getStringValue();
        String[] splited = toSolve.split("\t");
        Constants.TASKS current = Constants.TASKS.valueOf(splited[0]);
        PDDocument doc = PDDocument.load(new BufferedInputStream(new URL(splited[1]).openStream()));
        File resultFile = new File("result");
        switch (current) {
            case ToHTML:
                PDFText2HTML pdf2html = new PDFText2HTML();
                pdf2html.setStartPage(0);
                pdf2html.setEndPage(1);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                Writer output = new OutputStreamWriter(outStream);
                pdf2html.writeText(doc, output);
                output.flush();
                byte[] buff = outStream.toByteArray();
                InputStream inputStream = new ByteArrayInputStream(buff);
                output.close();
                doc.close();
                return uploadResult(inputStream, taskId, ".html", buff.length);
            case ToImage:
                PDFRenderer pdfRenderer = new PDFRenderer(doc);
                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bim, "png", os);
                byte[] buffer = os.toByteArray();
                InputStream is = new ByteArrayInputStream(buffer);
                doc.close();
                return uploadResult(is, taskId, ".png", buffer.length);
            case ToText:
                PDFTextStripper reader = new PDFTextStripper();
                reader.setStartPage(0);
                reader.setEndPage(1);
                String pageText = reader.getText(doc);
                FileUtils.writeStringToFile(resultFile, pageText, "utf-8");
                doc.close();
                return uploadResult(resultFile, taskId, ".txt");
            default:
                doc.close();
                throw new IOException("we recieved bad task");
        }
    }

    private static String uploadResult(File file, String id, String suffix) {
        String key = Constants.PUBLIC_FOLDER + Constants.instanceId + "-" + id + suffix;
        s3.uploadFile(Constants.BUCKET_NAME, key, file);
        file.deleteOnExit();
        return s3.getUrl(Constants.BUCKET_NAME, key).toString();
    }

    private static String uploadResult(InputStream ins, String id, String suffix, int length) {
        String key = Constants.PUBLIC_FOLDER + Constants.instanceId + "-" + id + suffix;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(length);
        s3.uploadFile(Constants.BUCKET_NAME, key, ins, meta);
        return s3.getUrl(Constants.BUCKET_NAME, key).toString();
    }
}