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

import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;

public class SystemDeploymentPackage extends SystemResourceInfo {

    private SystemBundle[] bundleInfos;

    public SystemDeploymentPackage(String name) {
        super(name);
    }

    public SystemDeploymentPackage(String name, String version) {
        super(name, version, SystemResourceType.DP);
    }

    public SystemBundle[] getBundleInfos() {
        return this.bundleInfos;
    }

    public void setBundleInfos(SystemBundle[] bundleInfos) {
        this.bundleInfos = bundleInfos;
    }
}
