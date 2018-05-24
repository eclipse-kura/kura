/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.factory;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link CloudConnectionFactory} is responsible to register cloud connection instances in the
 * framework.
 * The Component creates multiple component instances upon reception of a configuration
 * created through the Configuration Service.
 *
 * It provides all the implementations that can be used to connect to a specific Cloud platform.
 *
 * Typically, a {@link CloudConnectionFactory} creates the {@link CloudEndpoint} and the {@link CloudConnectionManager}
 * that are used to
 * establish and manage the connection to a cloud platform, for example an Mqtt connection.
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
     * Creates one or more service instances and initializes its configuration with the defaults
     * expressed in the Metatype of the target component factories.
     *
     * The created instances will have their {@code kura.service.pid} properties
     * set to the value provided in the {@code pid} parameter.
     *
     * @param pid
     *            the Kura persistent identifier ({@code kura.service.pid}) of the service
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
     *            the Kura persistent identifier, {@code kura.service.pid}
     * @throws KuraException
     *             if the provided {@code kura.service.pid} is incorrect or the delete operation fails.
     */
    public void deleteConfiguration(String pid) throws KuraException;

    /**
     * Returns a set of services managed by this factory
     * It is up to the factory to specify how to assembles the result. The PIDs returned by this list must be the PIDs
     * assigned to the OSGi service property {@code kura.service.pid} and it must be possible to pass all those results
     * into the method {@link #getStackComponentsPids(String)} of the same factory instance.
     *
     * The IDs returned by this method must not necessarily point to registered OSGi services.
     *
     * @return the set of services or an empty set.
     * @throws KuraException
     */
    public Set<String> getManagedCloudConnectionPids() throws KuraException;

}
