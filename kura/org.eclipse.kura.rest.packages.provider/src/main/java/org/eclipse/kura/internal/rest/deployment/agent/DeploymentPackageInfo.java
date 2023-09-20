/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package org.eclipse.kura.internal.rest.deployment.agent;

public class DeploymentPackageInfo {

    private final String name;
    private final String version;

    public DeploymentPackageInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

}
