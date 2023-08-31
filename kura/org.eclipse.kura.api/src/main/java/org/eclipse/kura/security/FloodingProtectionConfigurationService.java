/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.security;

import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface is used to provide the configuration for the flooding protection.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public interface FloodingProtectionConfigurationService {

    /**
     * Return a set of IPv4 firewall rules for the filter table needed to implement the flooding protection.
     * 
     * @return the set of rules
     */
    public Set<String> getFloodingProtectionFilterRules();

    /**
     * Return a set of IPv4 firewall rules for the nat table needed to implement the flooding protection.
     * 
     * @return the set of rules
     */
    public Set<String> getFloodingProtectionNatRules();

    /**
     * Return a set of IPv4 firewall rules for the mangle table needed to implement the flooding protection.
     * 
     * @return the sets of rules
     */
    public Set<String> getFloodingProtectionMangleRules();

    /**
     * Return a set of IPv6 firewall rules for the filter table needed to implement the flooding protection.
     * 
     * @return the set of rules
     * @since 2.6
     */
    public Set<String> getFloodingProtectionFilterRulesIPv6();

    /**
     * Return a set of IPv6 firewall rules for the nat table needed to implement the flooding protection.
     * 
     * @return the set of rules
     * @since 2.6
     */
    public Set<String> getFloodingProtectionNatRulesIPv6();

    /**
     * Return a set of IPv6 firewall rules for the mangle table needed to implement the flooding protection.
     * 
     * @return the sets of rules
     * @since 2.6
     */
    public Set<String> getFloodingProtectionMangleRulesIPv6();
}