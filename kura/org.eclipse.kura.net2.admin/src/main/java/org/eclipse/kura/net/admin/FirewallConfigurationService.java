/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
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

import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;

public interface FirewallConfigurationService {

    public static final String PID = "org.eclipse.kura.net.admin.FirewallConfigurationService";

    /**
     * Return the current firewall configuration.
     * 
     * @return the current {@link FirewallConfiguration}
     * @throws KuraException
     */
    public FirewallConfiguration getFirewallConfiguration() throws KuraException;

    /**
     * Set the firewall ports configuration.
     * 
     * @param the
     *            list of {@link org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP} to be applied.
     * @throws KuraException
     */
    public void setFirewallOpenPortConfiguration(
            List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException;

    /**
     * Set the firewall ports forwarding configuration.
     * 
     * @param the
     *            list of {@link org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP} to be applied.
     * @throws KuraException
     */
    public void setFirewallPortForwardingConfiguration(
            List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException;

    /**
     * Set the firewall nat configuration.
     * 
     * @param the
     *            list of {@link org.eclipse.kura.net.firewall.FirewallNatConfig} to be applied.
     * @throws KuraException
     */
    public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException;

    /**
     * Adds flooding protection rules to the firewall configuration.
     * 
     * @param floodingRules
     *            Set of rules specified as Strings to protect against flooding attacks
     */
    public void addFloodingProtectionRules(Set<String> floodingRules);

}
