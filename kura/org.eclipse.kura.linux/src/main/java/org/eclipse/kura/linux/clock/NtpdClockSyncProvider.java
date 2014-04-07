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
package org.eclipse.kura.linux.clock;

import java.util.Date;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtpdClockSyncProvider extends AbstractNtpClockSyncProvider
{
	private static final Logger s_logger = LoggerFactory.getLogger(NtpdClockSyncProvider.class);

	// ----------------------------------------------------------------
	//
	//   Concrete Methods
	//
	// ----------------------------------------------------------------	
	
	protected void syncClock() throws KuraException
	{
		Process proc = null;
		try {			
			// Execute a native Linux command to perform the NTP time sync.
			int ntpTimeout = m_ntpTimeout / 1000;
			proc = ProcessUtil.exec("ntpdate -t "+ntpTimeout+" "+m_ntpHost);
			proc.waitFor();
			if (proc.exitValue() == 0) {
				s_logger.info("System Clock Synchronized with "+m_ntpHost);
				m_lastSync = new Date();
				
				// Call update method with 0 offset to ensure the clock event gets fired and the HW clock
				// is updated if desired.
				m_listener.onClockUpdate(0);
			}
			else {
				s_logger.error("Unexpected error while Synchronizing System Clock with "+m_ntpHost);
			}
		} 
		catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			ProcessUtil.destroy(proc);
		}
	}
}
