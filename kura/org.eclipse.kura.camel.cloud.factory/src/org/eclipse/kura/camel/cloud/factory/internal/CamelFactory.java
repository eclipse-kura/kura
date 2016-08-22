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

import java.util.Map;

import org.eclipse.kura.KuraException;
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

    private String xml;

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void activate(ComponentContext context, Map<String, Object> properties) throws Exception {
        final String pid = asString(properties.get("kura.service.pid"));
        final String xml = asString(properties.get("xml"));
        setXml(pid, xml);
    }

    public void modified(ComponentContext context, Map<String, Object> properties) throws Exception {
        if (shouldDelete(properties)) {
            final Object pid = context.getProperties().get("kura.service.pid");
            if (pid instanceof String) {
                triggerDelete((String) pid);
            }
            return;
        } else {
            final String pid = asString(properties.get("kura.service.pid"));
            final String xml = asString(properties.get("xml"));
            setXml(pid, xml);
        }
    }

    public void deactivate() {
    }

    private void setXml(String pid, String xml) throws Exception {
        if (this.xml == xml) {
            // null to null
            return;
        }
        if (this.xml != null && !this.xml.equals(xml)) {
            // no change
            return;
        }

        // stop old service

        if (this.service != null) {
            this.service.stop();
            this.service = null;
        }

        // start new service
        if (xml != null && !xml.trim().isEmpty()) {
            this.service = new XmlCamelCloudService(FrameworkUtil.getBundle(CamelFactory.class).getBundleContext(), xml, pid);
            this.service.start();
        }

        this.xml = xml;
    }

    private void triggerDelete(String pid) {
        performDelete(pid);
    }

    private void performDelete(String pid) {
        try {
            this.configurationService.deleteFactoryConfiguration(pid, true);
        } catch (KuraException e) {
            logger.warn("Failed to delete: {}", pid, e);
        }
    }

    private static boolean shouldDelete(Map<String, Object> properties) {
        final Object value = properties.get("delete");
        if (value == null)
            return false;
        return "true".equals(value.toString());
    }

    private static String asString(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

}
