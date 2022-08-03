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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerNetworkConfiguration;
import org.eclipse.kura.container.orchestration.ImageConfiguration;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.container.orchestration.RegistryCredentials;
import org.eclipse.kura.util.configuration.Property;

public class ContainerInstanceOptions {

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
    private static final Property<Integer> CONTAINER_IMAGE_DOWNLOAD_RETRIES = new Property<>(
            "container.image.download.retries", 5);
    private static final Property<Integer> CONTAINER_IMAGE_DOWNLOAD_RETRY_INTERVAL = new Property<>(
            "container.image.download.interval", 30000);
    private static final Property<String> CONTAINER_LOGGER_PARAMETERS = new Property<>("container.loggerParameters",
            "");
    private static final Property<String> CONTAINER_LOGGING_TYPE = new Property<>("container.loggingType", "default");
    private static final Property<String> REGISTRY_URL = new Property<>("registry.hostname", "");
    private static final Property<String> REGISTRY_USERNAME = new Property<>("registry.username", "");
    private static final Property<String> REGISTRY_PASSWORD = new Property<>("registry.password", "");
    private static final Property<Integer> IMAGES_DOWNLOAD_TIMEOUT = new Property<>("container.image.download.timeout",
            500);
    private static final Property<String> CONTAINER_NETWORKING_MODE = new Property<>("container.networkMode", "");
    private static final Property<String> CONTAINER_ENTRY_POINT = new Property<>("container.entrypoint", "");
    private static final Property<Boolean> CONTAINER_RESTART_FAILURE = new Property<>("container.restart.onfailure", false);

    private final boolean enabled;
    private final String image;
    private final String imageTag;
    private final String containerName;
    private final List<Integer> internalPorts;
    private final List<Integer> externalPorts;
    private final String containerEnv;
    private final String containerVolumeString;
    private final String containerDevice;
    private final boolean privilegedMode;
    private final Map<String, String> containerVolumes;
    private final int maxDownloadRetries;
    private final int retryInterval;
    private final Map<String, String> containerLoggingParameters;
    private final String containerLoggerType;
    private final Optional<String> registryURL;
    private final Optional<String> registryUsername;
    private final Optional<String> registryPassword;
    private final int imageDownloadTimeout;
    private final Optional<String> containerNetworkingMode;
    private final List<String> containerEntryPoint;
    private final boolean restartOnFailure;

