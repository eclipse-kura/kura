/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.xml;

import static java.lang.String.format;
import static org.eclipse.kura.camel.component.Configuration.asBoolean;
import static org.eclipse.kura.camel.component.Configuration.asString;
import static org.eclipse.kura.camel.runner.CamelRunner.createOsgiRegistry;
import static org.eclipse.kura.camel.utils.CamelContexts.scriptInitCamelContext;
import static org.osgi.framework.FrameworkUtil.getBundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.camel.bean.PayloadFactory;
import org.eclipse.kura.camel.component.AbstractXmlCamelComponent;
import org.eclipse.kura.camel.component.Configuration;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.eclipse.kura.cloud.CloudService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ready to run XML based Apache Camel component
 *
 * @noextend This class is not intended to be extended
 */
public class XmlRouterComponent extends AbstractXmlCamelComponent {

    private static final String TOKEN_PATTERN = "\\s*,\\s*";

    private static final Logger logger = LoggerFactory.getLogger(XmlRouterComponent.class);

    private static final String CLOUD_SERVICE_PREREQS = "cloudService.prereqs";
    private static final String COMPONENT_PREREQS = "component.prereqs";
    private static final String LANGUAGE_PREREQS = "language.prereqs";
    private static final String DISABLE_JMX = "disableJmx";
    private static final String INIT_CODE = "initCode";

    private final BundleContext bundleContext;

    private Set<String> requiredComponents = new HashSet<>();
    private Set<String> requiredLanguages = new HashSet<>();

    private Map<String, String> cloudServiceRequirements = new HashMap<>();
    private String initCode = "";

    private boolean disableJmx;

    public XmlRouterComponent() {
        super("xml.data");
        this.bundleContext = FrameworkUtil.getBundle(XmlRouterComponent.class).getBundleContext();
    }

    @Override
    protected void customizeBuilder(final Builder builder, final Map<String, Object> properties) {

        // JMX

        final boolean disableJmx = asBoolean(properties, DISABLE_JMX, false);
        builder.disableJmx(disableJmx);

        // parse configuration

        final Set<String> newRequiredComponents = parseRequirements(asString(properties, COMPONENT_PREREQS));
        final Set<String> newRequiredLanguages = parseRequirements(asString(properties, LANGUAGE_PREREQS));

        final Map<String, String> cloudServiceRequirements = parseCloudServiceRequirements(
                asString(properties, CLOUD_SERVICE_PREREQS));

        final String initCode = parseInitCode(properties);

        // set component requirements

        logger.debug("Setting new component requirements");
        for (final String component : newRequiredComponents) {
            logger.debug("Require component: {}", component);
            builder.requireComponent(component);
        }

        logger.debug("Setting new language requirements");
        for (final String language : newRequiredLanguages) {
            logger.debug("Require language: {}", language);
            builder.requireLanguage(language);
        }

        // set cloud service requirements

        logger.debug("Setting new cloud service requirements");
        for (final Map.Entry<String, String> entry : cloudServiceRequirements.entrySet()) {
            final String filter;
            if (entry.getValue().startsWith("(")) {
                filter = entry.getValue();
            } else {
                filter = format("(&(%s=%s)(kura.service.pid=%s))", Constants.OBJECTCLASS, CloudService.class.getName(),
                        entry.getValue());
            }
            builder.cloudService(null, filter, Builder.addAsCloudComponent(entry.getKey()));
        }

        if (!initCode.isEmpty()) {

            // call init code before context start

            builder.addBeforeStart(camelContext -> {
                scriptInitCamelContext(camelContext, initCode, XmlRouterComponent.class.getClassLoader());
            });
        }

        // build registry

        final BundleContext ctx = getBundle(XmlRouterComponent.class).getBundleContext();
        final Map<String, Object> services = new HashMap<>();
        services.put("payloadFactory", new PayloadFactory());
        builder.registryFactory(createOsgiRegistry(ctx, services));

        // assign new state

        this.requiredComponents = newRequiredComponents;
        this.requiredLanguages = newRequiredLanguages;
        this.cloudServiceRequirements = cloudServiceRequirements;
        this.initCode = initCode;
        this.disableJmx = disableJmx;
    }

    @Override
    protected boolean isRestartNeeded(final Map<String, Object> properties) {

        final boolean disableJmx = asBoolean(properties, DISABLE_JMX, false);

        final Set<String> newRequiredComponents = parseRequirements(asString(properties, COMPONENT_PREREQS));

        final Map<String, String> cloudServiceRequirements = parseCloudServiceRequirements(
                asString(properties, CLOUD_SERVICE_PREREQS));

        final String initCode = parseInitCode(properties);

        if (this.disableJmx != disableJmx) {
            logger.debug("Require restart due to '{}' change", DISABLE_JMX);
            return true;
        }

        if (!this.requiredComponents.equals(newRequiredComponents)) {
            logger.debug("Require restart due to '{}' change", COMPONENT_PREREQS);
            return true;
        }

        if (!this.requiredLanguages.equals(newRequiredComponents)) {
            logger.debug("Require restart due to '{}' change", LANGUAGE_PREREQS);
            return true;
        }

        if (!this.cloudServiceRequirements.equals(cloudServiceRequirements)) {
            logger.debug("Require restart due to '{}' change", CLOUD_SERVICE_PREREQS);
            return true;
        }

        if (!this.initCode.equals(initCode)) {
            logger.debug("Require restart due to '{}' change", INIT_CODE);
            return true;
        }

        return false;
    }

    private static Map<String, String> parseCloudServiceRequirements(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> result = new HashMap<>();

        for (final String tok : value.split(TOKEN_PATTERN)) {
            logger.debug("Testing - '{}'", tok);

            final String[] s = tok.split("=", 2);
            if (s.length != 2) {
                continue;
            }

            logger.debug("CloudService - '{}' -> '{}'", s[0], s[1]);
            result.put(s[0], s[1]);
        }

        return result;
    }

    private static Set<String> parseRequirements(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(value.split(TOKEN_PATTERN)));
    }

    private static String parseInitCode(final Map<String, Object> properties) {
        return Configuration.asString(properties, INIT_CODE, "");
    }

    @Override
    protected BundleContext getBundleContext() {
        return this.bundleContext;
    }

}
