package aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import general.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EcManager extends AwsManager {
    private AWSCredentialsProvider credentialsProvider;
    private String region;
    private List<Instance> mechines;
    private AmazonEC2 ec2;

    //TODO should be singletone
    public EcManager(String region) {
        super();
        this.region = region;
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
        mechines = getActiveEc2s();
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
            IamInstanceProfileSpecification iam = new IamInstanceProfileSpecification();
            iam.withArn("arn:aws:iam::993541871317:instance-profile/worker");
            request.withIamInstanceProfile(iam);
            if (userData != null) {
                String base64UserData = new String(Base64.encodeBase64(userData.getBytes("UTF-8")), "UTF-8");
                request.withUserData(base64UserData);
            }
            request.setInstanceType(InstanceType.T1Micro.toString());
            List<Instance> instances = ec2.runInstances(request).getReservation().getInstances();
            mechines.addAll(instances);
            System.out.println("Launch instances: " + instances);
            return instances;
        } catch (AmazonServiceException ase) {
            System.out.println(ExceptionUtils.getStackTrace(ase));
            handleErrors(ase);
        } catch (UnsupportedEncodingException e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
        }
        return null;
    }

    public List<Instance> getNotStoped() {
        return listEc2s().stream().filter(s -> s.getState().getCode() < 32).collect(Collectors.toList());
    }

    public List<Instance> getActiveEc2s() {
        return listEc2s().stream().filter(s -> s.getPublicIpAddress() != null).collect(Collectors.toList());
    }

    private List<Instance> listEc2s() {
        List<Instance> result = new LinkedList<>();
        try {
            boolean done = false;
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            while (!done) {
                DescribeInstancesResult response = ec2.describeInstances(request);
                for (Reservation reservation : response.getReservations()) {
                    result.addAll(reservation.getInstances());
                }
                request.setNextToken(response.getNextToken());
                if (response.getNextToken() == null) {
                    done = true;
                }
            }
        } catch (AmazonServiceException ase) {
            System.out.println(ExceptionUtils.getStackTrace(ase));
            handleErrors(ase);
        }
        return result;
    }

    private Instance getEc2(String instanceId) {
        try {
            boolean done = false;
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            request.withInstanceIds(instanceId);
            DescribeInstancesResult response = ec2.describeInstances(request);
            return response.getReservations().get(0).getInstances().get(0);
        } catch (AmazonServiceException ase) {
            System.out.println(ExceptionUtils.getStackTrace(ase));
            handleErrors(ase);
        }
        return null;
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
            System.out.println(ExceptionUtils.getStackTrace(ase));
            handleErrors(ase);
            return null;
        }
    }

    public List<InstanceStateChange> terminateAll() {
        List<InstanceStateChange> result = terminateEc2(mechines.stream().map(Instance::getInstanceId).collect(Collectors.toList()));
        mechines.clear();
        return result;
    }

    public CreateTagsResult createTags(List<String> ids, Map<String, String> keyVal) {
        List<Tag> tagList = keyVal.keySet().stream().map(k -> new Tag(k, keyVal.get(k))).collect(Collectors.toList());
        CreateTagsRequest tagReq = new CreateTagsRequest(ids, tagList);
        return ec2.createTags(tagReq);
    }

    public List<Instance> getByTag(String keyName, String value) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        List<String> valuesT1 = new ArrayList<String>();
        valuesT1.add(value);
        Filter filter = new Filter("tag:" + keyName, valuesT1);
        DescribeInstancesResult result = ec2.describeInstances(request.withFilters(filter));
        List<Reservation> reservations = result.getReservations();
        return reservations.stream().flatMap(r -> r.getInstances().stream()).collect(Collectors.toList());
    }

    public static boolean isManager(Instance ec) {
        return ec.getTags().stream().anyMatch(t -> t.getKey().equals(Constants.MANAGER_TAG) && t.getValue().equals("true"));
    }
}
