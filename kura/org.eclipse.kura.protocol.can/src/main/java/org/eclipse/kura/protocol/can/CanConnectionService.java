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
package org.eclipse.kura.protocol.can;

import java.io.IOException;

import org.eclipse.kura.KuraException;

/**
 * This is the primary control class for a CAN network interface.
 */

public interface CanConnectionService {

    /**
     * Establishes a RAW CAN socket connection
     * 
     * @throws IOException
     */
    public void connectCanSocket() throws IOException;
    
    /**
     * Disconnects a CAN socket connection
     * 
     * @throws IOException
     */
    public void disconnectCanSocket() throws IOException;
    
    /**
     * Sends an array of bytes on a CAN socket
     *
     * @param ifName
     *            the name of the socket (eg "can0")
     * @param canId
     *            can identifier, must be unique
     * @param message
     *            the array of bytes to send to the socket
     * @throws KuraException
     * @throws IOException
     */
    public void sendCanMessage(String ifName, int canId, byte[] message) throws KuraException, IOException;

    /**
     * Reads frames that are waiting on socket CAN (all interfaces) and returns an array
     * if canId is correct.
     * <p>
     * A filter can be defined to receive only frames for the id we are interested in. If the can_id param is set to -1,
     * no filter is applied.
     *
     * @param can_id
     *            id to be filtered
     * @param can_mask
     *            mask to be applied to the id
     * @return CanMessage = canId and an array of bytes buffered on the socket if any
     * @throws KuraException
     * @throws IOException
     */
    public CanMessage receiveCanMessage(int canId, int canMask) throws IOException;
}
