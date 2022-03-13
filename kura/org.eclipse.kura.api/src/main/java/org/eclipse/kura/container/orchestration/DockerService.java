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
import org.eclipse.kura.container.orchestration.listener.DockerServiceListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for managing the container lifecycle and interacting with the container manager daemon.
 *
 * @since 2.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface DockerService {

    /**
     * Lists all available containers by id only, like running 'Docker ps -a'
     *
     * @return a list of string objects representing the container IDs
     */
    public List<String> listContainersIds();

    /**
     * Lists all available containers returning the corresponding {@link ContainerDescriptor}s
     *
     * @return a list of {@link ContainerDescriptor}s representing the available containers
     */
    public List<ContainerDescriptor> listContainerDescriptors();

    /**
     * Allows to pull the required image, using the specified tag. The image will be downloaded respecting the
     * configured timeout in seconds.
     *
     * @param imageName
     * @param imageTag
     * @param timeOutSeconds
     * @throws KuraException
     *             if the pull operation fails
     */
    public void pullImage(String imageName, String imageTag, int timeOutSeconds) throws KuraException;

    /**
     * Returns the id of the container corresponding to the specified name. If no container can be found an
     * {@link Optional#empty(} result is returned.
     *
     * @param name
     *            the string representing the container name
     * @return an {@link Optional} value that will contain the container ID, if the container exists. Otherwise and
     *         {@link Optional#empty()}
     */
    public Optional<String> getContainerIdByName(String name);

    /**
     * Starts a container identified by the values provided in the {@link ContainerDescriptor} object. If the requested
     * image does not exists, it will be downloaded. A String representing the container ID will be returned if the
     * operation of container creation and start succeed.
     *
     * @param containerDescriptor
     * @return a String representing the container ID created
     * @throws KuraException
     *             is thrown if the image pull or container creation and start fail
     */
    public String startContainer(ContainerDescriptor containerDescriptor) throws KuraException;

    /**
     * Starts a container identified by the specified ID
     *
     * @param id
     *            the ID of an already existing container
     * @throws KuraException
     *             if the container starting fails
     */
    public void startContainer(String id) throws KuraException;

    /**
     * Stops a container identified by the specified ID
     *
     * @param id
     *            the ID of an already existing container
     * @throws KuraException
     *             if the container stopping fails
     */
    public void stopContainer(String id) throws KuraException;

    /**
     * Deletes a container identified by the specified ID
     *
     * @param id
     *            the ID of an already existing container
     * @throws KuraException
     *             if the container removal fails
     */
    public void deleteContainer(String id) throws KuraException;

    /**
     * Adds a new {@link DockerServiceListener} and tracks the corresponding containerName
     *
     * @param dockerListener
     * @param containerName
     */
    public void registerListener(DockerServiceListener dockerListener, String containerName);

    /**
     * Removes the {@link DockerServiceListener} specified as parameter
     *
     * @param dockerListener
     */
    public void unregisterListener(DockerServiceListener dockerListener);
}
