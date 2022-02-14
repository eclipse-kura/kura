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

package org.eclipse.kura.container.provider;

import static java.util.Objects.isNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.util.configuration.Property;

import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;

public class ConfigurableGenericDockerServiceOptions {

    private static final Property<Boolean> IS_ENABLED = new Property<>("container.enabled", false);
    private static final Property<String> CONTAINER_IMAGE = new Property<>("container.image", "nginx");
    private static final Property<String> CONTAINER_IMAGE_TAG = new Property<>("container.image.tag", "latest");
    private static final Property<String> CONTAINER_NAME = new Property<>("kura.service.pid", "kura_test_container");
    private static final Property<String> CONTAINER_PORTS_EXTERNAL = new Property<>("container.ports.external", "");
    private static final Property<String> CONTAINER_PORTS_INTERNAL = new Property<>("container.ports.internal", "");
    private static final Property<String> CONTAINER_ENV = new Property<>("container.env", "");
    private static final Property<String> CONTAINER_VOLUME = new Property<>("container.volume", "");
    private static final Property<String> CONTAINER_DEVICE = new Property<>("container.device", "");
    private static final Property<Boolean> CONTAINER_PRIVILEGED = new Property<>("container.privileged", false);

    private final boolean enabled;
    private final String image;
    private final String imageTag;
    private final String containerName;
    private final int[] internalPorts;
    private final int[] externalPorts;
    private final String containerEnv;
    private final String containerVolumeString;
    private final String containerDevice;
    private final boolean privilegedMode;
    private final Map<String, String> containerVolumes;

    public ConfigurableGenericDockerServiceOptions(final Map<String, Object> properties) {
        if (isNull(properties)) {
            throw new IllegalArgumentException("Properties cannot be null!");
        }

        this.enabled = IS_ENABLED.get(properties);
        this.image = CONTAINER_IMAGE.get(properties);
        this.imageTag = CONTAINER_IMAGE_TAG.get(properties);
        this.containerName = CONTAINER_NAME.get(properties);
        this.internalPorts = parsePortString(CONTAINER_PORTS_INTERNAL.get(properties));
        this.externalPorts = parsePortString(CONTAINER_PORTS_EXTERNAL.get(properties));
        this.containerEnv = CONTAINER_ENV.get(properties);
        this.containerVolumeString = CONTAINER_VOLUME.get(properties);
        this.containerVolumes = parseVolume(this.containerVolumeString);
        this.containerDevice = CONTAINER_DEVICE.get(properties);
        this.privilegedMode = CONTAINER_PRIVILEGED.get(properties);
    }

    private Map<String, String> parseVolume(String volumeString) {
        Map<String, String> map = new HashMap<>();

        if (this.containerVolumeString.isEmpty()) {
            return map;
        }

        for (String entry : volumeString.trim().split(",")) {
            String[] tempEntry = entry.split(":");
            if (tempEntry.length == 2) {
                map.put(tempEntry[0].trim(), tempEntry[1].trim());
            }
        }

        return map;
    }

    private List<String> parseEnvVars(String containerVolumeString) {
        List<String> envList = new LinkedList<>();

        if (containerVolumeString.isEmpty()) {
            return envList;
        }

        for (String entry : containerVolumeString.trim().split(",")) {
            envList.add(entry.trim());
        }

        return envList;
    }

    private List<String> parseDeviceStrings(String containerDevice) {

        List<String> deviceList = new LinkedList<>();

        if (containerDevice.isEmpty()) {
            return deviceList;
        }

        for (String entry : containerDevice.trim().split(",")) {
            deviceList.add(entry.trim());
        }

        return deviceList;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getContainerImage() {
        return this.image;
    }

    public String getContainerImageTag() {
        return this.imageTag;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public int[] getContainerPortsInternal() {
        return this.internalPorts;
    }

    public int[] getContainerPortsExternal() {
        return this.externalPorts;
    }

    public Map<String, String> getContainerVolumeList() {
        return parseVolume(this.containerVolumeString);
    }

    public List<String> getContainerEnvList() {
        return parseEnvVars(this.containerEnv);
    }

    public List<String> getContainerDeviceList() {
        return parseDeviceStrings(this.containerDevice);
    }

    public boolean getPrivilegedMode() {
        return this.privilegedMode;
    }

    public ContainerDescriptor getContainerDescriptor() {
        return ContainerDescriptor.builder().setContainerName(getContainerName()).setContainerImage(getContainerImage())
                .setContainerImageTag(getContainerImageTag()).setExternalPort(getContainerPortsExternal())
                .setInternalPort(getContainerPortsInternal()).addEnvVar(getContainerEnvList())
                .setVolume(getContainerVolumeList()).setPrivilegedMode(this.privilegedMode)
                .setDeviceList(getContainerDeviceList()).build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.externalPorts);
        result = prime * result + Arrays.hashCode(this.internalPorts);
        result = prime * result + Objects.hash(this.containerDevice, this.containerEnv, this.containerName,
                this.containerVolumes, this.enabled, this.image, this.imageTag);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConfigurableGenericDockerServiceOptions other = (ConfigurableGenericDockerServiceOptions) obj;
        return Objects.equals(this.containerDevice, other.containerDevice)
                && Objects.equals(this.containerEnv, other.containerEnv)
                && Objects.equals(this.containerName, other.containerName)
                && Objects.equals(this.containerVolumes, other.containerVolumes) && this.enabled == other.enabled
                && Arrays.equals(this.externalPorts, other.externalPorts) && Objects.equals(this.image, other.image)
                && Objects.equals(this.imageTag, other.imageTag)
                && Arrays.equals(this.internalPorts, other.internalPorts);
    }

    private int[] parsePortString(String ports) {
        int[] tempArray = new int[] {};
        if (ports.isEmpty()) {
            return tempArray;
        }

        String[] tempString = ports.trim().replace(" ", "").split(",");
        tempArray = new int[tempString.length];
        for (int i = 0; i < tempString.length; i++) {
            tempArray[i] = Integer.parseInt(tempString[i].trim().replace("-", ""));
        }

        return tempArray;
    }

}
