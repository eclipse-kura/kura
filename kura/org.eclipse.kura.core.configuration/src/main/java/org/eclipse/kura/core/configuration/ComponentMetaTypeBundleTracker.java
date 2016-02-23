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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.util.CollectionsUtil;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BundleTracker to track all the Service which have defaults in MetaType.
 * When the ConfigurableComponet is found it is then registered to the ConfigurationService.
 */
public class ComponentMetaTypeBundleTracker extends BundleTracker<Bundle>
{
	private static final Logger s_logger = LoggerFactory.getLogger(ComponentMetaTypeBundleTracker.class);

	private BundleContext m_context;
	private ConfigurationAdmin m_configurationAdmin;
	private ConfigurationServiceImpl m_configurationService;

	public ComponentMetaTypeBundleTracker(BundleContext context,
										  ConfigurationAdmin configurationAdmin,
										  ConfigurationServiceImpl configurationService) 
		throws InvalidSyntaxException 
	{
		super(context, Bundle.ACTIVE, null);		
		m_context = context;
		m_configurationAdmin = configurationAdmin;
		m_configurationService = configurationService;
	}


	// ----------------------------------------------------------------
	//
	//   Override APIs
	//
	// ----------------------------------------------------------------

	@Override
	public void open() 
	{
		s_logger.info("Opening ComponentMetaTypeBundleTracker...");
		super.open();
		s_logger.debug("open(): getting bundles...");
		Bundle[] bundles = m_context.getBundles();
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (bundle.getState() == Bundle.ACTIVE) {
					s_logger.debug("open(): processing MetaType for bundle: {}...", bundle.getSymbolicName());
					processBundleMetaType(bundle);
					s_logger.debug("open(): processed MetaType for bundle: {}", bundle.getSymbolicName());
				} else {
					s_logger.debug("open(): bundle: {} is in state: {}. MetaType will be processed by addingBundle()",
							bundle.getSymbolicName(), bundle.getState());
				}
			}
		}
		s_logger.debug("open(): done");
	};
	
	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) 
	{
		Bundle bnd = super.addingBundle(bundle, event);
		s_logger.debug("addingBundle(): processing MetaType for bundle: {}...", bundle.getSymbolicName());
		processBundleMetaType(bundle);
		s_logger.debug("addingBundle(): processed MetaType for bundle: {}", bundle.getSymbolicName());
		return bnd;
	}


	// ----------------------------------------------------------------
	//
	//   Private APIs
	//
	// ----------------------------------------------------------------
	
	private void processBundleMetaType(Bundle bundle) 
	{
		// Push the latest configuration merging the properties in ConfigAdmin
		// with the default properties read from the component's meta-type.
		// This allows components to incrementally add new configuration
		// properties in the meta-type.
		// Only the new default properties are merged with the configuration 
		// properties in ConfigurationAdmin.
		// Note: configuration properties in snapshots no longer present in 
		// the meta-type are not purged.

		Map<String,OCD> ocds = ComponentUtil.getObjectClassDefinition(m_context, bundle);
		for (Entry<String, OCD> ocdEntry : ocds.entrySet()) {
			String pid = ocdEntry.getKey();
			try {
				OCD ocd = ocdEntry.getValue();
				Configuration config = m_configurationAdmin.getConfiguration(pid ,null);
				if (config != null) {

					// get the properties from ConfigurationAdmin if any are present
					Map<String, Object> props = new HashMap<String, Object>(); 
					if (config.getProperties() != null) {
						props = CollectionsUtil.dictionaryToMap(config.getProperties(), ocd);
					}
				
					// merge the current properties, if any, with the defaults from metatype
					boolean mergeDone = m_configurationService.mergeWithDefaults(ocds.get(pid), props); 
					if (mergeDone) {					

						// there was a merge with the defaults
						// so notify the updated configuration to ConfigurationAdmin
						config.update(CollectionsUtil.mapToDictionary(props));
						s_logger.info("Seeding updated configuration for pid: {}", pid);
					}
				}
			}
			catch (Exception e) {
				s_logger.error("Error seeding configuration for pid: "+pid, e);
			}
		}
	}
}
