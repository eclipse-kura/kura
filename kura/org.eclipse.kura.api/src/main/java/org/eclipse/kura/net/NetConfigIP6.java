/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Configuration for a network interface based on IPv6 addresses.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class NetConfigIP6 extends NetConfigIP<IP6Address>implements NetConfig6 {

    /**
     * Empty Constructor
     */
    public NetConfigIP6(NetInterfaceStatus status, boolean autoConnect) {
        super(status, autoConnect);
    }

    /**
     * Constructor for a DHCP Client Configuration for a
     * network interface based on IPv6 addresses.
     *
     * @param dhcp
     *            whether or not DHCP client mode should be used
     */
    public NetConfigIP6(NetInterfaceStatus status, boolean autoConnect, boolean dhcp) {
        super(status, autoConnect, dhcp);
    }

    /**
     * Constructor for a Static Configuration for a
     * network interface based on IPv6 addresses.
     *
     * @param address
     *            - address to be assigned to the interface
     * @param networkPrefixLength
     *            - network prefix length to be assigned to the interface
     * @param gateway
     *            - default gateway to be assigned to the interface
     * @throws KuraException
     */
    public NetConfigIP6(NetInterfaceStatus status, boolean autoConnect, IP6Address address, short networkPrefixLength,
            IP6Address gateway) throws KuraException {
        super(status, autoConnect, address, networkPrefixLength, gateway);
    }

    /**
     * Constructor for a Static Configuration for a
     * network interface based on IPv6 addresses.
     *
     * @param address
     *            - address to be assigned to the interface
     * @param subnetMask
     *            - subnet mask to be assigned to the interface
     * @param gateway
     *            - default gateway to be assigned to the interface
     * @throws KuraException
     */
    public NetConfigIP6(NetInterfaceStatus status, boolean autoConnect, IP6Address address, IP6Address subnetMask,
            IP6Address gateway) throws KuraException {
        super(status, autoConnect, address, subnetMask, gateway);
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }
}
