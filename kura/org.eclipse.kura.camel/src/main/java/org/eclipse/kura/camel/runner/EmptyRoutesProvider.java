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

public class EmptyRoutesProvider implements RoutesProvider {

    public static final EmptyRoutesProvider INSTANCE = new EmptyRoutesProvider();

    private EmptyRoutesProvider() {
    }

    @Override
    public void applyRoutes(final CamelContext camelContext) throws Exception {
        CamelRunner.removeRoutes(camelContext, CamelRunner.fromRoutes(camelContext.getRoutes()));
    }
}