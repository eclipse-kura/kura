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
import java.util.List;
import java.util.Objects;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Object which represents a container. Used to track created containers.
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
    private List<Integer> containerPortsExternal;
    private List<Integer> containerPortsInternal;
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
     * The method will provide information if the container is or not managed by the framework
     *
     * @return <code>true</code> if the framework manages the container. <code>false</code> otherwise
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
     * Returns the list of external ports that will be mapped to the given container
     *
     * @return
     */
    public List<Integer> getContainerPortsExternal() {
        return this.containerPortsExternal;
    }

    /**
     * Returns the list of internal ports that will be mapped to the given container
     *
     * @return
     */
    public List<Integer> getContainerPortsInternal() {
        return this.containerPortsInternal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.containerID, this.containerImage, this.containerImageTag, this.containerName,
                this.containerPortsExternal, this.containerPortsInternal, this.containerState, this.isFrameworkManaged);
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
                && Objects.equals(this.containerPortsExternal, other.containerPortsExternal)
                && Objects.equals(this.containerPortsInternal, other.containerPortsInternal)
                && this.containerState == other.containerState && this.isFrameworkManaged == other.isFrameworkManaged;
    }

    /**
     * Creates a builder for creating a new {@link ContainerInstanceDescriptor} instance.
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

        public ContainerInstanceDescriptorBuilder setExternalPorts(List<Integer> containerPortsExternal) {
            this.containerPortsExternal = new ArrayList<>(containerPortsExternal);
            return this;
        }

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

            containerDescriptor.containerName = this.containerName;
            containerDescriptor.containerImage = this.containerImage;
            containerDescriptor.containerImageTag = this.containerImageTag;
            containerDescriptor.containerID = this.containerId;
            containerDescriptor.containerPortsExternal = this.containerPortsExternal;
            containerDescriptor.containerPortsInternal = this.containerPortsInternal;
            containerDescriptor.containerState = this.containerState;
            containerDescriptor.isFrameworkManaged = this.isFrameworkManaged;

            return containerDescriptor;
        }

    }

}