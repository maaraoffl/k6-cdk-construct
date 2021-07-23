package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class K3InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
            .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
            .region(System.getenv("CDK_DEFAULT_REGION"))
            .build();
        new K3InfraStack(app, "K3InfraStack",
            StackProps.builder().env(env).build());

        new K3NetworkStack(app, "K3NetworkStack", 
            StackProps.builder().env(env).build());
        
        app.synth();
    }
}
