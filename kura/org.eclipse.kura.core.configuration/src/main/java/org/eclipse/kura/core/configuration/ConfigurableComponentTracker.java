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
package org.eclipse.kura.core.configuration;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceTracker to track all the ConfigurabaleComponents.
 * When the ConfigurableComponet is found it is then registered to the ConfigurationService.
 */

@SuppressWarnings("rawtypes")
public class ConfigurableComponentTracker extends ServiceTracker
{
	private static final Logger s_logger = LoggerFactory.getLogger(ConfigurableComponentTracker.class);

	private ConfigurationServiceImpl m_confService;	

	@SuppressWarnings("unchecked")
	public ConfigurableComponentTracker(BundleContext context,
									    ConfigurationServiceImpl confService) 
		throws InvalidSyntaxException 
	{
		//super(context, (String) null, null); // Wrong: throws an exception
		//super(context, "", null); // Wrong: does not track anything
		//super(context, "org.eclipse.kura..example.publisher.ExamplePublisher", null); // tracks the specified class but of course we cannot use this
		//super(context, (ServiceReference) null, null); // Wrong: throws an exception
		//super(context, SelfConfiguringComponent.class, null); // Wrong: does not track anything
		//super(context, context.createFilter("(" + Constants.OBJECTCLASS + "="+SelfConfiguringComponent.class.getName()+")"), null); // No
		//super(context, context.createFilter("(" + Constants.SERVICE_EXPORTED_INTERFACES + "="+SelfConfiguringComponent.class.getName()+")"), null); // Track nothing. Export the interface?

		// TODO: find a better filter
		super(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), null); // This works but we track everything
		
		m_confService = confService;
	}


	// ----------------------------------------------------------------
	//
	//   Override APIs
	//
	// ----------------------------------------------------------------

	@SuppressWarnings({ "unchecked" })
	@Override
	public void open(boolean trackAllServices) 
	{
		s_logger.info("Opening ServiceTracker");
		super.open(trackAllServices);
		try {

			s_logger.info("Getting ServiceReferences");
			ServiceReference[] refs = context.getServiceReferences((String) null, null);
			if (refs != null) {
				for (ServiceReference ref : refs) {
					String pid = (String) ref.getProperty(Constants.SERVICE_PID);
					if (pid == null) {
						pid = (String) ref.getProperty("component.name");
					}
					if (pid != null && !m_confService.hasConfigurableComponent(pid)) {

						Object obj = context.getService(ref);
						try {
							if (obj == null) {
								s_logger.info("Could not find service for: {}", ref);
							}
							else if (obj instanceof ConfigurableComponent) {
								s_logger.info("Adding ConfigurableComponent {}", pid);
								m_confService.registerComponentConfiguration(ref.getBundle(), pid);
							}
							else if (obj instanceof SelfConfiguringComponent) {
								s_logger.info("Adding SelfConfiguringComponent {}", pid);
								m_confService.registerSelfConfiguringComponent(pid);			    		
							} else if (obj instanceof ManagedService) {
								s_logger.info("Adding ManagedService as a ConfigurableComponent {}", pid);
								m_confService.registerComponentConfiguration(ref.getBundle(), pid);								
							}
						}
						finally {
							context.ungetService(ref);
						}
					}
				}
			}
		}
		catch (InvalidSyntaxException ise) {
			s_logger.error("Error in addingBundle", ise);
		}
		catch (KuraException e) {
			s_logger.error("Error in addingBundle", e);
		}
	};

	
	@SuppressWarnings({ "unchecked" })
	@Override 
	public Object addingService(ServiceReference ref) 
	{
		Object service = super.addingService(ref);
		
		String pid = (String) ref.getProperty(Constants.SERVICE_PID);
		if (pid == null) {
			pid = (String) ref.getProperty("component.name");
		}
		if (pid != null) {
			if (service instanceof ConfigurableComponent) {
				s_logger.info("Adding ConfigurableComponent {}", pid);
				try {
					m_confService.registerComponentConfiguration(ref.getBundle(), pid);
				} 
				catch (KuraException e) {
					s_logger.info("Error adding ConfigurableComponent {} {}", pid, e);
				}
			} else if (service instanceof SelfConfiguringComponent) {
				s_logger.info("Adding SelfConfiguringComponent {}", pid);
				try {
					m_confService.registerSelfConfiguringComponent(pid);
				} 
				catch (KuraException e) {
					s_logger.info("Error adding SelfConfiguringComponent {} {}", pid, e);
				}
			} else if (service instanceof ManagedService) {
				s_logger.info("Adding ManagedService as a ConfigurableComponent {}", pid);
				try {
					m_confService.registerComponentConfiguration(ref.getBundle(), pid);
				} 
				catch (KuraException e) {
					s_logger.info("Error adding ManagedService as a ConfigurableComponent {} {}", pid, e);
				}
			}
		}

		return service;
	};

	
	@SuppressWarnings({ "unchecked" })
	@Override
	public void removedService(ServiceReference reference, Object service) 
	{
		super.removedService(reference, service);

		String pid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (pid == null) {
			pid = (String) reference.getProperty("component.name");
		}
		if (pid != null) {
			if (service instanceof ConfigurableComponent) {
				s_logger.info("Removed ConfigurableComponent {}", pid);
				m_confService.unregisterComponentConfiguration(pid);
			} else if (service instanceof SelfConfiguringComponent) {
				s_logger.info("Removed SelfConfiguringComponent {}", pid);
				m_confService.unregisterComponentConfiguration(pid);
			} else if (service instanceof ManagedService) {
				s_logger.info("Removed ManagedService as a ConfigurableComponent {}", pid);
				m_confService.unregisterComponentConfiguration(pid);				
			}
		}
	};
}
