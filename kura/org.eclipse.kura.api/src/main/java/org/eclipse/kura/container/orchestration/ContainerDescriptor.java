/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.container.orchestration;

import java.util.List;
import java.util.Map;

/**
 * Object which represents a container. Used to track running containers.
 *
 * @since 2.3
 *
 */
public interface ContainerDescriptor {

    public static ContainerDescriptor findByName(String name, List<ContainerDescriptor> serviceList) {
        ContainerDescriptor sd = null;
        for (ContainerDescriptor container : serviceList) {
            if (container.getContainerName().equals(name)) {
                return container;
            }
        }
        return sd;
    }

    public ContainerStates getContainerState();

    public Boolean isFrameworkManaged();

    public String getContainerName();

    public String getContainerImage();

    public String getContainerImageTag();

    public String getContainerId();

    public List<Integer> getContainerPortsExternal();

    public List<Integer> getContainerPortsInternal();

    public List<String> getContainerEnvVars();

    public List<String> getContainerDevices();

    public Map<String, String> getContainerVolumes();

    public Boolean getContainerPrivileged();

    public static int compare(ContainerDescriptor obj1, ContainerDescriptor obj2) {
        return obj1.getContainerImage().compareTo(obj2.getContainerImage());
    }

    public static boolean equals(ContainerDescriptor obj1, ContainerDescriptor obj2) {
        boolean resultBuilder;

        resultBuilder = obj1.getContainerName().equals(obj2.getContainerName());
        resultBuilder = resultBuilder && obj1.getContainerImage().equals(obj2.getContainerImage());
        resultBuilder = resultBuilder && obj1.getContainerImageTag().equals(obj2.getContainerImageTag());
        resultBuilder = resultBuilder && obj1.getContainerPortsExternal().equals(obj2.getContainerPortsExternal());
        resultBuilder = resultBuilder && obj1.getContainerPortsInternal().equals(obj2.getContainerPortsInternal());
        resultBuilder = resultBuilder && obj1.getContainerEnvVars().equals(obj2.getContainerEnvVars());
        resultBuilder = resultBuilder && obj1.getContainerDevices().equals(obj2.getContainerDevices());
        resultBuilder = resultBuilder && obj1.getContainerVolumes().equals(obj2.getContainerVolumes());
        resultBuilder = resultBuilder && obj1.getContainerPrivileged().equals(obj2.getContainerPrivileged());
        resultBuilder = resultBuilder && obj1.getContainerState().equals(obj2.getContainerState());
        resultBuilder = resultBuilder && obj1.isFrameworkManaged().equals(obj2.isFrameworkManaged());

        return resultBuilder;
    }
}