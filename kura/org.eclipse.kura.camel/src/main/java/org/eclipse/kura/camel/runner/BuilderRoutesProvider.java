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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

public class BuilderRoutesProvider implements RoutesProvider {

    private final RouteBuilder builder;

    public BuilderRoutesProvider(final RouteBuilder builder) throws Exception {
        this.builder = builder;
    }

    @Override
    public void applyRoutes(CamelContext camelContext) throws Exception {
        CamelRunner.removeMissingRoutes(camelContext, this.builder.getRouteCollection().getRoutes());
        camelContext.addRoutes(this.builder);
    }
}