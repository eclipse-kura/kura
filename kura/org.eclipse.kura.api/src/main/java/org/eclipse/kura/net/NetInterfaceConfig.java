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
package org.eclipse.kura.net;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker class for NetInterfaceConfig objects.
 * Network interfaces implementing this maker will return addresses of type NetInterfaceAddressConfig in their
 * getNetInterfaceAddresses() method.
 * NetInterfaceAddressConfig complements NetInterfaceAddress, which provides the current addresses associated to the
 * interface, with the NetConfig objects.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetInterfaceConfig<T extends NetInterfaceAddressConfig> extends NetInterface<T> {

}
