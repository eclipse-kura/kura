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
package org.eclipse.kura.linux.net.modem;

import org.eclipse.kura.linux.net.util.KuraConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedSerialModemsInfo {

	private static final Logger s_logger = LoggerFactory.getLogger(SupportedSerialModemsInfo.class);
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");

	private static SupportedSerialModemInfo m_supportedSerialModemInfo = null;
	
	public static SupportedSerialModemInfo getModem() {

		SupportedSerialModemInfo supportedSerialModemInfo = null;
		if (m_supportedSerialModemInfo != null) {
			supportedSerialModemInfo = m_supportedSerialModemInfo;
		} else {
			if (OS_VERSION != null && OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
					TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
				s_logger.debug("obtaining serial modem info for {}", KuraConstants.Mini_Gateway.getImageName());
				supportedSerialModemInfo = SupportedSerialModems.getModem(KuraConstants.Mini_Gateway.getImageName(), KuraConstants.Mini_Gateway.getImageVersion(), KuraConstants.Mini_Gateway.getTargetName());
				m_supportedSerialModemInfo = supportedSerialModemInfo;
			} else if (OS_VERSION != null && OS_VERSION.equals(KuraConstants.Reliagate_10_11.getImageName() + "_" + KuraConstants.Reliagate_10_11.getImageVersion()) &&
					TARGET_NAME != null && TARGET_NAME.equals(KuraConstants.Reliagate_10_11.getTargetName())) {
				s_logger.debug("obtaining serial modem info for {}", KuraConstants.Reliagate_10_11.getImageName());
				supportedSerialModemInfo = SupportedSerialModems.getModem(KuraConstants.Reliagate_10_11.getImageName(), KuraConstants.Reliagate_10_11.getImageVersion(), KuraConstants.Reliagate_10_11.getTargetName());
				m_supportedSerialModemInfo = supportedSerialModemInfo;				
			}
		}
		return supportedSerialModemInfo;
	}
}
