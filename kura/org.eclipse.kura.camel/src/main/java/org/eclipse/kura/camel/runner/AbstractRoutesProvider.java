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