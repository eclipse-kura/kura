/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.cloud.factory;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataTransportService;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.component.ComponentContext;

/**
 * A CloudServiceFactory represents an OSGi Declarative Service Component
 * which registers {@link CloudService}S in the framework.
 * The Component creates multiple component instances upon reception of a configuration
 * created through the Configuration Service.
 * <br>
 * It provides a CloudService implementation that can be used to connect to a specific Cloud platform.
 * <br>
 * Typically, each CloudService created by a CloudServiceFactory
 * establishes and manages its own connection, for example an Mqtt connection.
 * <br>
 * Multiple CloudServiceFactory services can be registered in the framework to support multiple simultaneous
 * connections to different Cloud platforms.
 * <br>
 * Kura provides a default CloudServiceFactory implementation and creates a default CloudService.
 * <br>
 * A CloudServiceFactory manages the construction of a CloudService and the services it depends on.
 * While the same can be achieved through the {@link ConfigurationService},
 * CloudServiceFactory simplifies this process and offers more control.
 * <br>
 * For example, in a stack architecture with CloudService at the top of the stack
 * and where lower layers are also components,
 * an implementation of CloudServiceFactory could create new configurations
 * for all the stack layers thus constructing a new whole stack instance.
 * <br>
 * The Kura {@link CloudService}/{@link CloudService}/{@link DataTransportService}
 * cloud stack represents an example of the above architecture
 * and can serve as a reference implementation for alternative Cloud stacks.
 * <br>
 * In order to leverage the Kura configuration persistence in snapshot files,
 * an implementation will use the {@link ConfigurationService}
 * to create component configurations.
 *
 * @since 1.0.8
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * 
 * @deprecated Please consider using {@link CloudConnectionFactory}
 */
@ProviderType
@Deprecated
public interface CloudServiceFactory {

    /**
     * The name of the property set in a @{link CloudService} configuration created
     * through {@link #createConfiguration}.
     * The property is set in the cloud service instance to relate it with the Factory that generated the whole cloud
     * stack.
     *
     * @since 1.1.0
     */
    public static final String KURA_CLOUD_SERVICE_FACTORY_PID = "kura.cloud.service.factory.pid";

    /**
     * Returns the factory PID of the OSGi Factory Component represented by this CloudServiceFactory.
     *
     * @return a String representing the factory PID of the Factory Component.
     */
    String getFactoryPid();

    /**
     * Creates a {@link CloudService} instance and initializes its configuration with the defaults
     * expressed in the Metatype of the target component factory providing the CloudService.
     * <br>
     * Implementation will normally rely on {@link ConfigurationService#createFactoryConfiguration}
     * to perform the actual creation of the component instance and the persistence of the component configuration.
     * <br>
     * The created CloudService instance will have its <i>kura.service.pid</i> property
     * set to the value provided in the <i>pid</i> parameter.
     * <br>
     * Kura apps can look up the created CloudService instance through {@link ComponentContext#locateServices}
     * by filtering on the <i>kura.service.pid</i> property.
     * <br>
     * Likely, Kura apps will rely on OSGi Declarative Services to have their CloudService dependencies satisfied based
     * on a <i>target</i> filter on the value of the property <i>kura.service.pid</i>
     * in their component definition.
     * <br>
     * In the following example a Kura app declares two dependencies on CloudServiceS whose PIDs are
     * <i>myCloudService</i> and <i>anotherCloudService</i>:
     *
     * <pre>
     * &lt;reference name="myCloudServiceReference"
     *              policy="static"
     *              bind="setMyCloudService"
     *              unbind="unsetMyCloudService"
     *              cardinality="1..1"
     *              interface="org.eclipse.kura.cloud.CloudService"/&gt;
     * &lt;property  name="myCloudServiceReference.target"
     *              type="String"
     *              value="(kura.service.pid=myCloudService)"/&gt;
     *
     * &lt;reference name="anotherCloudServiceReference"
     *              policy="static"
     *              bind="setAnotherCloudService"
     *              unbind="unsetAnotherCloudService"
     *              cardinality="1..1"
     *              interface="org.eclipse.kura.cloud.CloudService"/&gt;
     * &lt;property  name="anotherCloudServiceReference.target"
     *              type="String"
     *              value="(kura.service.pid=anotherCloudService)"/&gt;
     * </pre>
     *
     * @param pid
     *            the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
     * @throws KuraException
     */
    void createConfiguration(String pid) throws KuraException;

    /**
     * Returns the list of <i>kura.service.pid</i>s that compose the cloud stack associated with the provided
     * <i>kura.service.pid</i> of the factory component configuration.
     *
     * @param pid
     *            the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
     * @return List&lt;String&gt;, the list of <i>kura.service.pid</i>s associated with the specified factory component
     *         configuration.
     * @throws KuraException
     *             if the specified <i>kura.service.pid</i> is not correct or compliant with what the factory
     *             implementation expects
     * @since 1.1.0
     */
    List<String> getStackComponentsPids(String pid) throws KuraException;

    /**
     * Deletes a previously created configuration deactivating the associated {@link CloudService} instance.
     *
     * @param pid
     *            the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
     * @throws KuraException
     */
    void deleteConfiguration(String pid) throws KuraException;

    /**
     * Return a set of services managed by this factory
     * <br>
     * It is up to the factory how it does assembles this. The PIDs returned by this list must be the PIDs assigned
     * to the OSGi service property <i>kura.service.pid</i> and it must be possible to pass all those results into
     * the method {@link #getStackComponentsPids(String)} of the same factory instance.
     * <br>
     * The IDs returned by this method must not necessarily point to registered OSGi services. But if they do, they must
     * point only to instances of the {@link CloudService}.
     *
     * @return the set of services, never returns {@code null}
     * @throws KuraException
     *
     * @since 1.1.0
     */
    Set<String> getManagedCloudServicePids() throws KuraException;
}
