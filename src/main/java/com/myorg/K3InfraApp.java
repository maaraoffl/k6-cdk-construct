package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class K3InfraApp {
    public static void main(final String[] args) {
        App app = new App();

        new K3InfraStack(app, "K3InfraStack", StackProps.builder()
                .build());
        app.synth();
    }
}
