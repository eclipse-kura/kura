/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;

import com.eurotech.framework.docker.ContainerDescriptor;
import com.eurotech.framework.docker.ContainerStates;

public class DockerContainer extends SystemResourceInfo {

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
    private ContainerStates containerState;

    private Boolean isEsfManaged;

    public DockerContainer(String name, String version) {
        super(name, version, SystemResourceType.DOCKER);
        this.containerName = name;
    }

    public DockerContainer(ContainerDescriptor container) {
        super(container.getContainerName(), container.getContainerImage() + ":" + container.getContainerImageTag(),
                SystemResourceType.DOCKER);

        containerName = container.getContainerName();
        containerImage = container.getContainerImage();
        containerImageTag = container.getContainerImageTag();
        containerID = container.getContainerId();
        containerPortsExternal = container.getContainerPortsExternal();
        containerPortsInternal = container.getContainerPortsInternal();
        containerEnvVars = container.getContainerEnvVars();
        containerDevices = container.getContainerDevices();
        containerVolumes = container.getContainerVolumes();
        containerPrivilaged = container.getContainerPrivileged();
        containerState = container.getContainerState();

        isEsfManaged = container.getIsEsfManaged();

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

    public ContainerStates getContainerState() {
        return this.containerState;
    }

    public void setContainerState(ContainerStates containerState) {
        this.containerState = containerState;
    }

    public Boolean getIsEsfManaged() {
        return this.isEsfManaged;
    }

}
