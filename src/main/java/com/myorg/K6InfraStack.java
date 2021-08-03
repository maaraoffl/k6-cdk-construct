package com.myorg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.AmazonLinuxGeneration;
import software.amazon.awscdk.services.ec2.AmazonLinuxImage;
import software.amazon.awscdk.services.ec2.AmazonLinuxImageProps;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.InstanceTarget;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.amazon.awscdk.services.iam.Role;

public class K6InfraStack extends Stack {
    public K6InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public K6InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        IVpc k6Vpc = Vpc.fromLookup(this, "referenceVpc",
            VpcLookupOptions.builder().isDefault(true).build());

        ISecurityGroup k6SG = SecurityGroup.fromLookup(this, "refK3SecurityGroup", 
            "sg-0b978b2eaf130e960");

        NetworkLoadBalancer nlb = NetworkLoadBalancer.Builder.create(this, "K6LoadBalancer")
        .vpc(k6Vpc)
        .loadBalancerName("K6LB")
        .internetFacing(true)
        .build();
    
        UserData influxUserData = UserData.custom(readUserData("influx_userdata.sh"));
        
        String prefix = "#!/bin/bash\nexport INFLUX_DB_URL="+nlb.getLoadBalancerDnsName()+"\n";
        UserData grafanaUserData = UserData.custom(prefix+readUserData("grafana_userdata.sh"));
        Instance influx = createInstance("K6Influx", k6Vpc, k6SG, influxUserData);
        Instance grafana = createInstance("K6Grafana", k6Vpc, k6SG, grafanaUserData);

        attachInstanceToLB(influx, nlb, k6Vpc, 8086);
        attachInstanceToLB(grafana, nlb, k6Vpc, 3000);
    }

    void attachInstanceToLB(Instance instance, NetworkLoadBalancer nlb, IVpc vpc, Integer port){
       
        InstanceTarget instanceTarget = new InstanceTarget(instance.getInstanceId(), port);
        NetworkTargetGroup ntgTCP = NetworkTargetGroup.Builder.create(this, String.format("NTG%d", port))
        .port(port)
        .targetType(TargetType.INSTANCE)
        .vpc(vpc)
        .build();
        ntgTCP.addTarget(instanceTarget);

        BaseNetworkListenerProps nlProps = BaseNetworkListenerProps.builder()
        .port(port)
        .defaultTargetGroups(Arrays.asList(ntgTCP))
        .build();
        nlb.addListener(String.format("NTG%d", port), nlProps);
    }

    Instance createInstance(String name, IVpc vpc, ISecurityGroup sg, UserData userdata){
        return Instance.Builder.create(this, name + "Instance")
        .keyName("k3stack")
        .role(Role.fromRoleArn(this, "ref" + name + "Role", "arn:aws:iam::514551510561:role/K3Infra"))
        .instanceType(new InstanceType("t2.micro"))
        .machineImage(new AmazonLinuxImage(
            AmazonLinuxImageProps.builder().generation(AmazonLinuxGeneration.AMAZON_LINUX_2).build()
        ))
        .vpc(vpc)
        .securityGroup(sg)
        .userData(userdata)
        .instanceName(name + "Instance")
        .build();
    }

    String readUserData(String fileName){
        try {
            Path path = Paths.get(getClass().getClassLoader()
            .getResource(fileName).toURI());
            Stream<String> lines = Files.lines(path);
            String data = lines.collect(Collectors.joining("\n"));
            lines.close(); 
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";   
    }
}
