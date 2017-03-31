/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others.
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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.apache.commons.io.input.ReaderInputStream;

public class XmlRoutesProvider extends AbstractRoutesProvider {

    private final Supplier<InputStream> inputStreamProvider;

    public XmlRoutesProvider(final String xml) {
        Objects.requireNonNull(xml);
        this.inputStreamProvider = () -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    private XmlRoutesProvider(final Supplier<InputStream> inputStreamProvider) {
        Objects.requireNonNull(inputStreamProvider);
        this.inputStreamProvider = inputStreamProvider;
    }

    @Override
    protected RoutesDefinition getRoutes(final CamelContext camelContext) throws Exception {
        try (final InputStream in = this.inputStreamProvider.get()) {
            return camelContext.loadRoutesDefinition(in);
        }
    }

    public static XmlRoutesProvider fromString(final String xml) {
        return new XmlRoutesProvider(xml);
    }

    public static XmlRoutesProvider fromReader(final Supplier<Reader> reader) {
        Objects.requireNonNull(reader);
        return new XmlRoutesProvider(() -> new ReaderInputStream(reader.get(), StandardCharsets.UTF_8));
    }

    public static XmlRoutesProvider fromInputStream(final Supplier<InputStream> inputStream) {
        return new XmlRoutesProvider(inputStream);
    }
}