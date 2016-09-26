/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.cloud.factory.internal;

import static org.eclipse.kura.camel.component.Configuration.asInteger;
import static org.eclipse.kura.camel.component.Configuration.asString;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelFactory implements ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(CamelFactory.class);

    public static final String FACTORY_ID = "org.eclipse.kura.camel.cloud.factory.CamelFactory";

    private ConfigurationService configurationService;

    private XmlCamelCloudService service;

    private ServiceConfiguration configuration;

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void activate(ComponentContext context, Map<String, Object> properties) throws Exception {
        setFromProperties(properties);
    }

    public void modified(ComponentContext context, Map<String, Object> properties) throws Exception {
        if (shouldDelete(properties)) {
            final Object pid = context.getProperties().get("kura.service.pid");
            if (pid instanceof String) {
                triggerDelete((String) pid);
            }
            return;
        } else {
            setFromProperties(properties);
        }
    }

    private void setFromProperties(Map<String, Object> properties) throws Exception {
        final String pid = asString(properties, "cloud.service.pid");

        final ServiceConfiguration configuration = new ServiceConfiguration();
        configuration.setXml(asString(properties, "xml"));
        configuration.setServiceRanking(asInteger(properties, "serviceRanking"));

        createService(pid, configuration);
    }

    public void deactivate() {
        if (this.service != null) {
            try {
                this.service.stop();
            } catch (Exception e) {
                logger.warn("Failed to stop", e);
            }
            this.service = null;
        }
    }

    private void createService(final String pid, final ServiceConfiguration configuration) throws Exception {
        if (pid == null) {
            return;
        }

        if (this.configuration == configuration) {
            // null to null?
            return;
        }
        if (this.configuration != null && this.configuration.equals(configuration)) {
            // no change
            return;
        }

        // stop old service

        if (this.service != null) {
            this.service.stop();
            this.service = null;
        }

        // start new service
        if (configuration.isValid()) {
            this.service = new XmlCamelCloudService(FrameworkUtil.getBundle(CamelFactory.class).getBundleContext(), pid, configuration);
            this.service.start();
        }

        this.configuration = configuration;
    }

    private void triggerDelete(String pid) {
        performDelete(pid);
    }

    private void performDelete(String pid) {
        CamelManager.delete(this.configurationService, pid);
    }

    private static boolean shouldDelete(Map<String, Object> properties) {
        final Object value = properties.get("delete");
        if (value == null) {
            return false;
        }
        return "true".equals(value.toString());
    }

}
