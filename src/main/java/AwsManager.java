import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

public class AwsManager {
    protected AWSCredentialsProvider credentialsProvider;

    public AwsManager() {
        this.credentialsProvider = credentialsProvider = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
    }
}
