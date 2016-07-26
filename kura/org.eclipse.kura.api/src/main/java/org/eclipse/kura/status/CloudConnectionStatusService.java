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
package org.eclipse.kura.status;

/**
 * The CloudConnectionStatusService is responsible for updating the user on the connection status of the framework.
 * Default implementation will link the status to a LED or to the log file.<br/>
 * The service stores an internal list of {@link CloudConnectionStatusComponent} which represents all the elements
 * that are responsible of notifying the connection status.<br/><br/>
 * 
 * Each {@link CloudConnectionStatusComponent} is assigned with a priority and a status ({@link CloudConnectionStatusEnum}).
 * The Service shows the status of the highest priority CloudConnectionStatusComponent registered in the list.<br/>
 * <br/>
 * In order to use the service, a class must implement the {@link CloudConnectionStatusComponent} interface and register itself
 * in the Service component registry using the {@code register(CloudConnectionStatusComponent c)} method.<br/>
 * <br/>
 * Unregistering the class from the Service registry will trigger a status switch to the previous priority found in the Service
 * component registry.<br/>
 * <br/>
 * A call to {@code updateStatus(...)} will trigger an internal status change for the relevant component, and the Service will
 * trigger a status change for the highest priority component only if needed.
 * 
 */
public interface CloudConnectionStatusService {

	/**
	 * Maximum priority for the notification. There should be only one StatusDisplayComponent at the same time
	 * using this priority.
	 */
	public static final int PRIORITY_MAX 		= Integer.MAX_VALUE;
	
	/**
	 * Priorities are evaluated in ascending order. 
	 * The Service will use the status of the registered component with highest priority
	 */
	public static final int PRIORITY_CRITICAL 	= 400;	
	public static final int PRIORITY_HIGH 		= 300;
	public static final int PRIORITY_MEDIUM 	= 200;
	public static final int PRIORITY_LOW 		= 100;
	
	/**
	 * Minimum priority for the notification. There should be at least and only one StatusDisplayComponent at the same time
	 * using this priority.
	 */
	public static final int PRIORITY_MIN 		= Integer.MIN_VALUE;
	
	/**
	 * Registers a {@link CloudConnectionStatusComponent} in the component registry of the Service
	 * @param component CloudConnectionStatusComponent to be registered in the registry
	 */
	public void register(CloudConnectionStatusComponent component);
	
	/**
	 * Unregisters a {@link CloudConnectionStatusComponent} from the component registry of the Service
	 * @param component CloudConnectionStatusComponent to be unregistered from the registry
	 */
	public void unregister(CloudConnectionStatusComponent component);

	/**
	 * Updates the status of a {@link CloudConnectionStatusComponent} in the registry.
	 * Implementation should also set the internal status of the CloudConnectionStatus component so to persist it.
	 * @param component {@link CloudConnectionStatusComponent} for which the status has to be changed
	 * @param status {@link CloudConnectionStatusEnum} representing the new status of the component
	 * @return false if an error occurs, true otherwise
	 */
	public boolean updateStatus(CloudConnectionStatusComponent component, CloudConnectionStatusEnum status);
}
