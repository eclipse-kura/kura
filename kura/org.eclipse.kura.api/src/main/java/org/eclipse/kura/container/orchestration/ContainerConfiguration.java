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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Object which represents a container. Used to track running containers.
 *
 * @since 2.3
 *
 */
public class ContainerConfiguration {

    private String containerName;
    private String containerImage;
    private String containerImageTag;
    private List<Integer> containerPortsExternal;
    private List<Integer> containerPortsInternal;
    private List<String> containerEnvVars;
    private List<String> containerDevices;
    private Map<String, String> containerVolumes;
    private Boolean containerPrivileged;
    private Boolean isFrameworkManaged = true;
    private Map<String, String> containerLoggerParameters;
    private String containerLoggingType;

    /**
     * The method will provide information if the container is or not managed by the framework
     *
     * @return <code>true</code> if the framework manages the container. <code>false</code> otherwise
     */
    public boolean isFrameworkManaged() {
        return isFrameworkManaged;
    }

    /**
     * Returns the container name
     *
     * @return
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Returns the base image for the associated container
     *
     * @return
     */
    public String getContainerImage() {
        return containerImage;
    }

    /**
     * Returns the image tag for the associated container
     *
     * @return
     */
    public String getContainerImageTag() {
        return containerImageTag;
    }

    /**
     * Returns the list of external ports that will be mapped to the given container
     *
     * @return
     */
    public List<Integer> getContainerPortsExternal() {
        return containerPortsExternal;
    }

    /**
     * Returns the list of internal ports that will be mapped to the given container
     *
     * @return
     */
    public List<Integer> getContainerPortsInternal() {
        return containerPortsInternal;
    }

    /**
     * Returns the list of environment properties that will be passed to the container
     *
     * @return
     */
    public List<String> getContainerEnvVars() {
        return containerEnvVars;
    }

    /**
     * Returns the list of devices that will be mapped to the container
     *
     * @return
     */
    public List<String> getContainerDevices() {
        return containerDevices;
    }

    /**
     * Returns a map that identifies the volumes and the internal container mapping
     *
     * @return
     */
    public Map<String, String> getContainerVolumes() {
        return containerVolumes;
    }

    /**
     * Returns a boolean representing if the container runs or will run in privileged mode.
     *
     * @return
     */
    public boolean getContainerPrivileged() {
        return containerPrivileged;
    }
    /**
     * Returns a Map that identifies configured logger parameters.
     * @return
     */
    public Map<String, String> getLoggerParameters() {
        return this.containerLoggerParameters;
    }
    
    /**
     * returns a string identifying which logger driver to use.
     * @return
     */
    public String getContainerLoggingType() {
        return this.containerLoggingType;
    }

    /**
     * Static method that will extract the {@link ContainerConfiguration} matching the given name in the given list
     *
     * @param name
     * @param serviceList
     * @return
     */
    public static ContainerConfiguration findByName(String name, List<ContainerConfiguration> containerDescriptors) {
        ContainerConfiguration sd = null;
        for (ContainerConfiguration container : containerDescriptors) {
            if (container.getContainerName().equals(name)) {
                return container;
            }
        }
        return sd;
    }

    public static int compare(ContainerConfiguration obj1, ContainerConfiguration obj2) {
        return obj1.getContainerImage().compareTo(obj2.getContainerImage());
    }

    public static boolean equals(ContainerConfiguration obj1, ContainerConfiguration obj2) {
        boolean resultBuilder;

        resultBuilder = obj1.getContainerName().equals(obj2.getContainerName());
        resultBuilder = resultBuilder && obj1.getContainerImage().equals(obj2.getContainerImage());
        resultBuilder = resultBuilder && obj1.getContainerImageTag().equals(obj2.getContainerImageTag());
        resultBuilder = resultBuilder && obj1.getContainerPortsExternal().equals(obj2.getContainerPortsExternal());
        resultBuilder = resultBuilder && obj1.getContainerPortsInternal().equals(obj2.getContainerPortsInternal());
        resultBuilder = resultBuilder && obj1.getContainerEnvVars().equals(obj2.getContainerEnvVars());
        resultBuilder = resultBuilder && obj1.getContainerDevices().equals(obj2.getContainerDevices());
        resultBuilder = resultBuilder && obj1.getContainerVolumes().equals(obj2.getContainerVolumes());
        resultBuilder = resultBuilder && obj1.getContainerPrivileged() == obj2.getContainerPrivileged();
        resultBuilder = resultBuilder && obj1.isFrameworkManaged() == obj2.isFrameworkManaged();
        resultBuilder = resultBuilder && obj1.containerLoggerParameters.equals(obj2.containerLoggerParameters);
        resultBuilder = resultBuilder && obj1.containerLoggingType.equals(obj2.containerLoggingType);

        return resultBuilder;
    }

