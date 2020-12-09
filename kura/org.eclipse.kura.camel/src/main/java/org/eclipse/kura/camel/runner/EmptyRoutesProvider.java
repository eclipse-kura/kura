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

import org.apache.camel.CamelContext;

public class EmptyRoutesProvider implements RoutesProvider {

    public static final EmptyRoutesProvider INSTANCE = new EmptyRoutesProvider();

    private EmptyRoutesProvider() {
    }

    @Override
    public void applyRoutes(final CamelContext camelContext) throws Exception {
        CamelRunner.removeRoutes(camelContext, CamelRunner.fromRoutes(camelContext.getRoutes()));
    }
}