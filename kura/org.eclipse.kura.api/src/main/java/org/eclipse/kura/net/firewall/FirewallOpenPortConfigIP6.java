/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.firewall;

import java.net.UnknownHostException;

import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The implementation of IPv6 firewall open port configurations
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.6
 */
@ProviderType
public class FirewallOpenPortConfigIP6 extends FirewallOpenPortConfigIP<IP6Address> implements FirewallOpenPortConfig6 {

    private FirewallOpenPortConfigIP6(FirewallOpenPortConfigIP6Builder builder) {
        super(builder);
    }

    public static FirewallOpenPortConfigIP6Builder builder() {
        return new FirewallOpenPortConfigIP6Builder();
    }

    /**
     * The builder class for the IPv6 firewall open port configuration
     */
    @ProviderType
    public static class FirewallOpenPortConfigIP6Builder
            extends FirewallOpenPortConfigIPBuilder<IP6Address, FirewallOpenPortConfigIP6Builder> {

        /**
         * Builds a new IPv6 firewall open port configuration
         */
        @Override
        public FirewallOpenPortConfigIP6 build() throws UnknownHostException {
            if (this.permittedNetwork == null) {
                this.withPermittedNetwork(new NetworkPair<>(IP6Address.getDefaultAddress(), (short) 0));
            }
            return new FirewallOpenPortConfigIP6(this);
        }

        @Override
        public FirewallOpenPortConfigIP6Builder getThis() {
            return this;
        }
    }
}
