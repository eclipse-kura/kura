/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;

/**
 * The Configuration Service is used to manage the configuration of ConfigurableComponents.
 * It works in concert with the OSGi ConfigurationAdmin and the OSGi MetaTypeService services.
 * What it provides over the native OSGi services is the ability to easily access the current
 * configuration of a ConfigurableComponent together with its full ObjectClassDefintion,
 * the ability to take snapshots of the current configuration of all registered 
 * ConfigurableComponents or rollback to older snapshots and, finally, the ability to access 
 * and manage configurations remotely through the CloudService. 
 * <br>    
 * The Configuration Service operates on a subset of all the Components registered under the OSGi container.
 * It tracks all OSGi Components which implement the {@see ConfigurableComponent} marker interface.
 * When a ConfigurableComponent is registered, the Configuration Service will call its "update"
 * method with the latest saved configuration as returned the ConfigurationAdmin or, if none
 * is available, with the Configuration properties fabricated from the default attribute values as 
 * specified in the ObjectClassDefinition of this service.
 * In OSGi terms, this process in similar to the Auto Configuration Service.
 * <b>The ConfigurationService assumes that Meta Type Information XML resource
 * for a given Declarative Service with name abc" to be stored under OSGI-INF/metatype/abc.xml.</b>
 * This is an extra restriction over the OSGi specification: the Meta Type Information XML resource
 * must be named as the name of the Declarative Service Component. 
 * <br>
 * The ConfigurationService has the ability to create a snapshot for the current configuration
 * of all the registered ConfigurableComponents. The snapshot is saved in the form of an
 * XML file stored under $KuraHome/snapshots/snapshot_epoch.xml where epoch is replaced
 * with the epoch timestamp at the time of the snapshot creation.
 * The ConfigurationService also has the ability to rollback the configuration of the
 * registered components taking them back to a previous stored snapshot. 
 * <br>
 * The ConfigurationService interacts with the CloudService to allow to access 
 * current configuration and to modify it remotely through messages sent and received via cloud.   
 */
public interface ConfigurationService 
{
	/**
	 * Return the PIDs (service's persistent identity) for all the services that 
	 * implements the ConfigurableComponent Maker interface and registered themselves
	 * with the container.
	 * The service's persistent identity is defined as the name attribute of the
	 * Component Descriptor XML file; at runtime, the same value is also available
	 * in the component.name and in the service.pid attributes of the Component Configuration. 
	 * @return list of PIDs for registered ConfigurableComponents
	 */
	public Set<String> getConfigurableComponentPids();

	
	/**
	 * Returns the list of ConfigurableComponents currently registered with the ConfigurationService.
	 * @return list of registered ConfigurableComponents
	 */
	public List<ComponentConfiguration> getComponentConfigurations() throws KuraException;


	
	/**
	 * Returns the ComponentConfiguration for the component identified with specified PID (service's persistent identity).
	 * The service's persistent identity is defined as the name attribute of the
	 * Component Descriptor XML file; at runtime, the same value is also available
	 * in the component.name and in the service.pid attributes of the Component Configuration. 
	 * @param pid The ID of the component whose configuration is requested. 
	 * @return ComponentConfiguration of the requested Component.
	 */
	public ComponentConfiguration getComponentConfiguration(String pid) throws KuraException;
	
	
	/**
	 * Updates the Configuration of the registered component with the specified pid.
	 * Using the OSGi ConfigurationAdmin, it retrieves the Configuration of the 
	 * component with the specified PID and then send an update using the 
	 * specified properties.
	 * <br>
	 * If the component to be updated is not yet registered with the ConfigurationService,
	 * it is first registered and then it is updated with the specified properties.
	 * Before updating the component, the specified properties are validated against
	 * the ObjectClassDefinition associated to the Component. The Configuration Service
	 * is fully compliant with the OSGi MetaType Information and the validation happens
	 * through the OSGi MetaType Service.
	 * <br>
	 * The Configuration Service is compliant with the OSGi MetaType Service so 
	 * it accepts all attribute types defined in the OSGi Compendium Specifications.
	 * <br>
	 * it accepts all 
	 * @param pid The ID of the component whose configuration is requested.
	 * @param properties Properties to be used as the new Configuration for the specified Component.
	 * @throws KuraException if the properties specified do not pass the validation of the ObjectClassDefinition
	 */
	public void updateConfiguration(String pid, Map<String,Object> properties)
		throws KuraException;


	public void updateConfigurations(List<ComponentConfiguration> configs)
		throws KuraException;
		
	/**
	 * Returns the ID of all the snapshots taken by the ConfigurationService.
	 * The snapshot ID is the epoch time at which the snapshot was taken.
	 * The snapshots are stored in the KuraHome/snapshots/ directory.
	 * This API will return all the snpashot files available in that location.
	 *  
	 * @return IDs of the snapshots available.
	 * @throws KuraException
	 */
	public Set<Long> getSnapshots() 
		throws KuraException;
	
	/**
	 * Loads a snapshot given its ID and return the component configurations stored in that snapshot.
	 * 
	 * @param sid - ID of the snapshot to be loaded
	 * @return List of ComponentConfigurations contained in the snapshot 
	 * @throws KuraException
	 */
	public List<ComponentConfiguration> getSnapshot(long sid)
		throws KuraException;
	
	
	/**
	 * Takes a new snapshot of the current configuration of all the registered ConfigurableCompoenents.
	 * It returns the ID of a snapshot as the epoch time at which the snapshot was taken. 
	 * @return the ID of the snapshot.
	 * @throws KuraException
	 */
	public long snapshot() 
		throws KuraException;

	/**
	 * Rolls back to the last saved snapshot if available.
	 * 
	 * @return the ID of the snapshot it rolled back to
	 * @throws KuraException if no snapshots are available or 
	 */
	public long rollback()
		throws KuraException;
	
	/**
	 * Rolls back to the specified snapshot id. 
	 * 
	 * @param id ID of the snapshot we need to rollback to
	 * @throws KuraException if the snapshot is not found
	 */
	public void rollback(long id) 
		throws KuraException;
}
