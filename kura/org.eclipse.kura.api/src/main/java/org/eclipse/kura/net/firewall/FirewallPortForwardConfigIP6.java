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
 * The implementation of IPv6 firewall port forward configurations
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.6
 */
@ProviderType
public class FirewallPortForwardConfigIP6 extends FirewallPortForwardConfigIP<IP6Address>
        implements FirewallPortForwardConfig6 {

    private FirewallPortForwardConfigIP6(FirewallPortForwardConfigIP6Builder builder) {
        super(builder);
    }

    public static FirewallPortForwardConfigIP6Builder builder() {
        return new FirewallPortForwardConfigIP6Builder();
    }

    /**
     * The builder class for the IPv6 firewall port forward configuration
     */
    @ProviderType
    public static class FirewallPortForwardConfigIP6Builder
            extends FirewallPortForwardConfigIPBuilder<IP6Address, FirewallPortForwardConfigIP6Builder> {

        /**
         * Builds a new IPv6 firewall port forward configuration
         */
        @Override
        public FirewallPortForwardConfigIP6 build() throws UnknownHostException {
            if (this.permittedNetwork == null) {
                this.withPermittedNetwork(new NetworkPair<>(IP6Address.getDefaultAddress(), (short) 0));
            }
            return new FirewallPortForwardConfigIP6(this);
        }

        @Override
        public FirewallPortForwardConfigIP6Builder getThis() {
            return this;
        }
    }

}
