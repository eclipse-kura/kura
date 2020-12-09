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

import java.util.Objects;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RoutesDefinition;

public class SimpleRoutesProvider extends AbstractRoutesProvider {

    private final RoutesDefinition routes;

    public SimpleRoutesProvider(RoutesDefinition routes) {
        Objects.requireNonNull(routes);
        this.routes = routes;
    }

    @Override
    protected RoutesDefinition getRoutes(final CamelContext camelContext) throws Exception {
        return this.routes;
    }
}