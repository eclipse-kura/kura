/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.net.wifi;

import org.eclipse.kura.KuraException;

/**
 * Marker interface for wifi client monitoring service
 */
public interface WifiClientMonitorService {

	public void registerListener(WifiClientMonitorListener listener);
	public void unregisterListener(WifiClientMonitorListener listener);
	public int getSignalLevel(String interfaceName, String ssid) throws KuraException;
}
