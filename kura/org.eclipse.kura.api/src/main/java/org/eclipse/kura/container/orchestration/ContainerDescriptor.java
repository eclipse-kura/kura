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

    /**
     * Returns the container status as {@link ContainerState}
     *
     * @return
     */
    public ContainerState getContainerState();

    /**
     * The method will provide information if the container is or not managed by the framework
     *
     * @return <code>true</code> if the framework manages the container. <code>false</code> otherwise
     */
    public Boolean isFrameworkManaged();

    /**
     * Returns the container name
     *
     * @return
     */
    public String getContainerName();

    /**
     * Returns the base image for the associated container
     *
     * @return
     */
    public String getContainerImage();

    /**
     * Returns the image tag for the associated container
     *
     * @return
     */
    public String getContainerImageTag();

    /**
     * Returns the containerID
     *
     * @return
     */
    public String getContainerId();

    /**
     * Returns the list of external ports that will be mapped to the given container
     *
     * @return
     */
    public List<Integer> getContainerPortsExternal();

    /**
     * Returns the list of internal ports that will be mapped to the given container
     *
     * @return
     */
    public List<Integer> getContainerPortsInternal();

    /**
     * Returns the list of environment properties that will be passed to the container
     *
     * @return
     */
    public List<String> getContainerEnvVars();

    /**
     * Returns the list of devices that will be mapped to the container
     *
     * @return
     */
    public List<String> getContainerDevices();

    /**
     * Returns a map that identifies the volumes and the internal container mapping
     *
     * @return
     */
    public Map<String, String> getContainerVolumes();

    /**
     * Returns a boolean representing if the container runs or will run in privileged mode.
     *
     * @return
     */
    public Boolean getContainerPrivileged();

    /**
     * Static method that will extract the {@link ContainerDescriptor} matching the given name in the given list
     *
     * @param name
     * @param serviceList
     * @return
     */
    public static ContainerDescriptor findByName(String name, List<ContainerDescriptor> containerDescriptors) {
        ContainerDescriptor sd = null;
        for (ContainerDescriptor container : containerDescriptors) {
            if (container.getContainerName().equals(name)) {
                return container;
            }
        }
        return sd;
    }

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