/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud.factory;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;

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
 * The Kura {@link CloudService}/{@link DataService}/{@link DataTransportService}
 * cloud stack represents an example of the above architecture
 * and can serve as a reference implementation for alternative Cloud stacks.
 * <br>
 * In order to leverage the Kura configuration persistence in snapshot files,
 * an implementation will use the {@link ConfigurationService}
 * to create component configurations.
 * <br>
 * The {@link CloudService} implementation class is also required
 * to implement the {@link ConfigurableComponent} interface.
 * 
 * @since {@link org.eclipse.kura.cloud.factory} 1.0.0
 */
public interface CloudServiceFactory {
	
	/**
	 * Returns the factory PID of the OSGi Factory Component represented by this CloudServiceFactory.
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
	 * In the following example a Kura app declares two dependencies on CloudServiceS whose PIDs are <i>myCloudService</i> and <i>anotherCloudService</i>:
	 * </br>
	 * <pre>
	 * &ltreference name="myCloudServiceReference"
     *              policy="static"
     *              bind="setMyCloudService"
     *              unbind="unsetMyCloudService"
     *              cardinality="1..1"
     *              interface="org.eclipse.kura.cloud.CloudService"/&gt
     * &ltproperty  name="myCloudServiceReference.target"
     *              type="String"
     *              value="(kura.service.pid=myCloudService)"/&gt
     *
     * &ltreference name="anotherCloudServiceReference"
     *              policy="static"
     *              bind="setAnotherCloudService"
     *              unbind="unsetAnotherCloudService"
     *              cardinality="1..1"
     *              interface="org.eclipse.kura.cloud.CloudService"/&gt
     * &ltproperty  name="anotherCloudServiceReference.target"
     *              type="String"
     *              value="(kura.service.pid=anotherCloudService)"/&gt
	 * </pre>
	 * 
	 * @param pid the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
	 * @throws KuraException
	 */
	void createConfiguration(String pid) throws KuraException;
	
	/**
	 * Deletes a previously created configuration deactivating the associated {@link CloudService} instance.
	 * 
	 * @param pid the Kura persistent identifier, <i>kura.service.pid</i>, of the factory component configuration.
	 * @throws KuraException
	 */
	void deleteConfiguration(String pid) throws KuraException;
}
