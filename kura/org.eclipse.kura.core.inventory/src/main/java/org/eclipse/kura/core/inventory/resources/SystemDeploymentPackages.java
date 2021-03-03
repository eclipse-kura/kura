/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.inventory.resources;

public class SystemDeploymentPackages {

    private SystemDeploymentPackage[] deploymentPackages;

    public SystemDeploymentPackage[] getDeploymentPackages() {
        return this.deploymentPackages;
    }

    public void setDeploymentPackages(SystemDeploymentPackage[] deploymentPackages) {
        this.deploymentPackages = deploymentPackages;
    }
}
