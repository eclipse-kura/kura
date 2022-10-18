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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Object which represents a instantiated container. Used to track created
 * containers.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 *
 */
@ProviderType
public class ContainerInstanceDescriptor {

    private String containerName;
    private String containerImage;
    private String containerImageTag;
    private String containerID;
    private List<ContainerPort> containerPorts = new ArrayList<>();
    private ContainerState containerState = ContainerState.STOPPING;
    private boolean isFrameworkManaged;

    private ContainerInstanceDescriptor() {
    }

    /**
     * Returns the container status as {@link ContainerState}
     *
     * @return
     */
    public ContainerState getContainerState() {
        return this.containerState;
    }

    /**
     * The method will provide information if the container is or not managed by the
     * framework
     *
     * @return <code>true</code> if the framework manages the container.
     *         <code>false</code> otherwise
     */
    public boolean isFrameworkManaged() {
        return this.isFrameworkManaged;
    }

    /**
     * Returns the container name
     *
     * @return
     */
    public String getContainerName() {
        return this.containerName;
    }

    /**
     * Returns the base image for the associated container
     *
     * @return
     */
    public String getContainerImage() {
        return this.containerImage;
    }

    /**
     * Returns the image tag for the associated container
     *
     * @return
     */
    public String getContainerImageTag() {
        return this.containerImageTag;
    }

    /**
     * Returns the containerID
     *
     * @return
     */
    public String getContainerId() {
        return this.containerID;
    }

    /**
     * Returns a list of {@link ContainerPort} mapped to the container.
     *
     * @return
     * @since 2.5
     */
    public List<ContainerPort> getContainerPorts() {
        return this.containerPorts;
    }

    /**
     * Returns the list of external ports that will be mapped to the
     * given container
     *
     * @return
     *
     * @deprecated please use {@link getContainerPorts} as it includes the network
     *             protocol with the port mapping.
     */
    @Deprecated
    public List<Integer> getContainerPortsExternal() {
        return this.containerPorts.stream().map(ContainerPort::getExternalPort).collect(Collectors.toList());
    }

    /**
     * Returns the list of internal ports that will be mapped to the
     * given container
     *
     * @return
     *
     * @deprecated please use {@link getContainerPorts} as it includes the network
     *             protocol with the port mapping.
     *
     */
    @Deprecated
    public List<Integer> getContainerPortsInternal() {
        return this.containerPorts.stream().map(ContainerPort::getInternalPort).collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.containerID, this.containerImage, this.containerImageTag, this.containerName,
                this.containerPorts, this.containerState, this.isFrameworkManaged);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ContainerInstanceDescriptor other = (ContainerInstanceDescriptor) obj;
        return Objects.equals(this.containerID, other.containerID)
                && Objects.equals(this.containerImage, other.containerImage)
                && Objects.equals(this.containerImageTag, other.containerImageTag)
                && Objects.equals(this.containerName, other.containerName)
                && Objects.equals(this.containerPorts, other.containerPorts)
                && this.containerState == other.containerState && this.isFrameworkManaged == other.isFrameworkManaged;
    }

    /**
     * Creates a builder for creating a new {@link ContainerInstanceDescriptor}
     * instance.
     *
     * @return the builder.
     */
    public static ContainerInstanceDescriptorBuilder builder() {
        return new ContainerInstanceDescriptorBuilder();
    }

    public static final class ContainerInstanceDescriptorBuilder {

        private String containerName;
        private String containerImage;
        private String containerImageTag = "latest";
        private String containerId = "";
        private List<Integer> containerPortsExternal = new ArrayList<>();
        private List<Integer> containerPortsInternal = new ArrayList<>();
        private List<ContainerPort> containerPorts = new ArrayList<>();
        private ContainerState containerState = ContainerState.STOPPING;
        private boolean isFrameworkManaged;

        public ContainerInstanceDescriptorBuilder setContainerName(String serviceName) {
            this.containerName = serviceName;
            return this;
        }

        public ContainerInstanceDescriptorBuilder setFrameworkManaged(Boolean isFrameworkManaged) {
            this.isFrameworkManaged = isFrameworkManaged;
            return this;
        }

        public ContainerInstanceDescriptorBuilder setContainerImage(String serviceImage) {
            this.containerImage = serviceImage;
            return this;
        }

        public ContainerInstanceDescriptorBuilder setContainerImageTag(String serviceImageTag) {
            this.containerImageTag = serviceImageTag;
            return this;
        }

        public ContainerInstanceDescriptorBuilder setContainerID(String containerID) {
            this.containerId = containerID;
            return this;
        }

        /**
         *
         * Set a list of container ports, to express which ports to expose and what
         * protocol to use.
         *
         * @since 2.5
         */
        public ContainerInstanceDescriptorBuilder setContainerPorts(List<ContainerPort> containerPorts) {
            this.containerPorts = containerPorts;
            return this;
        }

        /**
         * Accepts a list<Integer> of ports to be exposed. Assumes all ports
         * are TCP. To use other Internet protocols
         * please see the {@link setContainerPorts} method. Ensure that the
         * number of elements in this list is the same
         * as the number of elements set with {@link setInternalPorts}.
         *
         * @deprecated please use {@link setContainerPorts} as it allows for network
         *             protocol to be specified in a port mapping.
         *
         */
        @Deprecated
        public ContainerInstanceDescriptorBuilder setExternalPorts(List<Integer> containerPortsExternal) {
            this.containerPortsExternal = new ArrayList<>(containerPortsExternal);
            return this;
        }

        /**
         * Accepts a list<Integer> of ports to be open internally within the
         * container.
         * Assumes all ports are TCP. To
         * use other Internet protocols please see the
         * {@link setContainerPorts} method.
         * Ensure that the number of elements in this list is the same as
         * the number of elements set with {@link setExternalPorts}.
         *
         * @deprecated please use {@link setContainerPorts} as it allows for network
         *             protocol to be specified in a port mapping.
         *
         */
        @Deprecated
        public ContainerInstanceDescriptorBuilder setInternalPorts(List<Integer> containerPortsInternal) {
            this.containerPortsInternal = new ArrayList<>(containerPortsInternal);
            return this;
        }

        public ContainerInstanceDescriptorBuilder setContainerState(ContainerState containerState) {
            this.containerState = containerState;
            return this;
        }

        public ContainerInstanceDescriptor build() {
            ContainerInstanceDescriptor containerDescriptor = new ContainerInstanceDescriptor();

            if (this.containerPorts.isEmpty()) {
                Iterator<Integer> extPort = this.containerPortsExternal.iterator();
                Iterator<Integer> intPort = this.containerPortsInternal.iterator();

                while (extPort.hasNext() && intPort.hasNext()) {
                    this.containerPorts.add(new ContainerPort(intPort.next(), extPort.next()));
                }
            }

            containerDescriptor.containerName = this.containerName;
            containerDescriptor.containerImage = this.containerImage;
            containerDescriptor.containerImageTag = this.containerImageTag;
            containerDescriptor.containerID = this.containerId;
            containerDescriptor.containerPorts = this.containerPorts;
            containerDescriptor.containerState = this.containerState;
            containerDescriptor.isFrameworkManaged = this.isFrameworkManaged;

            return containerDescriptor;
        }

    }

}