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
 *     Jens Reimann <jreimann@redhat.com> - upgrade to Camel 2.17.2
 *******************************************************************************/
package org.eclipse.kura.camel.router;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.kura.KuraRouter;
import org.apache.camel.model.RoutesDefinition;
import org.eclipse.kura.camel.RouterConstants;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CamelRouter extends KuraRouter implements ConfigurableComponent {
	private static final Logger s_logger = LoggerFactory.getLogger(CamelRouter.class);

	/**
	 * Camel route XML, usually configured using SCR property.
	 */
	protected String m_camelRouteXml;
	private Map<String, Object> m_properties;

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	// TODO: Remove these overridden methods as soon as Camel 2.17 is out (see CAMEL-9351)
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		m_properties = properties;
		try {
			start(componentContext.getBundleContext());
			updated(properties); // TODO Keep this line even when Camel 2.17 is out
		} catch (Exception e) {
			s_logger.warn("Problem activating component", e);
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		try {
			stop(componentContext.getBundleContext());
		} catch (Exception e) {
			s_logger.warn("Problem deactivating component", e);
		}
	}

	protected void updated(Map<String, Object> properties) {
		s_logger.debug("Refreshing SCR properties: " + properties);
		refreshCamelRouteXml(m_camelRouteXml, (String) properties.get(RouterConstants.XML_ROUTE_PROPERTY));
	}

	public void refreshCamelRouteXml(String oldCamelRouteXml, String newCamelRouteXml) {
		if (newCamelRouteXml != null && !newCamelRouteXml.isEmpty() &&
				!newCamelRouteXml.equals(oldCamelRouteXml)) {
			this.m_camelRouteXml = newCamelRouteXml;
			try {
				ByteArrayInputStream bais =  new ByteArrayInputStream(m_camelRouteXml.getBytes());
				RoutesDefinition routesDefinition = camelContext.loadRoutesDefinition(bais);
				camelContext.addRouteDefinitions(routesDefinition.getRoutes());
			} catch (Exception e) {
				s_logger.warn("Cannot load routes definitions: {}", m_camelRouteXml, e);
			}
		}
	}

	// ASF Camel workarounds

	// TODO: Remove this overridden method as soon as Camel 2.17 is out (see CAMEL-9357)
	@Override
	public void configure() throws Exception {
		s_logger.debug("No programmatic routes configuration found.");
	}

	// TODO: Remove this overridden method as soon as Camel 2.17 is out (see CAMEL-9314)
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		try {
			super.start(bundleContext);
		} catch (Exception e) {
			s_logger.warn("Problem when starting Kura module", e);
			throw e;
		}
	}

	@Override
	protected void beforeStart(CamelContext camelContext) {
		camelContext.getShutdownStrategy().setTimeout(5);
		camelContext.disableJMX();
		super.beforeStart(camelContext);
	}

	protected void modified(Map<String, Object> properties) {
		m_properties = properties;
		updated(m_properties);
	}

}
