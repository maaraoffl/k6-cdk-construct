package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;

public class K3InfraStack extends Stack {
    public K3InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public K3InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket myBucket = Bucket.Builder.create(this, "myBucket")
            .bucketName("my-stream-354")
            .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
            .encryption(BucketEncryption.S3_MANAGED)
            .versioned(true)
            .build();

        Tags.of(this).add("OwnerContact", "theslidingwindow@gmail.com");
        System.out.println(myBucket.getBucketArn());
        
    }
}
