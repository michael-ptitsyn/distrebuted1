package aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

public class AwsManager {
    protected AWSCredentialsProvider credentialsProvider;

    public AwsManager() {
        try {
            this.credentialsProvider  = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
        }
        catch (IllegalArgumentException ex){
            this.credentialsProvider=new InstanceProfileCredentialsProvider(true);
        }
    }

    void handleErrors(AmazonServiceException ase) {
        System.out.println("Caught Exception: " + ase.getMessage());
        System.out.println("Reponse Status Code: " + ase.getStatusCode());
        System.out.println("Error Code: " + ase.getErrorCode());
        System.out.println("Request ID: " + ase.getRequestId());
    }
}
