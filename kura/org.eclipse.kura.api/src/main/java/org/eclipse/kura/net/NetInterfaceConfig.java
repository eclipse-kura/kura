/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

/**
 * Marker class for NetInterfaceConfig objects.
 * Network interfaces implementing this maker will return addresses of type NetInterfaceAddressConfig in their getNetInterfaceAddresses() method.
 * NetInterfaceAddressConfig complements NetInterfaceAddress, which provides the current addresses associated to the interface, with the NetConfig objects. 
 */
public interface NetInterfaceConfig<T extends NetInterfaceAddressConfig> extends NetInterface<T> {

}
