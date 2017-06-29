/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

import org.eclipse.kura.KuraException;

/**
 * Defines link tool interface
 *
 * @author ilya.binshtok
 *
 */
public interface LinkTool {

    /**
     * ethtool 'get' operation
     *
     * @return operation status as {@link boolean}
     * @throws KuraException
     */
    public boolean get() throws KuraException;

    /**
     * Reports interface name
     *
     * @return interface name as {@link String}
     */
    public String getIfaceName();

    /**
     * Reports link status
     *
     * @return link status - true if link is detected, otherwise false {@link boolean}
     */
    public boolean isLinkDetected();

    /**
     * Reports link speed in bits/s
     *
     * @return link speed as {@link int}
     */
    public int getSpeed();

    /**
     * Reports duplex (full or half)
     *
     * @return duplex as {@link String}
     */
    public String getDuplex();

    /**
     * Reports signal strength for WiFi interface
     *
     * @return signal strength as {@link int}
     */
    public int getSignal();
}
