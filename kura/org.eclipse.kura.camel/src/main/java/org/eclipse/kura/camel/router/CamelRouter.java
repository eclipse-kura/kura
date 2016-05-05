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
package org.eclipse.kura.camel.router;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.bean.BeanComponent;
import org.apache.camel.component.beanclass.ClassComponent;
import org.apache.camel.component.binding.BindingNameComponent;
import org.apache.camel.component.browse.BrowseComponent;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.component.timer.TimerComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spi.Language;
import org.apache.camel.component.kura.KuraRouter;
import org.eclipse.kura.camel.RouterConstants;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: is this base class an API? If so please add Javadoc.
 * Base class for Camel routers deployable into Eclipse Kura.
 * All CamelRouters are configurable components.
 */
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
				newCamelRouteXml.contains("<route ") &&
				!newCamelRouteXml.equals(oldCamelRouteXml)) {
			this.m_camelRouteXml = newCamelRouteXml;
			if (!m_camelRouteXml.isEmpty()) {
				try {
					ByteArrayInputStream bais =  new ByteArrayInputStream(m_camelRouteXml.getBytes());
					RoutesDefinition routesDefinition = camelContext.loadRoutesDefinition(bais);
					camelContext.addRouteDefinitions(routesDefinition.getRoutes());
				} catch (Exception e) {
					s_logger.warn("Cannot load routes definitions: {}", m_camelRouteXml, e);
				}
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
		registerComponents();
		super.beforeStart(camelContext);
	}

	protected void modified(Map<String, Object> properties) {
		m_properties = properties;
		updated(m_properties);
	}

	// Components registration

	protected void registerComponents() {
		camelContext.addComponent("bean", new BeanComponent());
		camelContext.addComponent("binding", new BindingNameComponent());
		camelContext.addComponent("browse", new BrowseComponent());
		camelContext.addComponent("class", new ClassComponent());
		camelContext.addComponent("direct", new DirectComponent());
		camelContext.addComponent("log", new LogComponent());
		camelContext.addComponent("mock", new MockComponent());
		camelContext.addComponent("properties", new PropertiesComponent());
		camelContext.addComponent("seda", new SedaComponent());
		camelContext.addComponent("timer", new TimerComponent());

		registerLanguage("simple", new SimpleLanguage());
	}

	protected void registerLanguage(String languageName, Language language) {
		try {
			Field field = DefaultCamelContext.class.getDeclaredField("languages");
			field.setAccessible(true);
			Map<String, Language> languages = (Map<String, Language>) field.get(camelContext);
			languages.put(languageName, language);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
