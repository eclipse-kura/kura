/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates. All rights reserved.
 *******************************************************************************/
package org.eclipse.kura.container.orchestration.provider;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;

/**
 * API for directly accessing docker containers.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface DockerService {

    /**
     * Lists all available containers, like running 'Docker ps -a'
     *
     * @return
     */
    public String[] listContainers();

    /**
     * Lists all available containers by id only, like running 'Docker ps -a'
     *
     * @return
     */
    public String[] listContainersByID();

    /**
     * Lists all running containers by string of ID's
     *
     * @return
     */
    public ContainerDescriptor[] listByContainerDescriptor();

    /**
     * Finds the id of the container with specified name. if cannot be found empty string will be returned.
     *
     * @param name
     * @return
     */
    public String getContainerIDbyName(String name);

    /**
     *
     * @param containerDescriptor
     * @throws KuraException
     */
    public void startContainer(ContainerDescriptor containerDescriptor) throws KuraException;

    /**
     * Starts a container identified by the specified id
     *
     * @param id
     */
    public void startContainer(String id) throws KuraException;

    /**
     *
     * @param containerDescriptor
     * @throws KuraException
     */
    public void stopContainer(ContainerDescriptor containerDescriptor) throws KuraException;

    /**
     * Stops a running container
     *
     * @param id
     * @throws KuraException
     */
    public void stopContainer(String id) throws KuraException;

    /**
     * Equivalent to, docker start <container id>
     *
     * @param id
     * @throws KuraException
     */
    public void deleteContainer(String id) throws KuraException;

    /**
     *
     * @param imageName
     * @throws KuraException
     */
    public void pullImage(String imageName) throws KuraException;

    /**
     *
     * @param imageName
     * @param timeOutSecconds
     * @throws KuraException
     */
    public void pullImage(String imageName, int timeOutSecconds) throws KuraException;

    /**
     *
     * @param containerDescription
     * @return
     * @throws KuraException
     */
    public String pullImageAndCreateContainer(ContainerDescriptor containerDescription) throws KuraException;

}
