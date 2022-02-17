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

package org.eclipse.kura.container.orchestration.provider;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Object which represents a container. Used to track running containers.
 *
 */
public class ContainerDescriptor {

    private String containerName;
    private String containerImage;
    private String containerImageTag;
    private String containerID;
    private int[] containerPortsExternal;
    private int[] containerPortsInternal;
    private List<String> containerEnvVars;
    private List<String> containerDevices;
    private Map<String, String> containerVolumes;
    private Boolean containerPrivilaged;
    private ContainerStates containerState = ContainerStates.STOPPING;
    private Boolean isEsfManaged = true;

    public static ContainerDescriptor findByName(String name, List<ContainerDescriptor> serviceList) {
        ContainerDescriptor sd = null;
        for (ContainerDescriptor container : serviceList) {
            if (container.containerName.equals(name)) {
                return container;
            }
        }
        return sd;
    }

    public ContainerStates getContainerState() {
        return this.containerState;
    }

    public void setContainerState(ContainerStates containerState) {
        this.containerState = containerState;
    }

    public Boolean getIsEsfManaged() {
        return this.isEsfManaged;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public String getContainerImage() {
        return this.containerImage;
    }

    public String getContainerImageTag() {
        return this.containerImageTag;
    }

    public String getContainerId() {
        return this.containerID;
    }

    public void setContainerId(String id) {
        this.containerID = id;
    }

    public int[] getContainerPortsExternal() {
        return this.containerPortsExternal;
    }

    public int[] getContainerPortsInternal() {
        return this.containerPortsInternal;
    }

    public List<String> getContainerEnvVars() {
        return this.containerEnvVars;
    }

    public List<String> getContainerDevices() {
        return this.containerDevices;
    }

    public Map<String, String> getContainerVolumes() {
        return this.containerVolumes;
    }

    public Boolean getContainerPrivileged() {
        return this.containerPrivilaged;
    }

    /**
     * Creates a builder for creating a new {@link ContainerDescriptor} instance.
     *
     * @return the builder.
     */
    public static ContainerDescriptorBuilder builder() {
        return new ContainerDescriptorBuilder();
    }

    public static int compare(ContainerDescriptor obj1, ContainerDescriptor obj2) {
        return obj1.containerImage.compareTo(obj2.containerImage);
    }

    public static boolean equals(ContainerDescriptor obj1, ContainerDescriptor obj2) {
        boolean resultBuilder;

        resultBuilder = obj1.containerName.equals(obj2.containerName);
        resultBuilder = resultBuilder && obj1.containerImage.equals(obj2.containerImage);
        resultBuilder = resultBuilder && obj1.containerImageTag.equals(obj2.containerImageTag);
        resultBuilder = resultBuilder && ArrayUtils.isEquals(obj1.containerPortsExternal, obj2.containerPortsExternal);
        resultBuilder = resultBuilder && ArrayUtils.isEquals(obj1.containerPortsInternal, obj2.containerPortsInternal);
        resultBuilder = resultBuilder && obj1.containerEnvVars.equals(obj2.containerEnvVars);
        resultBuilder = resultBuilder && obj1.containerDevices.equals(obj2.containerDevices);
        resultBuilder = resultBuilder && obj1.containerVolumes.equals(obj2.containerVolumes);
        resultBuilder = resultBuilder && obj1.containerPrivilaged.equals(obj2.containerPrivilaged);
        resultBuilder = resultBuilder && obj1.containerState.equals(obj2.containerState);
        resultBuilder = resultBuilder && obj1.isEsfManaged.equals(obj2.isEsfManaged);

        return resultBuilder;
    }

    public static final class ContainerDescriptorBuilder {

        private String containerName;
        private String containerImage;
        private String containerImageTag = "latest";
        private String containerID = "";
        private int[] containerPortsExternal = new int[] {};
        private int[] containerPortsInternal = new int[] {};
        private List<String> containerEnvVars = new LinkedList<>();
        private List<String> containerDevices = new LinkedList<>();
        private Map<String, String> containerVolumes = new HashMap<>();
        private Boolean containerPrivilaged = false;
        private ContainerStates containerState = ContainerStates.STOPPING;
        private Boolean isEsfManaged = true;

        public ContainerDescriptorBuilder setContainerName(String serviceName) {
            this.containerName = serviceName;
            return this;
        }

        public ContainerDescriptorBuilder setIsEsfManaged(Boolean isEsfManaged) {
            this.isEsfManaged = isEsfManaged;
            return this;
        }

        public ContainerDescriptorBuilder setContainerImage(String serviceImage) {
            this.containerImage = serviceImage;
            return this;
        }

        public ContainerDescriptorBuilder setContainerImageTag(String serviceImageTag) {
            this.containerImageTag = serviceImageTag;
            return this;
        }

        public ContainerDescriptorBuilder setContainerID(String containerID) {
            this.containerID = containerID;
            return this;
        }

        public ContainerDescriptorBuilder setExternalPort(int[] containerPortsExternal) {
            this.containerPortsExternal = containerPortsExternal.clone();
            return this;
        }

        public ContainerDescriptorBuilder setInternalPort(int[] containerPortsInternal) {
            this.containerPortsInternal = containerPortsInternal.clone();
            return this;
        }

        public ContainerDescriptorBuilder addExternalPort(int port) {
            if (this.containerPortsExternal == null) {
                this.containerPortsExternal = new int[] { port };
            } else {
                this.containerPortsExternal = ArrayUtils.add(this.containerPortsExternal, port);
            }
            return this;
        }

        public ContainerDescriptorBuilder addInternalPort(int port) {
            if (this.containerPortsInternal == null) {
                this.containerPortsInternal = new int[] { port };
            } else {
                this.containerPortsInternal = ArrayUtils.add(this.containerPortsInternal, port);
            }
            return this;
        }

        public ContainerDescriptorBuilder addExternalPorts(int[] containerPortsExternal) {
            if (this.containerPortsExternal == null) {
                this.containerPortsExternal = containerPortsExternal.clone();
            } else {
                this.containerPortsExternal = ArrayUtils.addAll(this.containerPortsExternal, containerPortsExternal);
            }
            return this;
        }

        public ContainerDescriptorBuilder addInternalPorts(int[] containerPortsInternal) {
            if (this.containerPortsInternal == null) {
                this.containerPortsInternal = containerPortsInternal.clone();
            } else {
                this.containerPortsInternal = ArrayUtils.addAll(this.containerPortsInternal, containerPortsInternal);
            }
            return this;
        }

        public ContainerDescriptorBuilder addEnvVar(String envVar) {
            this.containerEnvVars.add(envVar);
            return this;
        }

        public ContainerDescriptorBuilder addEnvVar(List<String> vars) {
            this.containerEnvVars.addAll(vars);
            return this;
        }

        public ContainerDescriptorBuilder setEnvVar(List<String> vars) {
            this.containerEnvVars = new LinkedList<>(vars);
            return this;
        }

        public ContainerDescriptorBuilder addDevice(String device) {
            this.containerDevices.add(device);
            return this;
        }

        public ContainerDescriptorBuilder addDevice(List<String> devices) {
            this.containerDevices.addAll(devices);
            return this;
        }

        public ContainerDescriptorBuilder setDeviceList(List<String> devices) {
            this.containerDevices = new LinkedList<>(devices);
            return this;
        }

        /**
         * source: path on host (key)
         * destination: path in container (value)
         *
         * @param source
         * @param destination
         * @return
         */
        public ContainerDescriptorBuilder addVolume(String source, String destination) {
            this.containerVolumes.put(source, destination);
            return this;
        }

        /**
         * source: path on host (key)
         * destination: path in container (value)
         *
         * @param volumeMap
         * @return
         */
        public ContainerDescriptorBuilder addVolume(Map<String, String> volumeMap) {
            this.containerVolumes.putAll(volumeMap);
            return this;
        }

        /**
         * source: path on host (key)
         * destination: path in container (value)
         *
         * @param volumeMap
         * @return
         */
        public ContainerDescriptorBuilder setVolume(Map<String, String> volumeMap) {
            this.containerVolumes = new HashMap<>(volumeMap);
            return this;
        }

        public ContainerDescriptorBuilder setPrivilegedMode(Boolean containerPrivilaged) {
            this.containerPrivilaged = containerPrivilaged;
            return this;
        }

        public ContainerDescriptorBuilder setContainerState(ContainerStates containerState) {
            this.containerState = containerState;
            return this;
        }

        public ContainerDescriptor build() {
            ContainerDescriptor containerDescriptor = new ContainerDescriptor();

            containerDescriptor.containerName = requireNonNull(this.containerName,
                    "Request Container Name cannot be null");
            containerDescriptor.containerImage = requireNonNull(this.containerImage,
                    "Request Container Image cannot be null");
            containerDescriptor.containerImageTag = this.containerImageTag;
            containerDescriptor.containerID = this.containerID;
            containerDescriptor.containerPortsExternal = this.containerPortsExternal;
            containerDescriptor.containerPortsInternal = this.containerPortsInternal;
            containerDescriptor.containerEnvVars = this.containerEnvVars;
            containerDescriptor.containerDevices = this.containerDevices;
            containerDescriptor.containerVolumes = this.containerVolumes;
            containerDescriptor.containerPrivilaged = this.containerPrivilaged;
            containerDescriptor.containerState = this.containerState;
            containerDescriptor.isEsfManaged = this.isEsfManaged;

            return containerDescriptor;
        }

    }

}