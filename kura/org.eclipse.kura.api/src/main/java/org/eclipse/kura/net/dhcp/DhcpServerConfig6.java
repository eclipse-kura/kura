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
package org.eclipse.kura.net.dhcp;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface for IPv6-based configurations of DHCP Servers
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface DhcpServerConfig6 extends DhcpServerConfig {

}