    public ContainerInstanceOptions(final Map<String, Object> properties) {
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
        this.maxDownloadRetries = CONTAINER_IMAGE_DOWNLOAD_RETRIES.get(properties);
        this.retryInterval = CONTAINER_IMAGE_DOWNLOAD_RETRY_INTERVAL.get(properties);
        this.containerLoggerType = CONTAINER_LOGGING_TYPE.get(properties);
        this.containerLoggingParameters = parseLoggingParams(CONTAINER_LOGGER_PARAMETERS.get(properties));
        this.registryURL = REGISTRY_URL.getOptional(properties);
        this.registryUsername = REGISTRY_USERNAME.getOptional(properties);
        this.registryPassword = REGISTRY_PASSWORD.getOptional(properties);
        this.imageDownloadTimeout = IMAGES_DOWNLOAD_TIMEOUT.get(properties);
        this.containerNetworkingMode = CONTAINER_NETWORKING_MODE.getOptional(properties);
        this.containerEntryPoint = parseStringListSplitByComma(CONTAINER_ENTRY_POINT.get(properties));
        this.restartOnFailure = CONTAINER_RESTART_FAILURE.get(properties);
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

    private Map<String, String> parseLoggingParams(String loggingString) {
        Map<String, String> map = new HashMap<>();

        if (loggingString.isEmpty()) {
            return map;
        }

        for (String entry : loggingString.trim().split(",")) {
            String[] tempEntry = entry.split("=");
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

    private List<String> parseStringListSplitByComma(String stringToSplit) {

        List<String> stringList = new LinkedList<>();

        if (stringToSplit.isEmpty()) {
            return stringList;
        }

        for (String entry : stringToSplit.trim().split(",")) {
            if (entry.trim().length() > 0) {
                stringList.add(entry.trim());
            }
        }

        return stringList;
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

    public List<Integer> getContainerPortsInternal() {
        return this.internalPorts;
    }

    public List<Integer> getContainerPortsExternal() {
        return this.externalPorts;
    }

    public Map<String, String> getContainerVolumeList() {
        return parseVolume(this.containerVolumeString);
    }

    public List<String> getContainerEnvList() {
        return parseEnvVars(this.containerEnv);
    }

    public List<String> getContainerDeviceList() {
        return parseStringListSplitByComma(this.containerDevice);
    }

    public boolean getPrivilegedMode() {
        return this.privilegedMode;
    }

    public boolean isUnlimitedRetries() {
        return this.maxDownloadRetries == 0;
    }

    public int getMaxDownloadRetries() {
        return this.maxDownloadRetries;
    }

    public int getRetryInterval() {
        return this.retryInterval;
    }

    public String getLoggingType() {
        return this.containerLoggerType;
    }
    
    public boolean getRestartOnFailure() {
        return this.restartOnFailure;
    }

    public Map<String, String> getLoggerParameters() {
        return this.containerLoggingParameters;
    }

    public Optional<String> getContainerNetworkingMode() {
        return this.containerNetworkingMode;
    }

    public Optional<RegistryCredentials> getRegistryCredentials() {
        if (this.registryUsername.isPresent() && this.registryPassword.isPresent()) {
            return Optional.of(new PasswordRegistryCredentials(this.registryURL, this.registryUsername.get(),
                    new Password(this.registryPassword.get())));
        }

        return Optional.empty();
    }

    public int getImageDownloadTimeout() {
        return this.imageDownloadTimeout;
    }

    private ContainerNetworkConfiguration buildContainerNetworkConfig() {
        return new ContainerNetworkConfiguration.ContainerNetworkConfigurationBuilder()
                .setNetworkMode(getContainerNetworkingMode()).build();
    }

    public List<String> getEntryPoint() {
        return this.containerEntryPoint;
    }

    private ImageConfiguration buildImageConfig() {
        return new ImageConfiguration.ImageConfigurationBuilder().setImageName(image).setImageTag(imageTag)
                .setImageDownloadTimeoutSeconds(imageDownloadTimeout).setRegistryCredentials(getRegistryCredentials())
                .build();
    }

    public ContainerConfiguration getContainerConfiguration() {
        return ContainerConfiguration.builder().setContainerName(getContainerName())
                .setImageConfiguration(buildImageConfig()).setExternalPorts(getContainerPortsExternal())
                .setInternalPorts(getContainerPortsInternal()).setEnvVars(getContainerEnvList())
                .setVolumes(getContainerVolumeList()).setPrivilegedMode(this.privilegedMode)
                .setDeviceList(getContainerDeviceList()).setFrameworkManaged(true).setLoggingType(getLoggingType())
                .setContainerNetowrkConfiguration(buildContainerNetworkConfig())
                .setLoggerParameters(getLoggerParameters()).setEntryPoint(getEntryPoint()).setRestartOnFailure(getRestartOnFailure()).build();
    }

    private List<Integer> parsePortString(String ports) {
        List<Integer> tempArray = new ArrayList<>();
        if (!ports.isEmpty()) {
            String[] tempString = ports.trim().replace(" ", "").split(",");

            for (String element : tempString) {
                tempArray.add(Integer.parseInt(element.trim().replace("-", "")));
            }
        }

        return tempArray;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.containerDevice, this.containerEnv, this.containerLoggerType,
                this.containerLoggingParameters, this.containerName, this.containerVolumeString, this.containerVolumes,
                this.enabled, this.externalPorts, this.image, this.imageDownloadTimeout, this.imageTag,
                this.internalPorts, this.maxDownloadRetries, this.privilegedMode, this.registryPassword,
                this.registryURL, this.registryUsername, this.retryInterval, this.containerEntryPoint,
                this.containerNetworkingMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ContainerInstanceOptions)) {
            return false;
        }
        ContainerInstanceOptions other = (ContainerInstanceOptions) obj;
        return Objects.equals(this.containerDevice, other.containerDevice)
                && Objects.equals(this.containerEnv, other.containerEnv)
                && Objects.equals(this.containerLoggerType, other.containerLoggerType)
                && Objects.equals(this.containerLoggingParameters, other.containerLoggingParameters)
                && Objects.equals(this.containerName, other.containerName)
                && Objects.equals(this.containerVolumeString, other.containerVolumeString)
                && Objects.equals(this.containerVolumes, other.containerVolumes) && this.enabled == other.enabled
                && Objects.equals(this.externalPorts, other.externalPorts) && Objects.equals(this.image, other.image)
                && this.imageDownloadTimeout == other.imageDownloadTimeout
                && Objects.equals(this.imageTag, other.imageTag)
                && Objects.equals(this.internalPorts, other.internalPorts)
                && this.maxDownloadRetries == other.maxDownloadRetries && this.privilegedMode == other.privilegedMode
                && Objects.equals(this.registryPassword, other.registryPassword)
                && Objects.equals(this.registryURL, other.registryURL)
                && Objects.equals(this.registryUsername, other.registryUsername)
                && Objects.equals(this.containerNetworkingMode, other.containerNetworkingMode)
                && Objects.equals(this.containerEntryPoint, other.containerEntryPoint)
                && this.retryInterval == other.retryInterval;
    }

}