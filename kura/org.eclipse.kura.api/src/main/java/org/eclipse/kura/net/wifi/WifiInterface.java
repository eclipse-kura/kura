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
package org.eclipse.kura.net.wifi;

import java.util.List;
import java.util.Set;

import org.eclipse.kura.net.NetInterface;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Wifi interface
 *
 * @param <T>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WifiInterface<T extends WifiInterfaceAddress> extends NetInterface<T> {

    /**
     * Flags describing the capabilities of a wireless device.
     */
    public enum Capability {
        /** no capabilities supported */
        NONE,
        /** The device supports the 40-bit WEP cipher. */
        CIPHER_WEP40,
        /** The device supports the 104-bit WEP cipher. */
        CIPHER_WEP104,
        /** The device supports the TKIP cipher. */
        CIPHER_TKIP,
        /** The device supports the CCMP cipher. */
        CIPHER_CCMP,
        /** The device supports the WPA encryption/authentication protocol. */
        WPA,
        /** The device supports the RSN encryption/authentication protocol. */
        RSN;
    }

    /**
     * Returns the the capabilities of the wireless device.
     *
     * @return
     * @since 2.0
     */
    public Set<Capability> getCapabilities();

    /**
     * Returns a List of all InterfaceAddresses of this network interface.
     *
     * @return a List object with all or a subset of the InterfaceAddresss of this network interface
     */
    @Override
    public List<T> getNetInterfaceAddresses();

}
