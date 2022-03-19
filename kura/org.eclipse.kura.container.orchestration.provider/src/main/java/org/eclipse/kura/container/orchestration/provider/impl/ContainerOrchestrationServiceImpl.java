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
package org.eclipse.kura.container.orchestration.provider.impl;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerInstanceDescriptor;
import org.eclipse.kura.container.orchestration.ContainerOrchestrationService;
import org.eclipse.kura.container.orchestration.ContainerState;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.eclipse.kura.container.orchestration.RegistryCredentials;
import org.eclipse.kura.container.orchestration.listener.ContainerOrchestrationServiceListener;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.LogConfig.LoggingType;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class ContainerOrchestrationServiceImpl implements ConfigurableComponent, ContainerOrchestrationService {

    private static final String PARAMETER_CANNOT_BE_NULL = "The provided parameter cannot be null";
    private static final String UNABLE_TO_CONNECT_TO_DOCKER_CLI = "Unable to connect to docker cli";
    private static final Logger logger = LoggerFactory.getLogger(ContainerOrchestrationServiceImpl.class);
    private static final String APP_ID = "org.eclipse.kura.container.orchestration.provider.ConfigurableDocker";

    private ContainerOrchestrationServiceOptions currentConfig;

    private final Set<ContainerOrchestrationServiceListener> dockerServiceListeners = new HashSet<>();
    private final Set<FrameworkManagedContainer> frameworkManagedContainers = new HashSet<>();

    private DockerClient dockerClient;
    private CryptoService cryptoService;

    public void setDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void activate(Map<String, Object> properties) {
        logger.info("Bundle {} is starting with config!", APP_ID);
        if (!isNull(properties)) {
            updated(properties);
        }
        logger.info("Bundle {} has started with config!", APP_ID);
    }

    public void deactivate() {
        logger.info("Bundle {} is stopping!", APP_ID);
        if (testConnection()) {
            disconnect();
        }
        logger.info("Bundle {} has stopped!", APP_ID);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Bundle {} is updating with config!", APP_ID);
        ContainerOrchestrationServiceOptions newProps = new ContainerOrchestrationServiceOptions(properties);
        if (!newProps.equals(this.currentConfig)) {

            this.currentConfig = newProps;

            logger.info("Connecting to docker ");

            if (!this.currentConfig.isEnabled()) {
                cleanUpDocker();
                return;
            }

            connect();

            if (!testConnection()) {
                logger.error("Could not connect to docker CLI.");
                return;
            }

            logger.info("Connection Successful");
        }

        logger.info("Bundle {} has updated with config!", APP_ID);
    }

    @Override
    public List<String> listContainersIds() {
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        List<String> result = new ArrayList<>();

        for (Container cont : containers) {
            result.add(cont.getId());
        }

        return result;

    }

    @Override
    public List<ContainerInstanceDescriptor> listContainerDescriptors() {
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        List<ContainerInstanceDescriptor> result = new ArrayList<>();
        containers.forEach(container -> result.add(ContainerInstanceDescriptor.builder()
                .setContainerName(getContainerName(container)).setContainerImage(getContainerTag(container))
                .setContainerImageTag(getContainerVersion(container)).setContainerID(container.getId())
                .setInternalPorts(parseInternalPortsFromDockerPs(container.getPorts()))
                .setExternalPorts(parseExternalPortsFromDockerPs(container.getPorts()))
                .setContainerState(convertDockerStateToFrameworkState(container.getState()))
                .setFrameworkManaged(isFrameworkManaged(container)).build()));

        return result;

    }

    private Boolean isFrameworkManaged(Container container) {
        String containerName = getContainerName(container);
        return this.frameworkManagedContainers.stream().anyMatch(c -> c.name.equals(containerName));
    }

    private String getContainerName(Container container) {
        return container.getNames()[0].replace("/", "");
    }

    private String getContainerVersion(Container container) {
        String version = "";
        if (container.getImage().split(":").length > 1) {
            version = container.getImage().split(":")[1];
        }
        return version;
    }

    private String getContainerTag(Container container) {
        return container.getImage().split(":")[0];
    }

    private List<Integer> parseExternalPortsFromDockerPs(ContainerPort[] ports) {
        List<Integer> externalPorts = new ArrayList<>();

        ContainerPort[] tempPorts = ports;
        for (ContainerPort tempPort : tempPorts) {
            if (tempPort.getIp() != null) {
                String ipFormatTest = tempPort.getIp();
                if (ipFormatTest != null && (ipFormatTest.equals("::") || ipFormatTest.equals("0.0.0.0"))) {
                    externalPorts.add(tempPort.getPublicPort());
                }
            }
        }
        return externalPorts;
    }

    private List<Integer> parseInternalPortsFromDockerPs(ContainerPort[] ports) {
        List<Integer> internalPorts = new ArrayList<>();

        ContainerPort[] tempPorts = ports;
        for (ContainerPort tempPort : tempPorts) {
            if (tempPort.getIp() != null) {
                String ipFormatTest = tempPort.getIp();
                if (ipFormatTest != null && (ipFormatTest.equals("::") || ipFormatTest.equals("0.0.0.0"))) {
                    internalPorts.add(tempPort.getPrivatePort());
                }
            }
        }
        return internalPorts;
    }

    private ContainerState convertDockerStateToFrameworkState(String dockerState) {

        switch (dockerState.trim()) {
        case "created":
            return ContainerState.INSTALLED;
        case "restarting":
            return ContainerState.INSTALLED;
        case "running":
            return ContainerState.ACTIVE;
        case "paused":
            return ContainerState.STOPPING;
        case "exited":
            return ContainerState.STOPPING;
        case "dead":
            return ContainerState.FAILED;
        default:
            return ContainerState.INSTALLED;
        }
    }

    @Override
    public Optional<String> getContainerIdByName(String name) {
        checkRequestEnv(name);

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        for (Container cont : containers) {
            String[] containerNames = cont.getNames();
            for (String containerName : containerNames) {
                if (containerName.equals("/" + name)) { // docker API seems to put a '/' in front of the names
                    return Optional.of(cont.getId());
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void startContainer(String id) throws KuraException {
        checkRequestEnv(id);
        try {
            this.dockerClient.startContainerCmd(id).exec();
        } catch (Exception e) {
            logger.error("Could not start container {}. It could be already running or not exist at all", id);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR);
        }
    }

    @Override
    public String startContainer(ContainerConfiguration container) throws KuraException, InterruptedException {
        checkRequestEnv(container);

        logger.info("Starting {} Microservice", container.getContainerName());

        final Optional<ContainerInstanceDescriptor> existingInstance = listContainerDescriptors().stream()
                .filter(c -> c.getContainerName().equals(container.getContainerName())).findAny();

        final String containerId;

        if (existingInstance.isPresent() && existingInstance.get().getContainerState() == ContainerState.ACTIVE) {
            logger.info("Found already existing running container");
            containerId = existingInstance.get().getContainerId();
        } else if (!existingInstance.isPresent()) {
            logger.info("Creating new container instance");
            pullImage(container.getContainerImage(), container.getContainerImageTag(),
                    container.getImageDownloadTimeoutSeconds(), container.getRegistryCredentials());
            containerId = createContainer(container);
            startContainer(containerId);
        } else {
            logger.info("Found already exisiting not running container, starting it..");
            containerId = existingInstance.get().getContainerId();
            startContainer(containerId);
        }

        logger.info("Container Started Successfully");

        if (container.isFrameworkManaged()) {
            this.frameworkManagedContainers
                    .add(new FrameworkManagedContainer(container.getContainerName(), containerId));
        }

        return containerId;
    }

    @Override
    public void stopContainer(String id) throws KuraException {
        checkRequestEnv(id);

        try {
            this.dockerClient.stopContainerCmd(id).exec();
        } catch (Exception e) {
            logger.error("Could not stop container {}.", id);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR);
        }
    }

    @Override
    public void deleteContainer(String id) throws KuraException {
        checkRequestEnv(id);
        try {
            this.dockerClient.removeContainerCmd(id).exec();
            this.frameworkManagedContainers.removeIf(c -> id.equals(c.id));
        } catch (Exception e) {
            logger.error("Could not remove container {}.", id);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR);
        }
    }

    private void checkRequestEnv(Object parameter) {
        if (isNull(parameter)) {
            throw new IllegalArgumentException(PARAMETER_CANNOT_BE_NULL);
        }
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }
    }

    @Override
    public void pullImage(String imageName, String imageTag, int timeOutSeconds,
            Optional<RegistryCredentials> registryCredentials) throws KuraException, InterruptedException {
        if (isNull(imageName) || isNull(imageTag) || timeOutSeconds < 0 || isNull(registryCredentials)) {
            throw new IllegalArgumentException("Parameters cannot be null or negative");
        }
        boolean imageAvailableLocally = doesImageExist(imageName, imageTag);

        if (!imageAvailableLocally) {
            try {
                imagePullHelper(imageName, imageTag, timeOutSeconds, registryCredentials);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.IO_ERROR, "Unable to pull container");
            }
        }
    }

    @Override
    public void registerListener(ContainerOrchestrationServiceListener dockerListener) {
        this.dockerServiceListeners.add(dockerListener);

    }

    @Override
    public void unregisterListener(ContainerOrchestrationServiceListener dockerListener) {
        this.dockerServiceListeners.remove(dockerListener);

    }

    private void imagePullHelper(String imageName, String imageTag, int timeOutSeconds,
            Optional<RegistryCredentials> repositoryCredentials) throws InterruptedException, KuraException {

        logger.info("Attempting to pull image: {}.", imageName);
        PullImageCmd pullRequest = this.dockerClient.pullImageCmd(imageName).withTag(imageTag);

        if (repositoryCredentials.isPresent()) {
            doAuthenticate(repositoryCredentials.get(), pullRequest);
        }

        pullRequest.exec(new PullImageResultCallback() {

            @Override
            public void onNext(PullResponseItem item) {
                super.onNext(item);
                createLoggerMessageForContainerPull(item, imageName, imageTag);

            }

        }).awaitCompletion(timeOutSeconds, TimeUnit.SECONDS);

    }

    private void doAuthenticate(RegistryCredentials repositoryCredentials, PullImageCmd pullRequest)
            throws KuraException {
        if (!(repositoryCredentials instanceof PasswordRegistryCredentials)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        PasswordRegistryCredentials repositoryPasswordCredentials = (PasswordRegistryCredentials) repositoryCredentials;

        AuthConfig authConfig = new AuthConfig().withUsername(repositoryPasswordCredentials.getUsername()).withPassword(
                new String(this.cryptoService.decryptAes(repositoryPasswordCredentials.getPassword().getPassword())));
        Optional<String> url = repositoryPasswordCredentials.getUrl();
        if (url.isPresent()) {
            logger.info("Attempting to sign into repo: {}", url.get());
            authConfig = authConfig.withRegistryAddress(url.get());
        }
        pullRequest.withAuthConfig(authConfig);
    }

    private void createLoggerMessageForContainerPull(PullResponseItem item, String imageName, String imageTag) {

        if (logger.isDebugEnabled()) {
            logger.debug("Pulling {}:{} Layer {}, State: {}", imageName, imageTag, item.getId(), item.getStatus());
        }

        if (item.isErrorIndicated()) {
            logger.error("Unable To Pull image {}:{} because : {}", item.getErrorDetail(), imageName, imageTag);
        }

        if (item.isPullSuccessIndicated()) {

            logger.info("Image pull of {}:{}, Layer: {}, was succsessful", imageName, imageTag, item.getId());
        }
    }

    private String createContainer(ContainerConfiguration containerDescription) throws KuraException {
        if (!testConnection()) {
            throw new IllegalStateException("failed to reach docker engine");
        }

        if (containerDescription == null || containerDescription.getContainerImage() == null
                || containerDescription.getContainerImageTag() == null) {
            throw new IllegalStateException("failed to create container, null containerImage passed");
        }

        String containerImageFullString = String.format("%s:%s", containerDescription.getContainerImage(),
                containerDescription.getContainerImageTag());

        CreateContainerCmd commandBuilder = null;
        try {
            commandBuilder = this.dockerClient.createContainerCmd(containerImageFullString);

            if (containerDescription.getContainerName() != null) {
                commandBuilder = commandBuilder.withName(containerDescription.getContainerName());
            }

            HostConfig configuration = new HostConfig();

            commandBuilder = containerEnviromentVariablesHandler(containerDescription, commandBuilder);

            // Host Configuration Related
            configuration = containerVolumeMangamentHandler(containerDescription, configuration);

            configuration = containerDevicesHandler(containerDescription, configuration);

            configuration = containerPortManagementHandler(containerDescription, configuration);

            configuration = containerLogConfigurationHandler(containerDescription, configuration);

            if (containerDescription.isContainerPrivileged()) {
                configuration = configuration.withPrivileged(containerDescription.isContainerPrivileged());
            }

            return commandBuilder.withHostConfig(configuration).exec().getId();

        } catch (Exception e) {
            logger.error("failed to create container", e);
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR);
        } finally {
            if (!isNull(commandBuilder)) {
                commandBuilder.close();
            }
        }
    }

    private HostConfig containerLogConfigurationHandler(ContainerConfiguration containerDescription,
            HostConfig configuration) {
        LoggingType lt;
        switch (containerDescription.getContainerLoggingType().toUpperCase().trim()) {
        case "NONE":
            lt = LoggingType.NONE;
            break;
        case "LOCAL":
            lt = LoggingType.LOCAL;
            break;
        case "ETWLOGS":
            lt = LoggingType.ETWLOGS;
            break;
        case "JSON_FILE":
            lt = LoggingType.JSON_FILE;
            break;
        case "SYSLOG":
            lt = LoggingType.SYSLOG;
            break;
        case "JOURNALD":
            lt = LoggingType.JOURNALD;
            break;
        case "GELF":
            lt = LoggingType.GELF;
            break;
        case "FLUENTD":
            lt = LoggingType.FLUENTD;
            break;
        case "AWSLOGS":
            lt = LoggingType.AWSLOGS;
            break;
        case "DB":
            lt = LoggingType.DB;
            break;
        case "SPLUNK":
            lt = LoggingType.SPLUNK;
            break;
        case "GCPLOGS":
            lt = LoggingType.GCPLOGS;
            break;
        case "LOKI":
            lt = LoggingType.LOKI;
            break;
        default:
            lt = LoggingType.DEFAULT;
            break;
        }

        LogConfig lc = new LogConfig(lt, containerDescription.getLoggerParameters());

        configuration.withLogConfig(lc);

        return configuration;
    }

    private HostConfig containerPortManagementHandler(ContainerConfiguration containerDescription,
            HostConfig commandBuilder) {

        if (containerDescription.getContainerPortsInternal() != null
                && containerDescription.getContainerPortsExternal() != null
                && containerDescription.getContainerPortsExternal().size() == containerDescription
                        .getContainerPortsInternal().size()) {
            List<ExposedPort> exposedPorts = new LinkedList<>();
            Ports portbindings = new Ports();

            for (int index = 0; index < containerDescription.getContainerPortsInternal().size(); index++) {

                ExposedPort tempExposedPort = new ExposedPort(
                        containerDescription.getContainerPortsInternal().get(index));
                exposedPorts.add(tempExposedPort);
                portbindings.bind(tempExposedPort,
                        Binding.bindPort(containerDescription.getContainerPortsExternal().get(index)));
            }

            commandBuilder.withPortBindings(portbindings);

        } else {
            logger.error("portsExternal and portsInternal must be int[] of the same size or they do not exist: {}",
                    containerDescription.getContainerName());
        }

        return commandBuilder;
    }

    private CreateContainerCmd containerEnviromentVariablesHandler(ContainerConfiguration containerDescription,
            CreateContainerCmd commandBuilder) {

        if (containerDescription.getContainerEnvVars().isEmpty()) {
            return commandBuilder;
        }

        if (containerDescription.getContainerEnvVars() != null
                && !containerDescription.getContainerEnvVars().isEmpty()) {
            List<String> formattedEnvVars = new LinkedList<>();
            for (String env : containerDescription.getContainerEnvVars()) {
                if (!env.trim().isEmpty()) {
                    formattedEnvVars.add(env.trim());
                }
            }
            commandBuilder = commandBuilder.withEnv(formattedEnvVars);
        }

        return commandBuilder;

    }

    private HostConfig containerVolumeMangamentHandler(ContainerConfiguration containerDescription,
            HostConfig hostConfiguration) {

        if (containerDescription.getContainerVolumes().isEmpty()) {
            return hostConfiguration;
        }

        List<Bind> bindsToAdd = new LinkedList<>();

        if (containerDescription.getContainerVolumes() != null
                && !containerDescription.getContainerVolumes().isEmpty()) {

            for (Map.Entry<String, String> element : containerDescription.getContainerVolumes().entrySet()) {
                // source: path on host (key)
                // destination: path in container (value)
                if (!element.getKey().isEmpty() && !element.getValue().isEmpty()) {
                    Volume tempVolume = new Volume(element.getValue());
                    Bind tempBind = new Bind(element.getKey(), tempVolume);
                    bindsToAdd.add(tempBind);
                }
            }
            hostConfiguration = hostConfiguration.withBinds(bindsToAdd);

        }

        return hostConfiguration;

    }

    private HostConfig containerDevicesHandler(ContainerConfiguration containerDescription,
            HostConfig hostConfiguration) {

        if (containerDescription.getContainerDevices().isEmpty()) {
            return hostConfiguration;
        }

        if (containerDescription.getContainerDevices() != null
                && !containerDescription.getContainerDevices().isEmpty()) {

            List<Device> deviceList = new LinkedList<>();
            for (String deviceString : containerDescription.getContainerDevices()) {
                deviceList.add(Device.parse(deviceString));
            }
            if (!deviceList.isEmpty()) {
                hostConfiguration = hostConfiguration.withDevices(deviceList);

            }
        }

        return hostConfiguration;

    }

    private boolean doesImageExist(String imageName, String imageTag) {

        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Image> images = this.dockerClient.listImagesCmd().exec();

        for (Image image : images) {
            if (image.getRepoTags() != null) {
                for (String tags : image.getRepoTags()) {
                    if (tags.contains(imageName + ":" + imageTag)) {
                        return true;
                    }
                }

            }
        }

        return false;
    }

    private void cleanUpDocker() {

        if (testConnection()) {
            this.dockerServiceListeners.forEach(ContainerOrchestrationServiceListener::onDisabled);
            disconnect();
        }
    }

    private boolean connect() {
        if (this.currentConfig.getHostUrl() == null) {
            return false;
        }
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(this.currentConfig.getHostUrl()).build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);

        final boolean connected = testConnection();

        if (connected) {
            this.dockerServiceListeners.forEach(ContainerOrchestrationServiceListener::onConnect);
        }
        return connected;
    }

    private void disconnect() {
        if (testConnection()) {
            try {
                this.dockerServiceListeners.forEach(ContainerOrchestrationServiceListener::onDisconnect);
                this.dockerClient.close();
            } catch (IOException e) {
                logger.error("Error disconnecting", e);
            }
        }
    }

    private boolean testConnection() {
        boolean canConnect = false;
        try {
            this.dockerClient.pingCmd().exec();
            canConnect = true;
        } catch (Exception ex) {
            canConnect = false;
        }
        return canConnect;
    }

    private static class FrameworkManagedContainer {

        private final String name;
        private final String id;

        public FrameworkManagedContainer(String name, String id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FrameworkManagedContainer other = (FrameworkManagedContainer) obj;
            return Objects.equals(this.id, other.id) && Objects.equals(this.name, other.name);
        }

    }
}
