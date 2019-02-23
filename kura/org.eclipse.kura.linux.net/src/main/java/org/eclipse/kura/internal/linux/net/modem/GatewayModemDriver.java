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

import java.io.IOException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.board.BoardPowerState;

/**
 * Provides methods to manage the modem status, allowing to turn off, on or reset the modems identified by the provided
 * vendor and product ids.
 *
 */
public interface GatewayModemDriver {

    /**
     * Enables the resource specified by the passed parameters
     * 
     * @param vendor
     * @param product
     * @throws KuraException
     */
    public void enable(String vendor, String product) throws KuraException;

    /**
     * Disables the resource specified by the passed parameters
     * 
     * @param vendor
     * @param product
     * @throws KuraException
     */
    public void disable(String vendor, String product) throws KuraException;

    /**
     * Resets the resource specified by the passed parameters
     * 
     * @param vendor
     * @param product
     * @throws KuraException
     */
    public void reset(String vendor, String product) throws KuraException;

    /**
     * Returns the state of the resource specified by the passed parameters
     *
     * @param vendor
     * @param product
     * @return a {@link BoardPowerState} representing the current status
     * @throws IOException
     */
    public BoardPowerState getState(String vendor, String product) throws KuraException;
}
