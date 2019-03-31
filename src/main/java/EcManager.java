import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;

public class EcManager {
        public static String JAVA8IMG = "ami-0b8962a37670b6d05";
        private AWSCredentialsProvider credentialsProvider;
        private static final String DEFAULT_REGION="us-east-1";
        private String region;
    public EcManager(String region) {
        credentialsProvider = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
        this.region=region;
    }

    public EcManager() {
        credentialsProvider = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
        this.region=DEFAULT_REGION;
    }

    //    public List<Instance> initMechines(int num, List<String> userData){
//        return IntStream.range(0, num)
//                .mapToObj(s->createEc2(userData.get(s)))
//                .collect(Collectors.toList());
//    }
    @Nullable
    public List<Instance> createEc2(int num, String image, @Nullable String userData){
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
        try {
            // Basic 32-bit Amazon Linux AMI 1.0 (AMI Id: ami-08728661)
            RunInstancesRequest request = new RunInstancesRequest(image, num, num);
            if(userData!=null) {
                request.withUserData(userData);
            }
            request.setInstanceType(InstanceType.T1Micro.toString());
            List<Instance> instances = ec2.runInstances(request).getReservation().getInstances();
            System.out.println("Launch instances: " + instances);
            return instances;
        } catch (AmazonServiceException ase) {
            handleErrors(ase);
            return null;
        }
    }

    @Nullable
    public List<InstanceStateChange> terminateEc2(List<String> instanceIds){

        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
        try {
            // Basic 32-bit Amazon Linux AMI 1.0 (AMI Id: ami-08728661)
            TerminateInstancesRequest request = new TerminateInstancesRequest(instanceIds);
            List<InstanceStateChange> instances = ec2.terminateInstances(request).getTerminatingInstances();
            System.out.println("terminating instances: " + instances);
            return instances;
        } catch (AmazonServiceException ase) {
            handleErrors(ase);
            return null;
        }
    }

    private void handleErrors(AmazonServiceException ase){
        System.out.println("Caught Exception: " + ase.getMessage());
        System.out.println("Reponse Status Code: " + ase.getStatusCode());
        System.out.println("Error Code: " + ase.getErrorCode());
        System.out.println("Request ID: " + ase.getRequestId());
    }

//    public static void main(String[] args) throws Exception {
//        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider().getCredentials());
//        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
//                .withCredentials(credentialsProvider)
//                .withRegion("us-west-1")
//                .build();
//
//        try {
//            // Basic 32-bit Amazon Linux AMI 1.0 (AMI Id: ami-08728661)
//            RebootInstancesRequest reboot = new RebootInstancesRequest(ImmutableList.<String>of("i-053c057ecc7248ecb"));
////            RunInstancesRequest request = new RunInstancesRequest("ami-8c1fece5", 1, 1);
////            request.setInstanceType(InstanceType.T1Micro.toString());
//            ec2.rebootInstances(reboot);
//            System.out.println("Rebooted please check !!!" );
//
//        } catch (AmazonServiceException ase) {
//            System.out.println("Caught Exception: " + ase.getMessage());
//            System.out.println("Reponse Status Code: " + ase.getStatusCode());
//            System.out.println("Error Code: " + ase.getErrorCode());
//            System.out.println("Request ID: " + ase.getRequestId());
//        }
//    }
}
