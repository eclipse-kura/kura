/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.wifi;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface WifiClientMonitorListener {

    /**
     * @since 2.0
     */
    public void setWifiSignalLevel(String interfaceName, int signalLevel);
}
