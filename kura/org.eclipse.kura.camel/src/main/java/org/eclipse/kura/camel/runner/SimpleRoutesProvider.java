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