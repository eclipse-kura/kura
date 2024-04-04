/*******************************************************************************
  * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerConfiguration.ContainerConfigurationBuilder;
import org.eclipse.kura.container.orchestration.ContainerNetworkConfiguration;
import org.eclipse.kura.container.orchestration.ContainerPort;
import org.eclipse.kura.container.orchestration.ImageConfiguration;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.container.orchestration.PortInternetProtocol;
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
    private static final Property<Boolean> CONTAINER_RESTART_FAILURE = new Property<>("container.restart.onfailure",
            false);
    private static final Property<String> CONTAINER_MEMORY = new Property<>("container.memory", "");
    private static final Property<Float> CONTAINER_CPUS = new Property<>("container.cpus", 1F);
    private static final Property<String> CONTAINER_GPUS = new Property<>("container.gpus", "all");
    private static final Property<String> CONTAINER_RUNTIME = new Property<>("container.runtime", "");

    private static final Property<String> SIGNATURE_TRUST_ANCHOR = new Property<>("container.signature.trust.anchor",
            "");
    private static final Property<Boolean> SIGNATURE_VERIFY_TLOG = new Property<>(
            "container.signature.verify.transparency.log", true);
    private static final Property<String> ENFORCEMENT_DIGEST = new Property<>("enforcement.digest", "");

    private boolean enabled;
    private final String image;
    private final String imageTag;
    private final String containerName;
    private final List<Integer> internalPorts;
    private final List<Integer> externalPorts;
    private final List<PortInternetProtocol> containerPortProtocol;
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
    private final Optional<Long> containerMemory;
    private final Optional<Float> containerCpus;
    private final Optional<String> containerGpus;
    private final Optional<String> containerRuntime;

    private final Optional<String> signatureTrustAnchor;
    private final Boolean signatureVerifyTransparencyLog;

    private final Optional<String> enforcementDigest;

    public ContainerInstanceOptions(final Map<String, Object> properties) {
        if (isNull(properties)) {
            throw new IllegalArgumentException("Properties cannot be null!");
        }

        this.enabled = IS_ENABLED.get(properties);
        this.image = CONTAINER_IMAGE.get(properties);
        this.imageTag = CONTAINER_IMAGE_TAG.get(properties);
        this.containerName = CONTAINER_NAME.get(properties);
        this.internalPorts = parsePortString(CONTAINER_PORTS_INTERNAL.get(properties));
        this.containerPortProtocol = parsePortStringProtocol(CONTAINER_PORTS_INTERNAL.get(properties));
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
        this.containerMemory = parseMemoryString(CONTAINER_MEMORY.getOptional(properties));
        this.containerCpus = CONTAINER_CPUS.getOptional(properties);
        this.containerGpus = parseOptionalString(CONTAINER_GPUS.getOptional(properties));
        this.containerRuntime = parseOptionalString(CONTAINER_RUNTIME.getOptional(properties));
        this.signatureTrustAnchor = parseOptionalString(SIGNATURE_TRUST_ANCHOR.getOptional(properties));
        this.signatureVerifyTransparencyLog = SIGNATURE_VERIFY_TLOG.get(properties);
        this.enforcementDigest = parseOptionalString(ENFORCEMENT_DIGEST.getOptional(properties));
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

    private Optional<Long> parseMemoryString(Optional<String> value) {
        if (value.isPresent() && !value.get().trim().isEmpty()) {
            String stringValue = value.get().trim();
            long result = 0;
            switch (stringValue.charAt(stringValue.length() - 1)) {
            case 'b':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1));
                break;
            case 'k':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) * 1024L;
                break;
            case 'm':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) * 1048576L;
                break;
            case 'g':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) * 1073741824L;
                break;
            default:
                result = Long.parseLong(stringValue);
            }
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> parseOptionalString(Optional<String> optionalString) {
        if (optionalString.isPresent() && optionalString.get().isEmpty()) {
            return Optional.empty();
        } else {
            return optionalString;
        }
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

    public Optional<Long> getMemory() {
        return this.containerMemory;
    }

    public Optional<Float> getCpus() {
        return this.containerCpus;
    }

    public Optional<String> getGpus() {
        return this.containerGpus;
    }

    public Optional<String> getRuntime() {
        return this.containerRuntime;
    }

    public Optional<String> getSignatureTrustAnchor() {
        return this.signatureTrustAnchor;
    }

    public Boolean getSignatureVerifyTransparencyLog() {
        return this.signatureVerifyTransparencyLog;
    }

    public Optional<String> getEnforcementDigest() {
        return this.enforcementDigest;
    }

    private ImageConfiguration buildImageConfig() {
        return new ImageConfiguration.ImageConfigurationBuilder().setImageName(this.image).setImageTag(this.imageTag)
                .setImageDownloadTimeoutSeconds(this.imageDownloadTimeout)
                .setRegistryCredentials(getRegistryCredentials()).build();
    }

    private ContainerConfigurationBuilder buildPortConfig(ContainerConfigurationBuilder cc) {
        List<ContainerPort> containerPorts = new LinkedList<>();

        Iterator<Integer> internalIt = this.internalPorts.iterator();
        Iterator<Integer> externalIt = this.externalPorts.iterator();
        Iterator<PortInternetProtocol> ipIt = this.containerPortProtocol.iterator();

        while (externalIt.hasNext() && internalIt.hasNext() && ipIt.hasNext()) {
            containerPorts.add(new ContainerPort(internalIt.next(), externalIt.next(), ipIt.next()));
        }

        cc.setContainerPorts(containerPorts);

        return cc;
    }

    public ContainerConfiguration getContainerConfiguration() {
        return buildPortConfig(ContainerConfiguration.builder()).setContainerName(getContainerName())
                .setImageConfiguration(buildImageConfig()).setEnvVars(getContainerEnvList())
                .setVolumes(getContainerVolumeList()).setPrivilegedMode(this.privilegedMode)
                .setDeviceList(getContainerDeviceList()).setFrameworkManaged(true).setLoggingType(getLoggingType())
                .setContainerNetowrkConfiguration(buildContainerNetworkConfig())
                .setLoggerParameters(getLoggerParameters()).setEntryPoint(getEntryPoint())
                .setRestartOnFailure(getRestartOnFailure()).setMemory(getMemory()).setCpus(getCpus()).setGpus(getGpus())
                .setRuntime(getRuntime()).setEnforcementDigest(getEnforcementDigest()).build();
    }

    private List<Integer> parsePortString(String ports) {
        List<Integer> tempArray = new ArrayList<>();
        if (!ports.isEmpty()) {
            String[] tempString = ports.trim().replace(" ", "").split(",");

            for (String element : tempString) {
                tempArray.add(Integer.parseInt(element.trim().replace("-", "").split(":")[0]));
            }
        }

        return tempArray;
    }

    private List<PortInternetProtocol> parsePortStringProtocol(String ports) {
        List<PortInternetProtocol> tempArray = new ArrayList<>();
        if (!ports.isEmpty()) {
            for (String portToken : ports.trim().replace(" ", "").split(",")) {
                if (portToken.split(":").length > 1) {
                    switch (portToken.split(":")[1].toUpperCase().trim()) {
                    case "UDP":
                        tempArray.add(PortInternetProtocol.UDP);
                        break;
                    case "SCTP":
                        tempArray.add(PortInternetProtocol.SCTP);
                        break;
                    default:
                        tempArray.add(PortInternetProtocol.TCP);
                        break;
                    }
                } else {
                    // if setting is not used add TCP by default.
                    tempArray.add(PortInternetProtocol.TCP);
                }
            }
        }

        return tempArray;
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerCpus, containerDevice, containerEntryPoint, containerEnv, containerGpus,
                containerLoggerType, containerLoggingParameters, containerMemory, containerName,
                containerNetworkingMode, containerPortProtocol, containerRuntime, containerVolumeString,
                containerVolumes, enabled, enforcementDigest, externalPorts, image, imageDownloadTimeout, imageTag,
                internalPorts, maxDownloadRetries, privilegedMode, registryPassword, registryURL, registryUsername,
                restartOnFailure, retryInterval, signatureTrustAnchor, signatureVerifyTransparencyLog);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerInstanceOptions other = (ContainerInstanceOptions) obj;
        return Objects.equals(containerCpus, other.containerCpus)
                && Objects.equals(containerDevice, other.containerDevice)
                && Objects.equals(containerEntryPoint, other.containerEntryPoint)
                && Objects.equals(containerEnv, other.containerEnv)
                && Objects.equals(containerGpus, other.containerGpus)
                && Objects.equals(containerLoggerType, other.containerLoggerType)
                && Objects.equals(containerLoggingParameters, other.containerLoggingParameters)
                && Objects.equals(containerMemory, other.containerMemory)
                && Objects.equals(containerName, other.containerName)
                && Objects.equals(containerNetworkingMode, other.containerNetworkingMode)
                && Objects.equals(containerPortProtocol, other.containerPortProtocol)
                && Objects.equals(containerVolumeString, other.containerVolumeString)
                && Objects.equals(containerVolumes, other.containerVolumes) && enabled == other.enabled
                && Objects.equals(enforcementDigest, other.enforcementDigest)
                && Objects.equals(externalPorts, other.externalPorts) && Objects.equals(image, other.image)
                && imageDownloadTimeout == other.imageDownloadTimeout && Objects.equals(imageTag, other.imageTag)
                && Objects.equals(internalPorts, other.internalPorts) && maxDownloadRetries == other.maxDownloadRetries
                && privilegedMode == other.privilegedMode && Objects.equals(registryPassword, other.registryPassword)
                && Objects.equals(registryURL, other.registryURL)
                && Objects.equals(registryUsername, other.registryUsername)
                && restartOnFailure == other.restartOnFailure && retryInterval == other.retryInterval
                && Objects.equals(containerRuntime, other.containerRuntime)
                && Objects.equals(signatureTrustAnchor, other.signatureTrustAnchor)
                && Objects.equals(signatureVerifyTransparencyLog, other.signatureVerifyTransparencyLog);
    }

}
