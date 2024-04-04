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

package org.eclipse.kura.container.orchestration;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.container.orchestration.ContainerNetworkConfiguration.ContainerNetworkConfigurationBuilder;
import org.eclipse.kura.container.orchestration.ImageConfiguration.ImageConfigurationBuilder;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Object which represents a container configuration used to request the
 * generation of a new container instance.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 *
 */
@ProviderType
public class ContainerConfiguration {

    private String containerName;
    private List<ContainerPort> containerPorts;
    private List<String> containerEnvVars;
    private List<String> containerDevices;
    private Map<String, String> containerVolumes;
    private Boolean containerPrivileged;
    private Boolean isFrameworkManaged = true;
    private Map<String, String> containerLoggerParameters;
    private String containerLoggingType;
    private ImageConfiguration imageConfig;
    private ContainerNetworkConfiguration networkConfiguration;
    private List<String> entryPoint;
    private Boolean containerRestartOnFailure = false;
    private Optional<Long> memory;
    private Optional<Float> cpus;
    private Optional<String> gpus;
    private Optional<String> runtime;
    private Optional<String> enforcementDigest;

    private ContainerConfiguration() {
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
     * given container.
     *
     * @return
     *
     * @deprecated since 2.5. Please use {@link getContainerPorts} as it includes
     *             the network
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
     * @deprecated since 2.5. Please use {@link getContainerPorts} as it includes
     *             the network
     *             protocol with the port mapping.
     */
    @Deprecated
    public List<Integer> getContainerPortsInternal() {
        return this.containerPorts.stream().map(ContainerPort::getInternalPort).collect(Collectors.toList());
    }

    /**
     * Returns the list of environment properties that will be passed to the
     * container
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
     * Returns a boolean representing if the container runs or will run in
     * privileged mode.
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
     * Returns the {@link ImageConfiguration} object
     *
     * @return
     * @since 2.4
     */
    public ImageConfiguration getImageConfiguration() {
        return this.imageConfig;
    }

    /**
     * Returns the base image for the associated container. Detailed image
     * information can be found in the {@link ImageConfig} class, provided by the
     * {@link #getImageConfiguration()} method.
     *
     * @return
     */
    public String getContainerImage() {
        return this.imageConfig.getImageName();
    }

    /**
     * Returns the image tag for the associated container. Detailed image
     * information can be found in the {@link ImageConfig} class, provided by the
     * {@link #getImageConfiguration()} method.
     *
     * @return
     */
    public String getContainerImageTag() {
        return this.imageConfig.getImageTag();
    }

    /**
     * Returns the Registry credentials. Detailed image information can be found in
     * the {@link ImageConfig} class, provided by the
     * {@link #getImageConfiguration()} method.
     *
     * @return
     */
    public Optional<RegistryCredentials> getRegistryCredentials() {
        return this.imageConfig.getRegistryCredentials();
    }

    /**
     * Returns the image download timeout (in seconds). Detailed image information
     * can be found in the {@link ImageConfig} class, provided by the
     * {@link #getImageConfiguration()} method.
     *
     * @return
     */
    public int getImageDownloadTimeoutSeconds() {
        return this.imageConfig.getimageDownloadTimeoutSeconds();
    }

    /**
     * return the container's network configuration as a
     * {@link ContainerNetworkConfiguration}.
     *
     * @return
     * @since 2.4
     */
    public ContainerNetworkConfiguration getContainerNetworkConfiguration() {
        return this.networkConfiguration;
    }

    /**
     * Returns a List<String> of container entry points. An empty list can be
     * returned if no entrypoints are specified.
     *
     * @return
     * @since 2.4
     */
    public List<String> getEntryPoint() {
        return this.entryPoint;
    }

    /**
     * Returns boolean which determines if container will restart on failure
     *
     * @return
     * @since 2.4
     */
    public boolean getRestartOnFailure() {
        return this.containerRestartOnFailure;
    }

    /**
     * Return the memory to be assigned to the container.
     *
     * @return
     * @since 2.4
     */
    public Optional<Long> getMemory() {
        return this.memory;
    }

    /**
     * Return the cpus resources to be assigned to the container.
     *
     * @return
     * @since 2.4
     */
    public Optional<Float> getCpus() {
        return this.cpus;
    }

    /**
     * Return the gpus to be assigned to the container.
     *
     * @return
     * @since 2.4
     */
    public Optional<String> getGpus() {
        return this.gpus;
    }

    /**
     * Return the runtime option to be assigned to the container.
     *
     * @return the optional runtime string used by the container
     * @since 2.7
     */
    public Optional<String> getRuntime() {
        return this.runtime;
    }

    /**
     * Return the enforcement digest assigned to the container.
     *
     * @return the optional runtime string used by the container
     * @since 2.7
     */
    public Optional<String> getEnforcementDigest() {
        return this.enforcementDigest;
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
        return Objects.hash(this.containerDevices, this.containerEnvVars, this.containerLoggerParameters,
                this.containerLoggingType, this.containerName, this.containerPorts, this.containerPrivileged,
                this.containerVolumes, this.cpus, this.enforcementDigest, this.entryPoint, this.gpus, this.imageConfig,
                this.isFrameworkManaged, this.memory, this.networkConfiguration, this.containerRestartOnFailure,
                this.runtime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ContainerConfiguration other = (ContainerConfiguration) obj;
        return Objects.equals(this.containerDevices, other.containerDevices)
                && Objects.equals(this.containerEnvVars, other.containerEnvVars)
                && Objects.equals(this.containerLoggerParameters, other.containerLoggerParameters)
                && Objects.equals(this.containerLoggingType, other.containerLoggingType)
                && Objects.equals(this.containerName, other.containerName)
                && Objects.equals(this.containerPorts, other.containerPorts)
                && Objects.equals(this.containerPrivileged, other.containerPrivileged)
                && Objects.equals(this.containerVolumes, other.containerVolumes)
                && Objects.equals(this.cpus, other.cpus) && Objects.equals(enforcementDigest, other.enforcementDigest)
                && Objects.equals(this.entryPoint, other.entryPoint) && Objects.equals(this.gpus, other.gpus)
                && Objects.equals(this.imageConfig, other.imageConfig)
                && Objects.equals(this.isFrameworkManaged, other.isFrameworkManaged)
                && Objects.equals(this.memory, other.memory)
                && Objects.equals(this.networkConfiguration, other.networkConfiguration)
                && Objects.equals(this.containerRestartOnFailure, other.containerRestartOnFailure)
                && Objects.equals(this.runtime, other.runtime);
    }

    public static final class ContainerConfigurationBuilder {

        private String containerName;
        private List<Integer> containerPortsExternal = new ArrayList<>();
        private List<Integer> containerPortsInternal = new ArrayList<>();
        private List<ContainerPort> containerPorts = new ArrayList<>();
        private List<String> containerEnvVars = new LinkedList<>();
        private List<String> containerDevices = new LinkedList<>();
        private Map<String, String> containerVolumes = new HashMap<>();
        private Boolean containerPrivileged = false;
        private Boolean isFrameworkManaged = false;
        private Map<String, String> containerLoggerParameters;
        private String containerLoggingType;
        private final ImageConfigurationBuilder imageConfigBuilder = new ImageConfiguration.ImageConfigurationBuilder();
        private final ContainerNetworkConfigurationBuilder networkConfigurationBuilder = new ContainerNetworkConfigurationBuilder();
        private List<String> entryPoint = new LinkedList<>();
        private Boolean containerRestartOnFailure = false;
        private Optional<Long> memory = Optional.empty();
        private Optional<Float> cpus = Optional.empty();
        private Optional<String> gpus = Optional.empty();
        private Optional<String> runtime = Optional.empty();
        private Optional<String> enforcementDigest = Optional.empty();

        public ContainerConfigurationBuilder setContainerName(String serviceName) {
            this.containerName = serviceName;
            return this;
        }

        public ContainerConfigurationBuilder setFrameworkManaged(Boolean isFrameworkManaged) {
            this.isFrameworkManaged = isFrameworkManaged;
            return this;
        }

        /**
         * Set a list of {@link ContainerPort}, to express which ports to expose and
         * what protocol to use.
         *
         * @since 2.5
         */
        public ContainerConfigurationBuilder setContainerPorts(List<ContainerPort> containerPorts) {
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
         * @deprecated since 2.5. Please use {@link setContainerPorts} as it allows for
         *             network protocol to be specified in a port mapping.
         */
        @Deprecated
        public ContainerConfigurationBuilder setExternalPorts(List<Integer> containerPortsExternal) {
            this.containerPortsExternal = new ArrayList<>(containerPortsExternal);
            return this;
        }

        /**
         * Accepts a list<Integer> of ports to be open internally within the
         * container. Assumes all ports are TCP. To
         * use other Internet protocols please see the
         * {@link setContainerPorts} method.
         * Ensure that the number of elements in this list is the same as
         * the number of elements set with {@link setExternalPorts}.
         *
         * @deprecated since 2.5. Please use {@link setContainerPorts} as it allows for
         *             network protocol to be specified in a port mapping.
         *
         */
        @Deprecated
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

        public ContainerConfigurationBuilder setContainerImage(String serviceImage) {
            this.imageConfigBuilder.setImageName(serviceImage);
            return this;
        }

        public ContainerConfigurationBuilder setContainerImageTag(String serviceImageTag) {
            this.imageConfigBuilder.setImageTag(serviceImageTag);
            return this;
        }

        public ContainerConfigurationBuilder setRegistryCredentials(Optional<RegistryCredentials> registryCredentials) {
            this.imageConfigBuilder.setRegistryCredentials(registryCredentials);
            return this;
        }

        public ContainerConfigurationBuilder setImageDownloadTimeoutSeconds(int imageDownloadTimeoutSeconds) {
            this.imageConfigBuilder.setImageDownloadTimeoutSeconds(imageDownloadTimeoutSeconds);
            return this;
        }

        /**
         * @since 2.4
         */
        public ContainerConfigurationBuilder setEntryPoint(List<String> entryPoint) {
            this.entryPoint = entryPoint;
            return this;
        }

        /**
         * Set the {@link NetworkConfiguration}
         *
         * @since 2.4
         */
        public ContainerConfigurationBuilder setContainerNetowrkConfiguration(
                ContainerNetworkConfiguration networkConfiguration) {
            this.networkConfigurationBuilder.setNetworkMode(networkConfiguration.getNetworkMode());
            return this;
        }

        /**
         * Set the {@link ImageConfiguration}
         *
         * @since 2.4
         */
        public ContainerConfigurationBuilder setImageConfiguration(ImageConfiguration imageConfig) {
            this.imageConfigBuilder.setImageName(imageConfig.getImageName());
            this.imageConfigBuilder.setImageTag(imageConfig.getImageTag());
            this.imageConfigBuilder.setRegistryCredentials(imageConfig.getRegistryCredentials());
            this.imageConfigBuilder.setImageDownloadTimeoutSeconds(imageConfig.getimageDownloadTimeoutSeconds());
            return this;
        }

        /**
         * Set if container will restart on failure
         *
         * @since 2.4
         */
        public ContainerConfigurationBuilder setRestartOnFailure(boolean containerRestartOnFailure) {
            this.containerRestartOnFailure = containerRestartOnFailure;
            return this;
        }

        /**
         * @since 2.4
         */
        public ContainerConfigurationBuilder setMemory(Optional<Long> memory) {
            this.memory = memory;
            return this;
        }

        /**
         * @since 2.4
         */
        public ContainerConfigurationBuilder setCpus(Optional<Float> cpus) {
            this.cpus = cpus;
            return this;
        }

        /**
         * @since 2.4
         */
        public ContainerConfigurationBuilder setGpus(Optional<String> gpus) {
            this.gpus = gpus;
            return this;
        }

        /**
         * @since 2.7
         */
        public ContainerConfigurationBuilder setRuntime(Optional<String> runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * @since 2.7
         */
        public ContainerConfigurationBuilder setEnforcementDigest(Optional<String> digest) {
            this.enforcementDigest = digest;
            return this;
        }

        public ContainerConfiguration build() {

            if (this.containerPorts.isEmpty()) {
                Iterator<Integer> extPort = this.containerPortsExternal.iterator();
                Iterator<Integer> intPort = this.containerPortsInternal.iterator();

                while (extPort.hasNext() && intPort.hasNext()) {
                    this.containerPorts.add(new ContainerPort(intPort.next(), extPort.next()));
                }
            }

            ContainerConfiguration result = new ContainerConfiguration();

            result.containerName = requireNonNull(this.containerName, "Request Container Name cannot be null");
            result.containerPorts = this.containerPorts;
            result.containerEnvVars = this.containerEnvVars;
            result.containerDevices = this.containerDevices;
            result.containerVolumes = this.containerVolumes;
            result.containerPrivileged = this.containerPrivileged;
            result.isFrameworkManaged = this.isFrameworkManaged;
            result.containerLoggerParameters = this.containerLoggerParameters;
            result.containerLoggingType = this.containerLoggingType;
            result.imageConfig = this.imageConfigBuilder.build();
            result.networkConfiguration = this.networkConfigurationBuilder.build();
            result.entryPoint = requireNonNull(this.entryPoint, "Container EntryPoint list must not be null");
            result.containerRestartOnFailure = this.containerRestartOnFailure;
            result.memory = this.memory;
            result.cpus = this.cpus;
            result.gpus = this.gpus;
            result.runtime = this.runtime;
            result.enforcementDigest = this.enforcementDigest;

            return result;
        }

    }
}
