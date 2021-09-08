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
 *******************************************************************************/
package org.eclipse.kura.net.admin;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;

public interface NetworkConfigurationService {

    public static final String PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";

    public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) throws KuraException;

    public NetworkConfiguration getNetworkConfiguration() throws KuraException;

    /**
     * Returns the Wi-Fi region for all Wi-Fi network interfaces.
     * 
     * @return the country code of the region or 00 for the Word Wide Region.
     */

    public String getWifiRegion() throws KuraException;

    /**
     * Set the Country Code for all Wi-fi network interfaces.
     * 
     * @param countryCode
     *            Country code in ISO 3166-1 alpha-2 format.
     * 
     */

    public void setWifiRegion(String countryCode) throws KuraException;

}
