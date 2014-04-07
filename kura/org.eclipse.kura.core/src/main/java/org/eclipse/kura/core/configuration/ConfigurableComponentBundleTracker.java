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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceTracker to track all the ConfigurabaleComponents.
 * When the ConfigurableComponet is found it is then registered to the ConfigurationService.
 */

@SuppressWarnings("rawtypes")
public class ConfigurableComponentBundleTracker extends BundleTracker<Bundle>
{
	private static final Logger s_logger = LoggerFactory.getLogger(ConfigurableComponentBundleTracker.class);

	private ConfigurationServiceImpl m_confService;
	private BundleContext m_context;
	private Map<String, List<String>> m_componentMap;

	public ConfigurableComponentBundleTracker(BundleContext context,
			ConfigurationServiceImpl confService) throws InvalidSyntaxException 
	{
		super(context, Bundle.ACTIVE, null);		
		m_context = context;
		m_confService = confService;
		m_componentMap = new Hashtable<String, List<String>>();
	}


	// ----------------------------------------------------------------
	//
	//   Override APIs
	//
	// ----------------------------------------------------------------

	@Override
	public void open() 
	{
		s_logger.info("Opening BundleTracker");
		super.open();
		
//		Bundle[] bundles = m_context.getBundles();
//		if (bundles != null) {
//			for (Bundle bundle : bundles) {
//				addComponents(bundle);		
//			}
//		}
	};
	
	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
		super.modifiedBundle(bundle, event, object);
		
		/*
		if(event.getType() == BundleEvent.STARTED) {
			System.out.println("Bundle started - adding");
			addComponents(bundle);
		} else if(event.getType() == BundleEvent.STOPPED) {
			System.out.println("Bundle stopped - removing");
			removeComponents(bundle);
		} else {
			System.out.println("Modified bundle: " + bundle.getSymbolicName() + "  Event Type: " + event.getType());
		}*/
		
		/*
		String state = "";
    	if(bundle.getState() == Bundle.ACTIVE) {
        	state = "ACTIVE";        		
    	} else if(bundle.getState() == Bundle.INSTALLED){
        	state = "INSTALLED";
    	} else if(bundle.getState() == Bundle.STARTING){
        	state = "STARTING";
    	} else if(bundle.getState() == Bundle.STOPPING){
        	state = "STOPPING";
    	} else if(bundle.getState() == Bundle.RESOLVED){
        	state = "RESOLVED";
    	} else if(bundle.getState() == Bundle.UNINSTALLED){
        	state = "UNINSTALLED";
    	}
		System.out.println("Modified bundle: " + bundle.getSymbolicName() + "  State: " + state);
    	*/
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		Bundle bnd = super.addingBundle(bundle, event);
		//System.out.println("Adding bundle... " + bundle.getSymbolicName());
		addComponents(bundle);
		
		/*
		String state = "";
    	if(bundle.getState() == Bundle.ACTIVE) {
        	state = "ACTIVE";        		
    	} else if(bundle.getState() == Bundle.INSTALLED){
        	state = "INSTALLED";
    	} else if(bundle.getState() == Bundle.STARTING){
        	state = "STARTING";
    	} else if(bundle.getState() == Bundle.STOPPING){
        	state = "STOPPING";
    	} else if(bundle.getState() == Bundle.RESOLVED){
        	state = "RESOLVED";
    	} else if(bundle.getState() == Bundle.UNINSTALLED){
        	state = "UNINSTALLED";
    	}
		System.out.println("Adding bundle: " + bundle.getSymbolicName() + "  State: " + state);
		*/
		
		return bnd;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Bundle bnd) {
		super.removedBundle(bundle, event, bnd);

		//System.out.println("Removing bundle... " + bundle.getSymbolicName());
		
		/*
		String state = "";
    	if(bundle.getState() == Bundle.ACTIVE) {
        	state = "ACTIVE";        		
    	} else if(bundle.getState() == Bundle.INSTALLED){
        	state = "INSTALLED";
    	} else if(bundle.getState() == Bundle.STARTING){
        	state = "STARTING";
    	} else if(bundle.getState() == Bundle.STOPPING){
        	state = "STOPPING";
    	} else if(bundle.getState() == Bundle.RESOLVED){
        	state = "RESOLVED";
    	} else if(bundle.getState() == Bundle.UNINSTALLED){
        	state = "UNINSTALLED";
    	}
		System.out.println("Removed bundle: " + bundle.getSymbolicName() + "  State: " + state);
		*/
		
		removeComponents(bundle);
	}

	@SuppressWarnings("unchecked")
	private void addComponents(Bundle bundle) 
	{
		//wait for activation??
		while(bundle.getState() != Bundle.ACTIVE) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ServiceReference scrServiceRef = m_context.getServiceReference( ScrService.class.getName() );
		ScrService scrService = (ScrService) m_context.getService(scrServiceRef);
		Component[] components = scrService.getComponents(bundle);
		if (components != null) {
			for (Component component : components) {
				
				s_logger.trace("Evaluating component: " + component.getId());
				
				String pid = component.getName();
				if (pid != null && !m_confService.hasConfigurableComponent(pid)) {
					s_logger.trace("\tpid is " + pid);
					
					String componentClassName = component.getClassName();					
					try {

						Class clazz = bundle.loadClass(componentClassName);
						if (ConfigurableComponent.class.isAssignableFrom(clazz)) {
							s_logger.info("Adding ConfigurableComponent {}", pid);
							List comps = m_componentMap.get(bundle.getSymbolicName());
							if(comps == null || comps.size() < 1) {
								comps = new ArrayList<String>();
								m_componentMap.put(bundle.getSymbolicName(), comps);
							}
							comps.add(pid);
							m_confService.registerComponentConfiguration(bundle, pid);
						}
						else if (SelfConfiguringComponent.class.isAssignableFrom(clazz)) {
							s_logger.info("Adding SelfConfiguringComponent {}", pid);
							List comps = m_componentMap.get(bundle.getSymbolicName());
							if(comps == null || comps.size() < 1) {
								comps = new ArrayList<String>();
								m_componentMap.put(bundle.getSymbolicName(), comps);
							}
							comps.add(pid);
							m_confService.registerSelfConfiguringComponent(pid);			    		
						}
					}
					catch (Exception e) {
						s_logger.error("Error in addComponents", e);
					}
				} else {
					if(s_logger.isTraceEnabled()) {
						if(pid != null) {
							s_logger.trace("\tpid: " + pid + " doesn't have a configurable component");
						} else {
							s_logger.trace("\tnull pid with " + component.getId());
						}
					}
				}
			}
		} else {
			s_logger.trace("No components with the bundle: " + bundle.getSymbolicName());
		}
		m_context.ungetService(scrServiceRef);
	}

	
	private void removeComponents(Bundle bundle) 
	{
		s_logger.info("Removing Components for bundle: {}", bundle.getSymbolicName());
		
		//do not rely on srcService as the components may be nonexistent (already removed) by the time we get this call
		List<String> comps = m_componentMap.get(bundle.getSymbolicName());
		if(comps != null && comps.size() > 0) {
			for(int i=0; i<comps.size(); i++) {
				String pid = comps.get(i);
				s_logger.debug("Removing component " + pid);
				m_confService.unregisterComponentConfiguration(pid);
				comps.remove(i);
				i--;
			}
		}
	}
}
