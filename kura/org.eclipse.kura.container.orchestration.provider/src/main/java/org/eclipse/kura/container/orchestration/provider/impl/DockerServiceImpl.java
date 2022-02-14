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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;
import org.eclipse.kura.container.orchestration.provider.ContainerStates;
import org.eclipse.kura.container.orchestration.provider.DockerService;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerServiceImpl implements ConfigurableComponent, DockerService {

    private static final String UNABLE_TO_CONNECT_TO_DOCKER_CLI = "Unable to connect to docker cli";
    private static final Logger logger = LoggerFactory.getLogger(DockerServiceImpl.class);
    private static final String APP_ID = "org.eclipse.kura.container.orchestration.provider.ConfigurableDocker";

    private DockerServiceOptions currentConfig;
    private List<ContainerDescriptor> containerQueue = new LinkedList<>();
    private final List<ContainerDescriptor> runningContainers = new LinkedList<>();
    private final List<ContainerDescriptor> registeredContainers = new LinkedList<>();

    private DockerClient dockerClient;
    private AuthConfig dockerAuthConfig;
    private CryptoService cryptoService;

    public void setDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        if (this.cryptoService == cryptoService) {
            this.cryptoService = null;
        }
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
        DockerServiceOptions newProps = new DockerServiceOptions(properties);
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

            logger.info("Connection Succsessful");

            logger.info("Populating running container list");
            populateRunningQueue();

            logger.info("Starting All Queued containers");
            startUpAllQueuedMicroservices();
        }

        logger.info("Bundle {} has updated with config!", APP_ID);
    }

    @Override
    public String[] listContainers() {
        String[] avaliableContainers;
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        avaliableContainers = new String[containers.size()];

        int temp = 0;
        for (Container cont : containers) {
            avaliableContainers[temp] = cont.toString();
            temp++;
        }

        return avaliableContainers;

    }

    @Override
    public String[] listContainersByID() {
        String[] avaliableContainers;
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        avaliableContainers = new String[containers.size()];

        int temp = 0;
        for (Container cont : containers) {
            avaliableContainers[temp] = cont.getId();
            temp++;
        }

        return avaliableContainers;

    }

    @Override
    public ContainerDescriptor[] listByContainerDescriptor() {
        ContainerDescriptor[] avaliableContainers;
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        avaliableContainers = new ContainerDescriptor[containers.size()];

        if (!containers.isEmpty()) {
            int temp = 0;
            for (Container cont : containers) {

                String tag = cont.getImage().split(":")[0];
                String version = "";

                if (cont.getImage().split(":").length > 1) {
                    version = cont.getImage().split(":")[1];
                }

                avaliableContainers[temp] = ContainerDescriptor.builder()
                        .setContainerName(cont.getNames()[0].replace("/", "")).setContainerImage(tag)
                        .setContainerImageTag(version).setContainerID(cont.getId())
                        .setInternalPort(parseInternalPortsFromDockerPs(cont.getPorts()))
                        .setExternalPort(parseExternalPortsFromDockerPs(cont.getPorts()))
                        .setContainerState(convertDockerStateToESFState(cont.getState())).setIsEsfManaged(false)
                        .build();
                temp++;
            }
        }
        return avaliableContainers;

    }

    private int[] parseExternalPortsFromDockerPs(ContainerPort[] ports) {
        int[] externalPorts = new int[] {};

        ContainerPort[] tempPorts = ports;
        for (ContainerPort tempPort : tempPorts) {
            if (tempPort.getIp() != null) {
                String ipFormatTest = tempPort.getIp();
                if (ipFormatTest != null && ipFormatTest.equals("::")) {
                    ArrayUtils.add(externalPorts, tempPort.getPublicPort());
                }
            }
        }
        return externalPorts;
    }

    private int[] parseInternalPortsFromDockerPs(ContainerPort[] ports) {
        int[] internalPorts = new int[] {};

        ContainerPort[] tempPorts = ports;
        for (ContainerPort tempPort : tempPorts) {
            if (tempPort.getIp() != null) {
                String ipFormatTest = tempPort.getIp();
                if (ipFormatTest != null && ipFormatTest.equals("::")) {
                    ArrayUtils.add(internalPorts, tempPort.getPrivatePort());
                }
            }
        }
        return internalPorts;
    }

    private ContainerStates convertDockerStateToESFState(String dockerState) {

        switch (dockerState.trim()) {
        case "created":
            return ContainerStates.INSTALLED;
        case "restarting":
            return ContainerStates.INSTALLED;
        case "running":
            return ContainerStates.ACTIVE;
        case "paused":
            return ContainerStates.STOPPING;
        case "exited":
            return ContainerStates.STOPPING;
        case "dead":
            return ContainerStates.FAILED;
        default:
            return ContainerStates.INSTALLED;
        }
    }

    @Override
    public String getContainerIDbyName(String name) {
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();

        for (Container cont : containers) {
            String[] containerNames = cont.getNames();
            for (String containerName : containerNames) {
                if (containerName.equals("/" + name)) { // docker API seems to put a '/' in front of the names
                    return cont.getId();
                }
            }
        }

        return "";
    }

    @Override
    public void stopContainer(String id) {
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }
        this.dockerClient.stopContainerCmd(id).exec();
    }

    @Override
    public void startContainer(String id) {
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }
        try {
            this.dockerClient.startContainerCmd(id).exec();
        } catch (Exception e) {
            logger.error("Could not start container {}. It could be already running or not exist at all", id);
        }
    }

    @Override
    public void deleteContainer(String id) {
        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }
        this.dockerClient.removeContainerCmd(id).exec();
    }

    @Override
    public void pullImage(String imageName) {
        pullImage(imageName, 120);
    }

    @Override
    public void pullImage(String imageName, int timeOutSecconds) {
        try {
            this.dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback())
                    .awaitCompletion(timeOutSecconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Image download failed", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String pullImageAndCreateContainer(ContainerDescriptor containerDescription) {

        boolean imageStatus = doesImageExist(containerDescription);

        logger.info("Image Does {} exist: {}", containerDescription.getContainerImage(), imageStatus);

        if (imageStatus) {
            try {
                containerDescription.setContainerId(createContainer(containerDescription));

            } catch (Exception e) {
                logger.error("could not create container {}:{}", containerDescription.getContainerName(), e);
            }

        } else {
            imagePullHelper(containerDescription, this.currentConfig.isRepositoryEnabled());
        }

        return containerDescription.getContainerId();
    }

    /**
     *
     * Helper method for pulling containers before creating containers. When using
     * an authenticated repo, docker-java will only try to check the authenticated
     * repo. This method will first check the authenticated repo and attempt to
     * pull, if no luck it will then pull from docker hub.
     *
     * @param containerDescription
     */
    private void imagePullHelper(ContainerDescriptor containerDescription, boolean withAuth) {

        try {
            containerDescription.setContainerId(""); // clear container ID
            logger.info("Attempting to pull image: {}. \n Authentication is set to: {}",
                    containerDescription.getContainerName(), withAuth);
            PullImageCmd pullRequest = this.dockerClient.pullImageCmd(containerDescription.getContainerImage())
                    .withTag(containerDescription.getContainerImageTag());

            if (withAuth) {
                pullRequest.withAuthConfig(this.dockerAuthConfig);
            }

            pullRequest.exec(new PullImageResultCallback() {

                @Override
                public void onNext(PullResponseItem item) {
                    super.onNext(item);
                    logger.info("Downloading: {}", item.getStatus());
                }

            }).awaitCompletion();
            logger.info("Creating container {}", containerDescription.getContainerName());

            if (containerDescription.getContainerId().isEmpty()) {
                containerDescription.setContainerId(createContainer(containerDescription));

            }
        } catch (InterruptedException e) {
            logger.error("error pulling {}: {}", containerDescription.getContainerName(), e);
            Thread.currentThread().interrupt();
        }
    }

    private String createContainer(ContainerDescriptor containerDescription) {
        String finalContainerID = "";
        if (!testConnection()) {
            throw new IllegalStateException("failed to reach docker engine");
        }

        if (containerDescription == null || containerDescription.getContainerImage() == null
                || containerDescription.getContainerImageTag() == null) {
            throw new IllegalStateException("failed to create container, null containerImage passed");
        }

        String containerImageFullString = String.format("%s:%s", containerDescription.getContainerImage(),
                containerDescription.getContainerImageTag());

        try (CreateContainerCmd containerBase = this.dockerClient.createContainerCmd(containerImageFullString)) {
            CreateContainerCmd commandBuilder = containerBase;

            if (containerDescription.getContainerName() != null) {
                commandBuilder = commandBuilder.withName(containerDescription.getContainerName());
            }

            HostConfig configuration = new HostConfig();

            commandBuilder = containerEnviromentVariablesHandler(containerDescription, commandBuilder);

            // Host Configuration Related
            configuration = containerVolumeMangamentHandler(containerDescription, configuration);

            configuration = containerDevicesHandler(containerDescription, configuration);

            configuration = containerPortManagementHandler(containerDescription, configuration);

            if (containerDescription.getContainerPrivileged() != null
                    && containerDescription.getContainerPrivileged()) {
                configuration = configuration.withPrivileged(containerDescription.getContainerPrivileged());
            }

            finalContainerID = commandBuilder.withHostConfig(configuration).exec().getId();

            commandBuilder.close();
            containerDescription.setContainerState(ContainerStates.ACTIVE);

        } catch (Exception e) {
            logger.error("failed to create container: {}", e.toString());
            containerDescription.setContainerState(ContainerStates.FAILED);
        }

        return finalContainerID;
    }

    private HostConfig containerPortManagementHandler(ContainerDescriptor containerDescription,
            HostConfig commandBuilder) {

        if (containerDescription.getContainerPortsInternal() != null
                && containerDescription.getContainerPortsExternal() != null
                && containerDescription.getContainerPortsExternal().length == containerDescription
                        .getContainerPortsInternal().length) {
            List<ExposedPort> exposedPorts = new LinkedList<>();
            Ports portbindings = new Ports();

            for (int index = 0; index < containerDescription.getContainerPortsInternal().length; index++) {

                ExposedPort tempExposedPort = new ExposedPort(containerDescription.getContainerPortsInternal()[index]);
                exposedPorts.add(tempExposedPort);
                portbindings.bind(tempExposedPort,
                        Binding.bindPort(containerDescription.getContainerPortsExternal()[index]));
            }

            commandBuilder.withPortBindings(portbindings);

        } else {
            logger.error("portsExternal and portsInternal must be int[] of the same size or they do not exist: {}",
                    containerDescription.getContainerName());
        }

        return commandBuilder;
    }

    private CreateContainerCmd containerEnviromentVariablesHandler(ContainerDescriptor containerDescription,
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

    private HostConfig containerVolumeMangamentHandler(ContainerDescriptor containerDescription,
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

    private HostConfig containerDevicesHandler(ContainerDescriptor containerDescription, HostConfig hostConfiguration) {

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

    private boolean doesImageExist(ContainerDescriptor containerDescription) {

        if (!testConnection()) {
            throw new IllegalStateException(UNABLE_TO_CONNECT_TO_DOCKER_CLI);
        }

        List<Image> images = this.dockerClient.listImagesCmd().exec();

        for (Image image : images) {
            if (image.getRepoTags() != null) {
                for (String tags : image.getRepoTags()) {
                    if (tags.contains(containerDescription.getContainerImage() + ":"
                            + containerDescription.getContainerImageTag())) {
                        return true;
                    }
                }

            }
        }

        return false;
    }

    private void cleanUpDocker() {

        if (testConnection()) {
            cleanUpAllMicroservices();
            disconnect();
        }
    }

    private void enqueueContainer(ContainerDescriptor containerDescription, List<ContainerDescriptor> containerList) {
        logger.info("Starting {} Microservice", containerDescription.getContainerName());

        // try to grab existing container using service name as UUID
        ContainerDescriptor containerDescriptorFromQueue = ContainerDescriptor
                .findByName(containerDescription.getContainerName(), containerList);

        // Handle Container Queuing
        if (containerDescriptorFromQueue == null) {
            containerList.add(containerDescription);
        } else {
            if (!ContainerDescriptor.equals(containerDescriptorFromQueue, containerDescription)) {
                stopContainer(containerDescription);
                containerList.remove(containerDescriptorFromQueue);
                containerList.add(containerDescriptorFromQueue);
            }
        }
    }

    private String deqeueContainer(ContainerDescriptor containerDescription, List<ContainerDescriptor> containerList) {
        ContainerDescriptor tempSD = ContainerDescriptor.findByName(containerDescription.getContainerName(),
                containerList);
        if (containerList.contains(tempSD)) {
            containerList.remove(tempSD);
            return tempSD.getContainerId();
        }
        return containerDescription.getContainerId();
    }

    @Override
    public void startContainer(ContainerDescriptor container) {
        container.setContainerState(ContainerStates.STARTING);

        if (testConnection()) {
            enqueueContainer(container, this.runningContainers);

            try {
                container.setContainerId(getContainerIDbyName(container.getContainerName()));
            } catch (Exception e) {
                logger.error("Failed to get container name by ID.", e);
            }

            if (container.getContainerId().isEmpty()) {
                try {
                    container.setContainerId(pullImageAndCreateContainer(container));
                    startContainer(container.getContainerId());
                    container.setContainerState(ContainerStates.ACTIVE);
                    logger.info("Container Started Successfully");
                } catch (Exception e) {
                    logger.error("Failed to pull and create container.", e);
                }
            } else {
                logger.info("{}  Microservice Already exists, starting container", container.getContainerImage());
                try {
                    startContainer(container.getContainerId());
                    container.setContainerState(ContainerStates.ACTIVE);
                } catch (NotModifiedException ex) {
                    logger.info("Container Already running.");
                }
            }

        } else {
            enqueueContainer(container, this.containerQueue);
        }

    }

    @Override
    public void stopContainer(ContainerDescriptor container) {
        container.setContainerState(ContainerStates.STOPPING);
        if (!this.currentConfig.isEnabled()) {
            container.setContainerId(deqeueContainer(container, this.containerQueue));

        }
        container.setContainerId(deqeueContainer(container, this.runningContainers));

        if (!container.getContainerId().isEmpty()) {

            try {
                logger.info("Stopping {} Microservice", container.getContainerName());
                stopContainer(container.getContainerId());
            } catch (Exception e) {
                logger.error("Failed to stop {} Microservice", container.getContainerName());
            }

            if (Boolean.FALSE.equals(container.getIsEsfManaged())) {
                // container is not ESF Managed, and thus should only be stopped and not deleted.
                return;
            }

            try {
                logger.info("Deleting {} Microservice", container.getContainerName());
                deleteContainer(container.getContainerId());
                logger.info("Succsesfully deleted {} Microservice", container.getContainerName());
                container.setContainerId("");
            } catch (Exception e) {
                logger.error("Failed to delete {} Microservice", container.getContainerName());
            }

        } else {
            logger.error("Microservice {} does not exist", container.getContainerName());
        }

    }

    @Override
    public void registerContainer(ContainerDescriptor container) {
        enqueueContainer(container, this.registeredContainers);

    }

    @Override
    public void unregisterContainer(ContainerDescriptor container) {
        deqeueContainer(container, this.registeredContainers);
    }

    @Override
    public List<ContainerDescriptor> listRegisteredContainers() {
        // Add all registered containers
        List<ContainerDescriptor> cds = new LinkedList<>(this.registeredContainers);

        for (ContainerDescriptor container : listByContainerDescriptor()) {
            boolean unique = true;
            for (ContainerDescriptor listCotnainer : cds) {
                if (container.getContainerId().equals(listCotnainer.getContainerId())) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                cds.add(container);
            }
        }

        return cds;
    }

    private void cleanUpAllMicroservices() {
        List<ContainerDescriptor> temp = new LinkedList<>(this.runningContainers);
        for (ContainerDescriptor containerName : temp) {
            stopContainer(containerName);
        }
        // re-queue all stopped containers, so that when Docker is started up again all
        // containers will start up again
        this.containerQueue = temp;
    }

    private void startUpAllQueuedMicroservices() {
        for (ContainerDescriptor containerName : this.containerQueue) {
            startContainer(containerName);
        }
    }

    private void populateRunningQueue() {
        Collections.addAll(this.runningContainers, listByContainerDescriptor());
    }

    public boolean connect() {
        if (this.currentConfig.getHostUrl() == null) {
            return false;
        }
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(this.currentConfig.getHostUrl()).withDockerCertPath("/home/user/.docker").build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig()).build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);

        if (this.currentConfig.isRepositoryEnabled()) {
            logIntoRemoteRepository();
        }

        return testConnection();
    }

    private void disconnect() {
        if (testConnection()) {
            try {
                this.dockerClient.close();
            } catch (IOException e) {
                logger.error(e.toString());
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

    private void logIntoRemoteRepository() {

        // Decode password
        String decodedPassword;
        try {
            decodedPassword = String
                    .valueOf(this.cryptoService.decryptAes(this.currentConfig.getRepositoryPassword().toCharArray()));
        } catch (Exception e) {
            logger.error("Failed to decode password: {0}", e);
            decodedPassword = this.currentConfig.getRepositoryPassword();
        }

        if (this.currentConfig.getRepositoryUrl().isEmpty()) {
            logger.info("Attempting to sign into Docker-Hub");
            this.dockerAuthConfig = new AuthConfig().withUsername(this.currentConfig.getRepositoryUsername())
                    .withPassword(decodedPassword);
        } else {
            logger.info("Attempting to sign into repo: {}", this.currentConfig.getRepositoryUrl());
            this.dockerAuthConfig = new AuthConfig().withUsername(this.currentConfig.getRepositoryUsername())
                    .withPassword(decodedPassword).withRegistryAddress(this.currentConfig.getRepositoryUrl());
        }
    }

}