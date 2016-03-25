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
package org.eclipse.kura.protocol.can;

import java.io.IOException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.entropia.can.CanSocket;
import de.entropia.can.CanSocket.CanFrame;
import de.entropia.can.CanSocket.CanId;
import de.entropia.can.CanSocket.CanInterface;
import de.entropia.can.CanSocket.Mode;


public class CanConnectionServiceImpl implements CanConnectionService {

	private static final Logger s_logger = LoggerFactory.getLogger(CanConnectionServiceImpl.class);
	
	protected void activate(ComponentContext componentContext) {
		s_logger.info("activating CanConnectionService");
	}
	
	protected void deactivate(ComponentContext componentContext) {
	}
	
	@Override
	public void sendCanMessage(String ifName, int canId, byte[] message)
			throws KuraException, IOException {
		if(message.length>8)
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "CAN send : Incorrect frame length");
		
		CanSocket socket=null;
    	try {
			socket = new CanSocket(Mode.RAW);
			socket.setLoopbackMode(false);
			CanInterface canif = new CanInterface(socket, ifName);
			socket.bind(canif);
			socket.send(new CanFrame(canif, new CanId(canId), message));
			//s_logger.debug("message sent on " + ifName);
		} catch (IOException e) {
			s_logger.error("Error on CanSocket in sendCanMessage: {}",e.getMessage());
			throw e;
		}finally{
			if(socket!=null)
				socket.close();
		} 	
    }

	@Override
	public CanMessage receiveCanMessage(int can_id, int can_mask) throws KuraException, IOException {
		CanSocket socket=null;
		try {
			socket = new CanSocket(Mode.RAW);
			socket.setLoopbackMode(false);
			socket.bind(CanSocket.CAN_ALL_INTERFACES);
			if(can_id>=0){
				socket.setCanFilter(can_id, can_mask);
			}
			CanFrame cf = socket.recv();
			CanId ci = cf.getCanId();
			//s_logger.debug(cf.toString());
			CanMessage cm = new CanMessage();
			cm.setCanId(ci.getCanId_SFF());
			cm.setData(cf.getData());
			return cm;
		} catch (IOException e) {
			s_logger.error("Error on CanSocket in receiveCanMessage: {}",e.getMessage());
			throw e;
		}finally{
			if(socket!=null)
				socket.close();
		}
	}

}
