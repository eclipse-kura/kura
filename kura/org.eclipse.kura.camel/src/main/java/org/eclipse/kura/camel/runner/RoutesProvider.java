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

import org.apache.camel.CamelContext;

@FunctionalInterface
public interface RoutesProvider {

    /**
     * Apply the desired state of camel routes to the context
     * <p>
     * <strong>Note: </strong> This method may need to stop and remove
     * routes which are no longer used
     * </p>
     *
     * @param camelContext
     *            the context the routes should by applied to
     * @throws Exception
     *             if anything goes wrong
     */
    public void applyRoutes(CamelContext camelContext) throws Exception;
}