package aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

public class S3Client extends AwsManager {
    private AmazonS3 s3;

    public S3Client() {
        super();
        this.s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Constants.DEFAULT_REGION).build();
    }

    public Iterator<String> downloadItemAsLines(String bucketName, String keyName) throws IOException {
        S3ObjectInputStream s3is = downloadItem(bucketName, keyName);
        return iteratorFromReader(new BufferedReader(new InputStreamReader(s3is)));
    }

    public static Iterator<String> iteratorFromReader(BufferedReader reader) throws IOException {
        return new Iterator<String>() {
            String nextLine = reader.readLine();

            @Override
            public boolean hasNext() {
                return nextLine != null;
            }

            @Override
            public String next() {
                String lineToReturn = nextLine;
                try {
                    nextLine = reader.readLine();
                } catch (IOException e) {
                    nextLine = null;
                }
                return lineToReturn;
            }
        };
    }

    public PutObjectResult uploadFile(String bucketName, String keyName, File file){
        return s3.putObject(bucketName, keyName, file);
    }

    private S3ObjectInputStream downloadItem(String bucketName, String keyName) {
        try {
            S3Object o = s3.getObject(bucketName, keyName);
            return o.getObjectContent();

        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
            return null;
        }
    }
}
