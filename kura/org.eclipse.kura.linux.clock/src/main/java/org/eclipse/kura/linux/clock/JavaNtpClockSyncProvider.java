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
package org.eclipse.kura.linux.clock;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaNtpClockSyncProvider extends AbstractNtpClockSyncProvider
{
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(JavaNtpClockSyncProvider.class);
	
	
	// ----------------------------------------------------------------
	//
	//   Concrete Methods
	//
	// ----------------------------------------------------------------	
	
	protected boolean syncClock() throws KuraException
	{
		boolean ret = false;
		// connect and get the delta
		NTPUDPClient ntpClient = new NTPUDPClient();
        ntpClient.setDefaultTimeout(m_ntpTimeout);
        try {
            ntpClient.open();
            try {
            	InetAddress ntpHostAddr = InetAddress.getByName(m_ntpHost);
            	TimeInfo info = ntpClient.getTime(ntpHostAddr, m_ntpPort);
            	m_lastSync = new Date();
                info.computeDetails();
                
                m_listener.onClockUpdate(info.getOffset());
                ret = true;
            } catch (IOException e) {
            	ntpClient.close();
				s_logger.warn(
						"Error while synchronizing System Clock with NTP host {}. Please verify network connectivity ...",
						m_ntpHost);
            }
        } 
        catch (Exception e) {
        	throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return ret;
	}
}
