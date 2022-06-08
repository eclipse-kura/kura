/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin;

import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;

public interface NetworkConfigurationService {

    public static final String PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";

    /*
     * Sets the network configuration and write it into the system.
     */
    public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) throws KuraException;

    /**
     * Returns the current network configuration with actual and desired properties.
     * i.e. the current ip address as read from the system and the ip address that should be applied.
     * 
     * @return the current NetworkConfiguration
     * @throws KuraException
     * 
     * @deprecated since 1.5. Use {@link getNetworkConfiguration(boolean recompute)) instead.
     */
    @Deprecated
    public NetworkConfiguration getNetworkConfiguration() throws KuraException;

    /**
     * Returns the current network configuration with actual and desired properties.
     * i.e. the current ip address as read from the system and the ip address that should be applied.
     * 
     * @param recompute:
     *            if true, the configuration is updated before returning it. Otherwise, a cached configuration is
     *            returned.
     * @return the current NetworkConfiguration
     * @throws KuraException
     * @since 1.5
     */
    public Optional<NetworkConfiguration> getNetworkConfiguration(boolean recompute) throws KuraException;

    /**
     * Update the cached configuration.
     * 
     * @throws KuraException
     * 
     * @since 1.5
     */
    public void updateCurrentNetworkConfiguration() throws KuraException;

}
