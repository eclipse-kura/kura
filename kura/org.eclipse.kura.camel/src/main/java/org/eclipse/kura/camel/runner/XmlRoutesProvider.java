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
package org.eclipse.kura.camel.runner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RoutesDefinition;

public class XmlRoutesProvider extends AbstractRoutesProvider {

    private final String xml;

    public XmlRoutesProvider(final String xml) {
        Objects.requireNonNull(xml);
        this.xml = xml;
    }

    @Override
    protected RoutesDefinition getRoutes(final CamelContext camelContext) throws Exception {
        try (final InputStream in = new ByteArrayInputStream(this.xml.getBytes(StandardCharsets.UTF_8))) { // just always close it
            return camelContext.loadRoutesDefinition(in);
        }
    }
}