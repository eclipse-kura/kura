/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import java.util.List;

import org.eclipse.kura.system.SystemResourceInfo;

public class SystemResourcesInfo {

    private List<SystemResourceInfo> resources;

    public SystemResourcesInfo(List<SystemResourceInfo> resources) {
        this.resources = resources;
    }

    public List<SystemResourceInfo> getSystemResources() {
        return this.resources;
    }

    public void setSysteResourcesInfo(List<SystemResourceInfo> resources) {
        this.resources = resources;
    }

}
