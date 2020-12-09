/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.cloudconnection.factory;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link CloudConnectionFactory} is responsible to register {@link org.eclipse.kura.cloudconnection.CloudEndpoint}
 * instances in the framework.
 * The Component creates multiple component instances upon reception of a configuration
 * created through the Configuration Service.
 *
 * It provides all the implementations that can be used to connect to a specific Cloud platform.
 *
 * A {@link CloudConnectionFactory} must create a {@link org.eclipse.kura.cloudconnection.CloudEndpoint} and,
 * eventually, a {@link org.eclipse.kura.cloudconnection.CloudConnectionManager} that are used to establish and manage
 * the connection to a cloud platform, for example an Mqtt connection.
 *
 * Multiple {@link CloudConnectionFactory} services can be registered in the framework to support multiple simultaneous
 * connections to different Cloud platforms.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
@ProviderType
public interface CloudConnectionFactory {

    /**
     * The name of the property set in the instance configuration created
     * through {@link #createConfiguration}.
     * The property is set in the instance to relate it with the Factory that generated
     * the whole cloud stack.
     */
    public static final String KURA_CLOUD_CONNECTION_FACTORY_PID = "kura.cloud.connection.factory.pid";

    /**
     * Returns the factory PID of the OSGi Factory Component represented by this {@link CloudConnectionFactory}.
     *
     * @return a String representing the factory PID of the Factory Component.
     */
    public String getFactoryPid();

    /**
     * This method creates a CloudEndpoint instance and, eventually, more service instances that are necessary to
     * identify and the manage the endpoint and the connection. It initializes the configuration of the created services
     * with the defaults expressed in the Metatype of the target component factories.
     *
     * The created Cloud Endpoint instance will have its {@code kura.service.pid} property
     * set to the value provided in the {@code pid} parameter.
     *
     * @param pid
     *            the Kura persistent identifier ({@code kura.service.pid}) of the Cloud Endpoint service
     *            instance created by this factory.
     * @throws KuraException
     *             an exception is thrown in case the creation operation fails
     */
    public void createConfiguration(String pid) throws KuraException;

    /**
     * Returns the list of {@code kura.service.pid}s that compose the cloud connection associated with the provided
     * {@code kura.service.pid}.
     *
     * @param pid
     *            the Kura persistent identifier, {@code kura.service.pid}
     * @return the {@link List} of {@code kura.service.pid}s related to the provided {@code pid}.
     * @throws KuraException
     *             if the specified {@code kura.service.pid} is incorrect.
     */
    public List<String> getStackComponentsPids(String pid) throws KuraException;

    /**
     * Deletes a previously created configuration deactivating the associated instances.
     *
     * @param pid
     *            the Kura persistent identifier, {@code kura.service.pid} of a Cloud Endpoint
     * @throws KuraException
     *             if the provided {@code kura.service.pid} is incorrect or the delete operation fails.
     */
    public void deleteConfiguration(String pid) throws KuraException;

    /**
     * Returns a set of {@code kura.service.pid} that corresponds to the Cloud Endpoint services managed by this
     * factory.
     *
     * @return the set of services or an empty set.
     * @throws KuraException
     */
    public Set<String> getManagedCloudConnectionPids() throws KuraException;

}
