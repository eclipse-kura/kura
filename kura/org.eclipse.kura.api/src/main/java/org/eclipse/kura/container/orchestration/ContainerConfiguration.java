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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Object which represents a container configuration used to request the generation of a new container instance and
 * running
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 *
 */
@ProviderType
public class ContainerConfiguration {

    private String containerName;
    private String containerImage;
    private String containerImageTag;
    private List<Integer> containerPortsExternal;
    private List<Integer> containerPortsInternal;
    private List<String> containerEnvVars;
    private List<String> containerDevices;
    private Map<String, String> containerVolumes;
    private Boolean containerPrivileged;
    private Boolean isFrameworkManaged = true;
    private Map<String, String> containerLoggerParameters;
    private String containerLoggingType;
    private Optional<RegistryCredentials> registryCredentials;
    private int imageDownloadTimeoutSeconds;

    private ContainerConfiguration() {
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

    /**
     * Returns the list of environment properties that will be passed to the container
     *
     * @return
     */
    public List<String> getContainerEnvVars() {
        return this.containerEnvVars;
    }

    /**
     * Returns the list of devices that will be mapped to the container
     *
     * @return
     */
    public List<String> getContainerDevices() {
        return this.containerDevices;
    }

    /**
     * Returns a map that identifies the volumes and the internal container mapping
     *
     * @return
     */
    public Map<String, String> getContainerVolumes() {
        return this.containerVolumes;
    }

    /**
     * Returns a boolean representing if the container runs or will run in privileged mode.
     *
     * @return
     */
    public boolean isContainerPrivileged() {
        return this.containerPrivileged;
    }

    /**
     * Returns a Map that identifies configured logger parameters.
     *
     * @return
     */
    public Map<String, String> getLoggerParameters() {
        return this.containerLoggerParameters;
    }

    /**
     * Returns a string identifying which logger driver to use.
     *
     * @return
     */
    public String getContainerLoggingType() {
        return this.containerLoggingType;
    }

    /**
     * Returns the Registry credentials
     *
     * @return
     */
    public Optional<RegistryCredentials> getRegistryCredentials() {
        return this.registryCredentials;
    }

    /**
     * Returns the image download timeout (in seconds)
     *
     * @return
     */
    public int getImageDownloadTimeoutSeconds() {
        return this.imageDownloadTimeoutSeconds;
    }

    /**
     * Creates a builder for creating a new {@link ContainerConfiguration} instance.
     *
     * @return the builder.
     */
    public static ContainerConfigurationBuilder builder() {
        return new ContainerConfigurationBuilder();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.containerDevices, this.containerEnvVars, this.containerImage, this.containerImageTag,
                this.containerLoggerParameters, this.containerLoggingType, this.containerName,
                this.containerPortsExternal, this.containerPortsInternal, this.containerPrivileged,
                this.containerVolumes, this.imageDownloadTimeoutSeconds, this.isFrameworkManaged,
                this.registryCredentials);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ContainerConfiguration)) {
            return false;
        }
        ContainerConfiguration other = (ContainerConfiguration) obj;
        return Objects.equals(this.containerDevices, other.containerDevices)
                && Objects.equals(this.containerEnvVars, other.containerEnvVars)
                && Objects.equals(this.containerImage, other.containerImage)
                && Objects.equals(this.containerImageTag, other.containerImageTag)
                && Objects.equals(this.containerLoggerParameters, other.containerLoggerParameters)
                && Objects.equals(this.containerLoggingType, other.containerLoggingType)
                && Objects.equals(this.containerName, other.containerName)
                && Objects.equals(this.containerPortsExternal, other.containerPortsExternal)
                && Objects.equals(this.containerPortsInternal, other.containerPortsInternal)
                && Objects.equals(this.containerPrivileged, other.containerPrivileged)
                && Objects.equals(this.containerVolumes, other.containerVolumes)
                && this.imageDownloadTimeoutSeconds == other.imageDownloadTimeoutSeconds
                && Objects.equals(this.isFrameworkManaged, other.isFrameworkManaged)
                && Objects.equals(this.registryCredentials, other.registryCredentials);
    }

    public static final class ContainerConfigurationBuilder {

        private String containerName;
        private String containerImage;
        private String containerImageTag;
        private List<Integer> containerPortsExternal = new ArrayList<>();
        private List<Integer> containerPortsInternal = new ArrayList<>();
        private List<String> containerEnvVars = new LinkedList<>();
        private List<String> containerDevices = new LinkedList<>();
        private Map<String, String> containerVolumes = new HashMap<>();
        private Boolean containerPrivileged = false;
        private Boolean isFrameworkManaged = false;
        private Map<String, String> containerLoggerParameters;
        private String containerLoggingType;
        private Optional<RegistryCredentials> registryCredentials;
        private int imageDownloadTimeoutSeconds = 500;

        public ContainerConfigurationBuilder setContainerName(String serviceName) {
            this.containerName = serviceName;
            return this;
        }

        public ContainerConfigurationBuilder setFrameworkManaged(Boolean isFrameworkManaged) {
            this.isFrameworkManaged = isFrameworkManaged;
            return this;
        }

        public ContainerConfigurationBuilder setContainerImage(String serviceImage) {
            this.containerImage = serviceImage;
            return this;
        }

        public ContainerConfigurationBuilder setContainerImageTag(String serviceImageTag) {
            this.containerImageTag = serviceImageTag;
            return this;
        }

        public ContainerConfigurationBuilder setExternalPorts(List<Integer> containerPortsExternal) {
            this.containerPortsExternal = new ArrayList<>(containerPortsExternal);
            return this;
        }

        public ContainerConfigurationBuilder setInternalPorts(List<Integer> containerPortsInternal) {
            this.containerPortsInternal = new ArrayList<>(containerPortsInternal);
            return this;
        }

        public ContainerConfigurationBuilder setEnvVars(List<String> vars) {
            this.containerEnvVars = new LinkedList<>(vars);
            return this;
        }

        public ContainerConfigurationBuilder setDeviceList(List<String> devices) {
            this.containerDevices = new LinkedList<>(devices);
            return this;
        }

        public ContainerConfigurationBuilder setVolumes(Map<String, String> volumeMap) {
            this.containerVolumes = new HashMap<>(volumeMap);
            return this;
        }

        public ContainerConfigurationBuilder setLoggerParameters(Map<String, String> paramMap) {
            this.containerLoggerParameters = new HashMap<>(paramMap);
            return this;
        }

        public ContainerConfigurationBuilder setLoggingType(String containerLoggingType) {
            this.containerLoggingType = containerLoggingType;
            return this;
        }

        public ContainerConfigurationBuilder setPrivilegedMode(Boolean containerPrivileged) {
            this.containerPrivileged = containerPrivileged;
            return this;
        }

        public ContainerConfigurationBuilder setRegistryCredentials(Optional<RegistryCredentials> registryCredentials) {
            this.registryCredentials = registryCredentials;
            return this;
        }

        public ContainerConfigurationBuilder setImageDownloadTimeoutSeconds(int imageDownloadTimeoutSeconds) {
            this.imageDownloadTimeoutSeconds = imageDownloadTimeoutSeconds;
            return this;
        }

        public ContainerConfiguration build() {
            ContainerConfiguration result = new ContainerConfiguration();

            result.containerName = requireNonNull(this.containerName, "Request Container Name cannot be null");
            result.containerImage = requireNonNull(this.containerImage, "Request Container Image cannot be null");
            result.containerImageTag = this.containerImageTag;
            result.containerPortsExternal = this.containerPortsExternal;
            result.containerPortsInternal = this.containerPortsInternal;
            result.containerEnvVars = this.containerEnvVars;
            result.containerDevices = this.containerDevices;
            result.containerVolumes = this.containerVolumes;
            result.containerPrivileged = this.containerPrivileged;
            result.isFrameworkManaged = this.isFrameworkManaged;
            result.containerLoggerParameters = this.containerLoggerParameters;
            result.containerLoggingType = this.containerLoggingType;
            result.registryCredentials = requireNonNull(this.registryCredentials,
                    "Request Registry Credentials object cannot be null");
            result.imageDownloadTimeoutSeconds = this.imageDownloadTimeoutSeconds;

            return result;
        }

    }
}