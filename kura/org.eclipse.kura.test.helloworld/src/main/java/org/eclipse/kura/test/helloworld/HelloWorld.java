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
package org.eclipse.kura.test.helloworld;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld implements SelfConfiguringComponent{
	
	private static final Logger s_logger = LoggerFactory.getLogger(HelloWorld.class);
	
	private static final String APP_ID = HelloWorld.class.getName();
	
	private ComponentContext m_ctx;
	private Map<String, Object> m_properties;
	
	// ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		 s_logger.info("Bundle " + APP_ID + " has started");
		 m_ctx = componentContext;
		 m_properties = properties;
	}
	
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Deactivating " + APP_ID + " ...");
	}
	
	protected void update(Map<String, Object> properties){
		m_properties = properties;
	}

	@Override
	public ComponentConfiguration getConfiguration() throws KuraException {
		
		String pid = "NOT_ASSIGNED_YET";
		if(m_ctx != null){
			pid = (String)m_ctx.getProperties().get(ConfigurationService.KURA_SERVICE_PID);
		}
		Tocd ocd = new Tocd();
		ocd.setName("Test OCD");
		ocd.setDescription("This is a stub for testing SelfConfiguringComponents");
		ocd.setId("TestOcdId");
		Tad prop = new Tad();
		prop.setId("test.property");
		prop.setName("test.property");
		prop.setDescription("Test property");
		prop.setCardinality(0);
		prop.setDefault("123");
		prop.setType(Tscalar.INTEGER);
		prop.setRequired(true);
		ocd.addAD(prop);
		
		Map<String, Object> props = new HashMap<String, Object>();
		if(m_properties != null){
			props.putAll(m_properties);
		} else{
			props.put("test.property", 123);
		}
		
		
		
		ComponentConfiguration conf = new ComponentConfigurationImpl(pid, ocd, props); 
		return conf;
	}
}
