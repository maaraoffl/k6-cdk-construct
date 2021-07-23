package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.RollingUpdateConfiguration;
import software.amazon.awscdk.services.ec2.AmazonLinuxImage;
import software.amazon.awscdk.services.ec2.ExecuteFileOptions;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;

public class K3InfraStack extends Stack {
    public K3InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public K3InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        UserData userData = UserData.forLinux();
        userData.addCommands("yum update -y",
        "yum install -y docker",
        "service docker start",
        "usermod -a -G docker ec2-user"
        );

        AutoScalingGroup.Builder.create(this, "K3GrafanaAsg")
        .desiredCapacity(1)
        .instanceType(new InstanceType("t2.micro"))
        .vpc(Vpc.fromLookup(this, "referenceVpc",
            VpcLookupOptions.builder().isDefault(true).build())
        ).machineImage(new AmazonLinuxImage())
        .keyName("k3stack")
        .userData(userData)

        .securityGroup(SecurityGroup.fromLookup(this, "refK3SecurityGroup", 
            "sg-0b978b2eaf130e960"))
        .build();
    }
}
