/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.entropia.can.CanSocket;
import de.entropia.can.CanSocket.CanFrame;
import de.entropia.can.CanSocket.CanId;
import de.entropia.can.CanSocket.CanInterface;
import de.entropia.can.CanSocket.Mode;

public class CanConnectionServiceImpl implements CanConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(CanConnectionServiceImpl.class);

    private CanSocket socket = null;

    protected void activate() {
        logger.info("activating CanConnectionService");
    }

    protected void deactivate() {
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                logger.error("Error closing CAN socket");
            }
        }
    }

    @Override
    public void connectCanSocket() throws IOException {
        if (this.socket == null) {
            this.socket = new CanSocket(Mode.RAW);
            this.socket.setLoopbackMode(false);
            this.socket.bind(CanSocket.CAN_ALL_INTERFACES);
        }
    }

    @Override
    public void disconnectCanSocket() throws IOException {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
    }

    @Override
    public void sendCanMessage(String ifName, int canId, byte[] message) throws KuraException, IOException {
        if (message.length > 8) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, "CAN send : Incorrect frame length");
        }
        if (this.socket == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "CAN Socket must be open before sending");
        }

        try {
            CanInterface canif = new CanInterface(this.socket, ifName);
            this.socket.send(new CanFrame(canif, new CanId(canId), message));
        } catch (IOException e) {
            logger.error("Error on CanSocket in sendCanMessage");
            throw e;
        }
    }

    @Override
    public CanMessage receiveCanMessage(int canId, int canMask) throws IOException {
        if (this.socket == null) {
            throw new IllegalStateException("CAN Socket must be open before receiving");
        }
        try {
            if (canId >= 0) {
                this.socket.setCanFilter(canId, canMask);
            }
            CanFrame cf = this.socket.recv();
            CanId ci = cf.getCanId();

            CanMessage cm = new CanMessage();
            cm.setCanId(ci.getCanId_EFF());
            cm.setData(cf.getData());
            return cm;
        } catch (IOException e) {
            logger.error("Error on CanSocket in receiveCanMessage");
            throw e;
        }
    }

}
