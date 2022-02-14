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
package org.eclipse.kura.container.orchestration.provider;

import java.util.List;
import org.eclipse.kura.KuraException;

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
     *
     * Registers Container into internal container tracking mechanism.
     *
     * @param containerDescriptor
     */
    public void registerContainer(ContainerDescriptor container);

    /**
     * Un-Registers Container into internal container tracking mechanism.
     *
     * @param containerDescriptor
     */
    public void unregisterContainer(ContainerDescriptor container);

    /**
     * lists all tracked docker containers.
     *
     * @param containerDescriptor
     */
    public List<ContainerDescriptor> listRegisteredContainers();
    
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