    /**
     * Creates a builder for creating a new {@link ContainerDescriptor} instance.
     *
     * @return the builder.
     */
    public static ContainerConfigurationBuilder builder() {
        return new ContainerConfigurationBuilder();
    }

    public static final class ContainerConfigurationBuilder {

        private String containerName;
        private String containerImage;
        private String containerImageTag = "latest";
        private List<Integer> containerPortsExternal = new ArrayList<>();
        private List<Integer> containerPortsInternal = new ArrayList<>();
        private List<String> containerEnvVars = new LinkedList<>();
        private List<String> containerDevices = new LinkedList<>();
        private Map<String, String> containerVolumes = new HashMap<>();
        private Boolean containerPrivilaged = false;
        private Boolean isFrameworkManaged = true;
        private Map<String, String> containerLoggerParameters;
        private String containerLoggingType;

        public ContainerConfigurationBuilder setContainerName(String serviceName) {
            this.containerName = serviceName;
            return this;
        }

        public ContainerConfigurationBuilder setFrameworkManaged(Boolean isFrameworkManaged) {
            this.isFrameworkManaged = isFrameworkManaged;
            return this;
        }

        public ContainerConfigurationBuilder setContainerImage(String serviceImage) {
            this.containerImage = serviceImage;
            return this;
        }

        public ContainerConfigurationBuilder setContainerImageTag(String serviceImageTag) {
            this.containerImageTag = serviceImageTag;
            return this;
        }

        public ContainerConfigurationBuilder setExternalPorts(List<Integer> containerPortsExternal) {
            this.containerPortsExternal = new ArrayList<>(containerPortsExternal);
            return this;
        }

        public ContainerConfigurationBuilder setInternalPorts(List<Integer> containerPortsInternal) {
            this.containerPortsInternal = new ArrayList<>(containerPortsInternal);
            return this;
        }

        public ContainerConfigurationBuilder setEnvVars(List<String> vars) {
            this.containerEnvVars = new LinkedList<>(vars);
            return this;
        }

        public ContainerConfigurationBuilder setDeviceList(List<String> devices) {
            this.containerDevices = new LinkedList<>(devices);
            return this;
        }

        public ContainerConfigurationBuilder setVolumes(Map<String, String> volumeMap) {
            this.containerVolumes = new HashMap<>(volumeMap);
            return this;
        }
        
        public ContainerConfigurationBuilder setLoggerParameters(Map<String, String> paramMap) {
            this.containerLoggerParameters = new HashMap<>(paramMap);
            return this;
        }

        public ContainerConfigurationBuilder setLoggingTypeByString(String containerLoggingType) {
            this.containerLoggingType = containerLoggingType;
            return this;
        }

        public ContainerConfigurationBuilder setPrivilegedMode(Boolean containerPrivilaged) {
            this.containerPrivilaged = containerPrivilaged;
            return this;
        }

        public ContainerConfiguration build() {
            ContainerConfiguration result = new ContainerConfiguration();

            result.containerName = requireNonNull(this.containerName, "Request Container Name cannot be null");
            result.containerImage = requireNonNull(this.containerImage, "Request Container Image cannot be null");
            result.containerImageTag = this.containerImageTag;
            result.containerPortsExternal = this.containerPortsExternal;
            result.containerPortsInternal = this.containerPortsInternal;
            result.containerEnvVars = this.containerEnvVars;
            result.containerDevices = this.containerDevices;
            result.containerVolumes = this.containerVolumes;
            result.containerPrivileged = this.containerPrivilaged;
            result.isFrameworkManaged = this.isFrameworkManaged;
            result.containerLoggerParameters = this.containerLoggerParameters;
            result.containerLoggingType = this.containerLoggingType;

            return result;
        }

    }
}