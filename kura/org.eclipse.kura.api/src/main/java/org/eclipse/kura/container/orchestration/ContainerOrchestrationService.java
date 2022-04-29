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

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.listener.ContainerOrchestrationServiceListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for managing the container lifecycle and interacting with the
 * container manager daemon.
 *
 * @since 2.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ContainerOrchestrationService {

    /**
     * Lists all available containers by id only, like running the cli command 'docker ps -a'
     *
     * @return a list of string objects representing the container IDs
     */
    public List<String> listContainersIds();

    /**
     * Lists all available containers returning the corresponding
     * {@link ContainerInstanceDescriptor}s
     *
     * @return a list of {@link ContainerInstanceDescriptor}s representing the
     *         available containers
     */
    public List<ContainerInstanceDescriptor> listContainerDescriptors();

    /**
     * Lists all available images, returning the corresponding
     * {@link ImageInstanceDescriptor}s
     *
     * @return a list of {@link ContainerInstanceDescriptor}s representing the
     *         available containers
     * @since 2.4
     */
    public List<ImageInstanceDescriptor> listImageInstanceDescriptors();

    /**
     * Deletes Image from container engine. Takes ImageID String as parameter. Will
     * throw an error if image is being used by a container.
     *
     * @param imageId string parameter that identifies the image to be deleted
     * @throws KuraException if the image is used by a container or if the image
     *                       deletion process fails
     * @since 2.4
     */
    public void deleteImage(String imageId) throws KuraException;

    /**
     * Allows to pull the required image, using the specified tag and credentials.
     * The image will be downloaded respecting the configured timeout in seconds.
     *
     * @param imageConfig an ImageConfiguration object which contains info such as
     *                    image name, tag, pull timeout in seconds and registry
     *                    credentials.
     * 
     * @throws KuraException        if the pull operation fails
     * @throws InterruptedException
     * @since 2.4
     */
    public void pullImage(ImageConfiguration imageConfig) throws KuraException, InterruptedException;

    /**
     * Allows to pull the required image, using the specified tag and credentials.
     * The image will be downloaded respecting the configured timeout in seconds.
     *
     * @param imageName           the image name to be used. Must not be null.
     * @param imageTag            a string representing the image tag. Must not be
     *                            null.
     * @param timeOutSeconds      a non negative integer representing the image
     *                            download timeout in seconds
     * @param registryCredentials an optional that can contain the registry URL and
     *                            credentials for authentication
     * @throws KuraException        if the pull operation fails
     * @throws InterruptedException
     */
    public void pullImage(String imageName, String imageTag, int timeOutSeconds,
            Optional<RegistryCredentials> registryCredentials) throws KuraException, InterruptedException;

    /**
     * Returns the id of the container corresponding to the specified name. If no
     * container can be found an {@link Optional#empty(} result is returned.
     *
     * @param name the string representing the container name. Must not be null
     * @return an {@link Optional} value that will contain the container ID, if the
     *         container exists. Otherwise and {@link Optional#empty()}
     */
    public Optional<String> getContainerIdByName(String name);

    /**
     * Starts a container identified by the values provided in a not null
     * {@link ContainerConfiguration} object. If the requested image does not
     * exists, it will be downloaded. A String representing the container ID will be
     * returned if the operation of container creation and start succeed.
     *
     * @param containerConfiguration
     * @return a String representing the container ID created
     * @throws KuraException is thrown if the image pull or container creation and
     *                       start fail
     */
    public String startContainer(ContainerConfiguration containerConfiguration)
            throws KuraException, InterruptedException;

    /**
     * Starts a container identified by the specified ID
     *
     * @param id the ID of an already existing container. Must not be null
     * @throws KuraException if the container starting fails
     */
    public void startContainer(String id) throws KuraException;

    /**
     * Stops a container identified by the specified ID
     *
     * @param id the ID of an already existing container. Must not be null
     * @throws KuraException if the container stopping fails
     */
    public void stopContainer(String id) throws KuraException;

    /**
     * Deletes a container identified by the specified ID
     *
     * @param id the ID of an already existing container. Must not be null
     * @throws KuraException if the container removal fails
     */
    public void deleteContainer(String id) throws KuraException;

    /**
     * Adds a new {@link ContainerOrchestrationServiceListener}
     *
     * @param dockerListener
     * @param containerName
     */
    public void registerListener(ContainerOrchestrationServiceListener dockerListener);

    /**
     * Removes the {@link ContainerOrchestrationServiceListener} specified as
     * parameter
     *
     * @param dockerListener
     */
    public void unregisterListener(ContainerOrchestrationServiceListener dockerListener);
}
