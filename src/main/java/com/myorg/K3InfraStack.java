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

public class K3InfraStack extends Stack {
    public K3InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public K3InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        UserData influxUserData = UserData.custom(readUserData("influx_userdata.sh"));

        UserData grafanaUserData = UserData.custom(readUserData("userdata.sh"));

        IVpc k3Vpc = Vpc.fromLookup(this, "referenceVpc",
            VpcLookupOptions.builder().isDefault(true).build());

        ISecurityGroup k3SG = SecurityGroup.fromLookup(this, "refK3SecurityGroup", 
            "sg-0b978b2eaf130e960");

        Instance influx = Instance.Builder.create(this, "K3InfluxInstance")
        .keyName("k3stack")
        .role(Role.fromRoleArn(this, "refK3InfraForInflux", "arn:aws:iam::514551510561:role/K3Infra"))
        .instanceType(new InstanceType("t2.micro"))
        .machineImage(new AmazonLinuxImage(
            AmazonLinuxImageProps.builder().generation(AmazonLinuxGeneration.AMAZON_LINUX_2).build()
        ))
        .vpc(k3Vpc)
        .securityGroup(k3SG)
        .userData(influxUserData)
        .instanceName("k3-influxdb")
        .build();
        
        Instance grafana = Instance.Builder.create(this, "K3GrafanaInstance")
        .keyName("k3stack")
        .role(Role.fromRoleArn(this, "refK3InfraForGrafana", "arn:aws:iam::514551510561:role/K3Infra"))
        .instanceType(new InstanceType("t2.micro"))
        .machineImage(new AmazonLinuxImage(
            AmazonLinuxImageProps.builder().generation(AmazonLinuxGeneration.AMAZON_LINUX_2).build()
        ))
        .vpc(k3Vpc)
        .securityGroup(k3SG)
        .userData(grafanaUserData)
        .instanceName("k3-grafana")
        .build();

        NetworkLoadBalancer nlb = NetworkLoadBalancer.Builder.create(this, "K3LoadBalancer")
        .vpc(k3Vpc)
        .loadBalancerName("k3-lb")
        .internetFacing(true)
        .build();

        InstanceTarget grafanaInstanceTarget = new InstanceTarget(grafana.getInstanceId(), 3000);
        NetworkTargetGroup ntgTCP3000 = NetworkTargetGroup.Builder.create(this, "NTGTCP3000")
        .port(3000)
        .targetType(TargetType.INSTANCE)
        .vpc(k3Vpc)
        .build();
        BaseNetworkListenerProps nlGrafanaProps = BaseNetworkListenerProps.builder()
        .port(3000)
        .defaultTargetGroups(Arrays.asList(ntgTCP3000))
        .build();
        ntgTCP3000.addTarget(grafanaInstanceTarget);

        InstanceTarget influxInstanceTarget = new InstanceTarget(influx.getInstanceId(), 8086);
        NetworkTargetGroup ntgTCP8086 = NetworkTargetGroup.Builder.create(this, "NTGTCP8086")
        .port(8086)
        .targetType(TargetType.INSTANCE)
        .vpc(k3Vpc)
        .build();
        BaseNetworkListenerProps nlInfluxProps = BaseNetworkListenerProps.builder()
        .port(8086)
        .defaultTargetGroups(Arrays.asList(ntgTCP8086))
        .build();
        ntgTCP8086.addTarget(influxInstanceTarget);

        nlb.addListener("Grafana", nlGrafanaProps);
        nlb.addListener("Influx", nlInfluxProps);
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
