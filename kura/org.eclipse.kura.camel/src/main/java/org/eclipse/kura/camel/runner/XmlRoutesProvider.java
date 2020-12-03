/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
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