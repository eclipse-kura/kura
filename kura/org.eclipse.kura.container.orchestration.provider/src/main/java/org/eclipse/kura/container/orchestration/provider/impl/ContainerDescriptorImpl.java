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

package org.eclipse.kura.container.orchestration.provider.impl;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.container.orchestration.ContainerDescriptor;
import org.eclipse.kura.container.orchestration.ContainerStates;

/**
 * Object which represents a container. Used to track running containers.
 *
 */
public class ContainerDescriptorImpl implements ContainerDescriptor {

    private String containerName;
    private String containerImage;
    private String containerImageTag;
    private String containerID;
    private List<Integer> containerPortsExternal;
    private List<Integer> containerPortsInternal;
    private List<String> containerEnvVars;
    private List<String> containerDevices;
    private Map<String, String> containerVolumes;
    private Boolean containerPrivileged;
    private ContainerStates containerState = ContainerStates.STOPPING;
    private Boolean isFrameworkManaged = true;

    @Override
    public ContainerStates getContainerState() {
        return this.containerState;
    }

    @Override
    public Boolean isFrameworkManaged() {
        return this.isFrameworkManaged;
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getContainerImage() {
        return this.containerImage;
    }

    @Override
    public String getContainerImageTag() {
        return this.containerImageTag;
    }

    @Override
    public String getContainerId() {
        return this.containerID;
    }

    @Override
    public List<Integer> getContainerPortsExternal() {
        return this.containerPortsExternal;
    }

    @Override
    public List<Integer> getContainerPortsInternal() {
        return this.containerPortsInternal;
    }

    @Override
    public List<String> getContainerEnvVars() {
        return this.containerEnvVars;
    }

    @Override
    public List<String> getContainerDevices() {
        return this.containerDevices;
    }

    @Override
    public Map<String, String> getContainerVolumes() {
        return this.containerVolumes;
    }

    @Override
    public Boolean getContainerPrivileged() {
        return this.containerPrivileged;
    }

    @Override
    public void setContainerId(String id) {
        this.containerID = id;
    }

    /**
     * Creates a builder for creating a new {@link ContainerDescriptor} instance.
     *
     * @return the builder.
     */
    public static ContainerDescriptorBuilder builder() {
        return new ContainerDescriptorBuilder();
    }

    public static final class ContainerDescriptorBuilder {

        private String containerName;
        private String containerImage;
        private String containerImageTag = "latest";
        private String containerID = "";
        private List<Integer> containerPortsExternal = new ArrayList<>();
        private List<Integer> containerPortsInternal = new ArrayList<>();
        private List<String> containerEnvVars = new LinkedList<>();
        private List<String> containerDevices = new LinkedList<>();
        private Map<String, String> containerVolumes = new HashMap<>();
        private Boolean containerPrivilaged = false;
        private ContainerStates containerState = ContainerStates.STOPPING;
        private Boolean isFrameworkManaged = true;

        public ContainerDescriptorBuilder setContainerName(String serviceName) {
            this.containerName = serviceName;
            return this;
        }

        public ContainerDescriptorBuilder setFrameworkManaged(Boolean isFrameworkManaged) {
            this.isFrameworkManaged = isFrameworkManaged;
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

        public ContainerDescriptorBuilder setExternalPort(List<Integer> containerPortsExternal) {
            this.containerPortsExternal = new ArrayList<>(containerPortsExternal);
            return this;
        }

        public ContainerDescriptorBuilder setInternalPort(List<Integer> containerPortsInternal) {
            this.containerPortsInternal = new ArrayList<>(containerPortsInternal);
            return this;
        }

        public ContainerDescriptorBuilder addExternalPort(int port) {
            this.containerPortsExternal.add(port);
            return this;
        }

        public ContainerDescriptorBuilder addInternalPort(int port) {
            this.containerPortsInternal.add(port);
            return this;
        }

        public ContainerDescriptorBuilder addExternalPorts(List<Integer> containerPortsExternal) {
            this.containerPortsExternal.addAll(containerPortsExternal);
            return this;
        }

        public ContainerDescriptorBuilder addInternalPorts(List<Integer> containerPortsInternal) {
            this.containerPortsInternal.addAll(containerPortsInternal);
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
            ContainerDescriptorImpl containerDescriptor = new ContainerDescriptorImpl();

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
            containerDescriptor.containerPrivileged = this.containerPrivilaged;
            containerDescriptor.containerState = this.containerState;
            containerDescriptor.isFrameworkManaged = this.isFrameworkManaged;

            return containerDescriptor;
        }

    }

}