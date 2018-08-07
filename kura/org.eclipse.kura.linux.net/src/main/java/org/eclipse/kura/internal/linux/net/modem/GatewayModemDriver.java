/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.modem;

import org.eclipse.kura.KuraException;

/**
 * Provides methods to manage the modem status, allowing to turn off, on or reset the modems identified by the provided
 * vendor and product ids.
 *
 */
public interface GatewayModemDriver {

    public void turnModemOff(String vendor, String product) throws KuraException;

    public void turnModemOn(String vendor, String product) throws KuraException;

    public void resetModem(String vendor, String product) throws KuraException;
}
