/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.route;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Route configuration interface
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface RouteConfig extends NetConfig {

    /**
     * Gets the description of the route
     *
     * @return The description of the route
     */
    public String getDescription();

    /**
     * Gets the destination of the route
     *
     * @return The destination of the route
     */
    public IPAddress getDestination();

    /**
     * Gets the gateway of the route
     *
     * @return The gateway of the route
     */
    public IPAddress getGateway();

    /**
     * Gets the network mask of the route
     *
     * @return The network mask of the route
     */
    public IPAddress getNetmask();

    /**
     * Gets the interface name associated with the route
     *
     * @return The interface name associated with the route
     */
    public String getInterfaceName();

    /**
     * Gets the metric of the route
     *
     * @return The metric of the route
     */
    public int getMetric();

    /**
     * Compares one route to another
     *
     * @return Whether or not the two routes are equal
     */
    public boolean equals(RouteConfig r);
}
