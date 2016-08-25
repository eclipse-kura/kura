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
package org.eclipse.kura.core.configuration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.ds.model.DeclarationParser;
import org.eclipse.kura.configuration.metatype.Designate;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.Tmetadata;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.util.ComponentUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * BundleTracker to track all the Service which have defaults in MetaType.
 * When the ConfigurableComponet is found it is then registered to the ConfigurationService.
 */
public class ComponentMetaTypeBundleTracker extends BundleTracker<Bundle>
{
	private static final Logger s_logger = LoggerFactory.getLogger(ComponentMetaTypeBundleTracker.class);

	private static final String PATTERN_CONFIGURATION_REQUIRE = "configuration-policy=\"require\"";
	private static final String PATTERN_SERVICE_PROVIDE	= "provide interface=\"org.eclipse.kura.configuration.SelfConfiguringComponent\"";
	private BundleContext m_context;
	private ConfigurationServiceImpl m_configurationService;

	public ComponentMetaTypeBundleTracker(BundleContext context,
										  ConfigurationServiceImpl configurationService) 
		throws InvalidSyntaxException 
	{
		super(context, Bundle.ACTIVE, null);		
		m_context = context;
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

		Map<String,Tmetadata> metas = ComponentUtil.getMetadata(m_context, bundle); 
		for (String metatypePid : metas.keySet()) {
			try {
				
				// register the OCD for all the contained services
				Tmetadata  metadata = metas.get(metatypePid);
				if (metadata != null) {
					
					// check if this component is a factory
					boolean isFactory = false;
					Designate designate = ComponentUtil.getDesignate(metadata, metatypePid);
					if (designate.getFactoryPid() != null && !designate.getFactoryPid().isEmpty()) {
						isFactory = true;
					}

					// register the pid with the OCD and whether it is a factory
					OCD ocd = ComponentUtil.getOCD(metadata, metatypePid);
					m_configurationService.registerComponentOCD(metatypePid, (Tocd) ocd, isFactory);
				}
			}
			catch (Exception e) {
				s_logger.error("Error seeding configuration for pid: "+metatypePid, e);
			}
		}
		
		Enumeration<URL> enumeration = bundle.findEntries("OSGI-INF", "*.xml", false);
		if(enumeration != null){
			while(enumeration.hasMoreElements()){
				URL entry = enumeration.nextElement();
				s_logger.info(entry.getPath());
				BufferedReader reader = null;
				try{
					URL fileUrl = FileLocator.toFileURL(entry);
					reader = new BufferedReader(new FileReader(fileUrl.getFile()));
					StringBuilder contents = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null){
						contents.append(line);
					}
					if(contents.toString().contains(PATTERN_SERVICE_PROVIDE) && contents.toString().contains(PATTERN_CONFIGURATION_REQUIRE)){
						Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fileUrl.getFile());
						NodeList nl = dom.getElementsByTagName("property");
						for(int i=0; i<nl.getLength(); i++){
							Node n = nl.item(i);
							if(n instanceof Element){
								if( (((Element)n).getAttribute("name")).equals("service.pid")
									&& (((Element)n).getAttribute("type")).equals("String")){
									m_configurationService.addFacoryPid(((Element)n).getAttribute("value"));			
								}
							}
						}
						
					}
				}catch(Exception ex){
					s_logger.error("Error while reading Component Definition file {}", entry.getPath());
				}finally{
					try {
						reader.close();
					} catch (IOException e) {
						s_logger.error("Error closing File Reader!");
					}
				}
			}
		}
		
	}
}
