/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.linux.net;

import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;

/**
 * Serialization service for network interface configuration
 *
 */
public interface NetInterfaceConfigSerializationService {

    /**
     * Reads the persisted configuration and returns a Properties object representing the persisted configuration
     *
     * @param interfaceName
     * @return
     * @throws KuraException
     */
    public Properties read(String interfaceName) throws KuraException;

    /**
     * Persists the network configuration received as argument. Throws a {@link KuraException} if the persist operation
     * fails.
     *
     * @param netConfig
     * @throws KuraException
     */
    public void write(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netConfig) throws KuraException;

}
