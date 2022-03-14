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
import java.util.Map;

import org.eclipse.kura.container.orchestration.ContainerDescriptor;
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
    private List<String> containerEnvVars;
    private List<String> containerDevices;
    private Map<String, String> containerVolumes;
    private Boolean containerPrivilaged;
    private ContainerState containerState;

    private Boolean isFrameworkManaged;

    public DockerContainer(String name, String version) {
        super(name, version, SystemResourceType.DOCKER);
        this.containerName = name;
    }

    public DockerContainer(ContainerDescriptor container) {
        super(container.getContainerName(), container.getContainerImage() + ":" + container.getContainerImageTag(),
                SystemResourceType.DOCKER);

        this.containerName = container.getContainerName();
        this.containerImage = container.getContainerImage();
        this.containerImageTag = container.getContainerImageTag();
        this.containerID = container.getContainerId();
        this.containerPortsExternal = container.getContainerPortsExternal();
        this.containerPortsInternal = container.getContainerPortsInternal();
        this.containerEnvVars = container.getContainerEnvVars();
        this.containerDevices = container.getContainerDevices();
        this.containerVolumes = container.getContainerVolumes();
        this.containerPrivilaged = container.getContainerPrivileged();
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

    public ContainerState getContainerState() {
        return this.containerState;
    }

    public void setContainerState(ContainerState containerState) {
        this.containerState = containerState;
    }

    public Boolean isFrameworkManaged() {
        return this.isFrameworkManaged;
    }

}
