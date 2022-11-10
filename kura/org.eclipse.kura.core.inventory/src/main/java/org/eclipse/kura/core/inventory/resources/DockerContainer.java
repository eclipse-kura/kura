/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https:www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.inventory.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerPort;
import org.eclipse.kura.container.orchestration.ContainerState;
import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;

public class DockerContainer extends SystemResourceInfo {

    private String containerName;
    private String containerImage;
    private String containerImageTag;
    private String containerID;
    private List<Integer> containerPortsExternal;
    private List<Integer> containerPortsInternal;
    private ContainerState containerState;

    private Boolean isFrameworkManaged;

    public DockerContainer(String name, String version) {
        super(name, version, SystemResourceType.DOCKER);
        this.containerName = name;
    }

    public DockerContainer(ContainerInstanceDescriptor container) {
        super(container.getContainerName(), container.getContainerImage() + ":" + container.getContainerImageTag(),
                SystemResourceType.DOCKER);

        this.containerName = container.getContainerName();
        this.containerImage = container.getContainerImage();
        this.containerImageTag = container.getContainerImageTag();

        this.containerID = container.getContainerId();
        this.containerPortsExternal = container.getContainerPorts().stream().map(ContainerPort::getExternalPort)
                .collect(Collectors.toList());

        this.containerPortsInternal = container.getContainerPorts().stream().map(ContainerPort::getInternalPort)
                .collect(Collectors.toList());
        this.containerState = container.getContainerState();
        this.isFrameworkManaged = container.isFrameworkManaged();

    }

    public String getContainerName() {
        return this.containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerImage() {
        return this.containerImage;
    }

    public void setContainerImage(String containerImage) {
        this.containerImage = containerImage;
    }

    public String getContainerImageTag() {
        return this.containerImageTag;
    }

    public void setContainerImageTag(String containerImageTag) {
        this.containerImageTag = containerImageTag;
    }

    public String getContainerId() {
        return this.containerID;
    }

    public void setContainerId(String id) {
        this.containerID = id;
    }

    public List<Integer> getContainerPortsExternal() {
        return this.containerPortsExternal;
    }

    public List<Integer> getContainerPortsInternal() {
        return this.containerPortsInternal;
    }

    public ContainerState getContainerState() {
        return this.containerState;
    }

    public String getFrameworkContainerState() {

        switch (containerState) {
            case STARTING:
                return "installed";
            case ACTIVE:
                return "active";
            case FAILED:
            case STOPPING:
                return "uninstalled";
            default:
                return "unknown";
        }
    }

    public void setContainerState(ContainerState containerState) {
        this.containerState = containerState;
    }

    public Boolean isFrameworkManaged() {
        return this.isFrameworkManaged;
    }

}
