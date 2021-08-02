package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
public class K3NetworkStack extends Stack{
	public K3NetworkStack(final Construct scope, final String id){
		this(scope, id, null);
	}

	public K3NetworkStack(final Construct scope, final String id, final StackProps props){
		super(scope, id, props);
		SecurityGroup k3SecurityGroup = SecurityGroup.Builder.create(this, "K3SecurityGroup")
		.allowAllOutbound(true)
		.securityGroupName("K3SecurityGroup")
		.description("Security group for k3")
		.vpc(
			Vpc.fromLookup(
				this,
				"referenceVpc",
				VpcLookupOptions.builder().vpcId("vpc-234ed648").build()
			)
		)
		.build();
		k3SecurityGroup.addIngressRule(Peer.anyIpv4(),Port.allTraffic());
		k3SecurityGroup.addIngressRule(Peer.anyIpv6(),Port.allTraffic());
	}
}