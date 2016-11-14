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
package org.eclipse.kura.camel.xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.camel.component.AbstractXmlCamelComponent;
import org.eclipse.kura.camel.component.Configuration;
import org.eclipse.kura.camel.runner.CamelRunner.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ready to run XML based Apache Camel component
 *
 * @noextend This class is not intended to be extended
 */
public class XmlRouterComponent extends AbstractXmlCamelComponent {

    private static final Logger logger = LoggerFactory.getLogger(XmlRouterComponent.class);

    private Set<String> requiredComponents = new HashSet<>();

    public XmlRouterComponent() {
        super("xml.data");
    }

    @Override
    protected void customizeBuilder(final Builder builder, final Map<String, Object> properties) {
        final Set<String> newRequiredComponents = parse(Configuration.asString(properties, "component.prereqs"));

        logger.debug("Setting new component requirements");
        for (final String component : newRequiredComponents) {
            logger.debug("Require component: {}", component);
            builder.requireComponent(component);
        }

        this.requiredComponents = newRequiredComponents;
    }

    @Override
    protected boolean isRestartNeeded(final Map<String, Object> properties) {
        final Set<String> newRequiredComponents = parse(Configuration.asString(properties, "component.prereqs"));

        if (!this.requiredComponents.equals(newRequiredComponents)) {
            logger.debug("Require restart due to 'component.prereqs' change");
            return true;
        }

        return false;
    }

    private static Set<String> parse(String value) {
        if (value == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(value.split("\\s*,\\s*")));
    }

}
