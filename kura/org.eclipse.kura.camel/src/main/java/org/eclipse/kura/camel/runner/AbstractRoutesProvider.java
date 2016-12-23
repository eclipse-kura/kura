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

import static org.eclipse.kura.camel.runner.CamelRunner.removeMissingRoutes;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RoutesDefinition;

public abstract class AbstractRoutesProvider implements RoutesProvider {

    @Override
    public void applyRoutes(final CamelContext camelContext) throws Exception {

        final RoutesDefinition routes = getRoutes(camelContext);

        removeMissingRoutes(camelContext, routes.getRoutes());

        camelContext.addRouteDefinitions(routes.getRoutes());
    }

    protected abstract RoutesDefinition getRoutes(CamelContext camelContext) throws Exception;
}