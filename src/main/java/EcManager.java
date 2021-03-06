import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EcManager extends AwsManager {
    private AWSCredentialsProvider credentialsProvider;
    private String region;
    private List<Instance> mechines;
    private AmazonEC2 ec2;

    public EcManager(String region) {
        super();
        this.region = region;
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    public EcManager() {
        this(Constants.DEFAULT_REGION);
    }

    //    public List<Instance> initMechines(int num, List<String> userData){
//        return IntStream.range(0, num)
//                .mapToObj(s->createEc2(userData.get(s)))
//                .collect(Collectors.toList());
//    }
    @Nullable
    public List<Instance> createEc2(int num, String image, @Nullable String userData) {
        try {
            RunInstancesRequest request = new RunInstancesRequest(image, num, num);
            request.withKeyName(Constants.KEY_PAIR);
            if (userData != null) {
                String base64UserData = new String(Base64.encodeBase64( userData.getBytes( "UTF-8" )), "UTF-8" );
                request.withUserData(base64UserData);
            }
            request.setInstanceType(InstanceType.T1Micro.toString());
            List<Instance> instances = ec2.runInstances(request).getReservation().getInstances();
            System.out.println("Launch instances: " + instances);
            return instances;
        } catch (AmazonServiceException ase) {
            handleErrors(ase);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Instance> getActiveEc2s() {
        List<Instance> result = new LinkedList<>();
        try {
            boolean done = false;
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            while (!done) {
                DescribeInstancesResult response = ec2.describeInstances(request);
                for (Reservation reservation : response.getReservations()) {
                    result.addAll(reservation.getInstances().stream().filter(s->s.getPublicIpAddress()!= null).collect(Collectors.toList()));
                }
                request.setNextToken(response.getNextToken());
                if (response.getNextToken() == null) {
                    done = true;
                }
            }
        }catch (AmazonServiceException ase) {
            handleErrors(ase);
        }
        return result;
    }

    @Nullable
    public List<InstanceStateChange> terminateEc2(List<String> instanceIds) {


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
