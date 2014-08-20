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
package org.eclipse.kura.linux.net.modem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.kura.linux.net.util.KuraConstants;

public class SupportedSerialModems {

	private static final Logger s_logger = LoggerFactory.getLogger(SupportedSerialModems.class);
	private static final String OS_VERSION = System.getProperty("kura.os.version");
	private static final String TARGET_NAME = System.getProperty("target.device");
	
	private static boolean modemReachable = false;
	static {
		breakout:
		for (SupportedSerialModemInfo modem : SupportedSerialModemInfo.values()) {
			if (modem == SupportedSerialModemInfo.MiniGateway_Telit_HE910_NAD) {
				if (OS_VERSION.equals(KuraConstants.Mini_Gateway.getImageName() + "_" + KuraConstants.Mini_Gateway.getImageVersion()) &&
						TARGET_NAME.equals(KuraConstants.Mini_Gateway.getTargetName())) {
					s_logger.info("Installing modem driver for {} ...", modem.getModemName());
					try {
						if (!SupportedUsbModems.isAttached(
								SupportedUsbModemInfo.Telit_HE910_D.getVendorId(),
								SupportedUsbModemInfo.Telit_HE910_D.getProductId())) {
							if (modem.getDriver().install() == 0) {
								for (String modemModel : modem.getModemModels()) {
									if (modemModel.equals(modem.getDriver().getModemModel())) {
										s_logger.info("Driver for the {} modem has been installed" , modemModel);
										modemReachable = true;
										break breakout;
									}
								}
							}
							s_logger.warn("Failed to installing modem driver for {}", modem.getModemName());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public static SupportedSerialModemInfo getModem(String imageName, String imageVersion) {
		
		SupportedSerialModemInfo supportedSerialModemInfo = null;
		
		for (SupportedSerialModemInfo modem : SupportedSerialModemInfo.values()) {
			if (modem.getOsImageName().equals(imageName) && modem.getOsImageVersion().equals(imageVersion)) {
				if (modemReachable) {
					s_logger.debug("The {} modem is attached", modem.getModemName());
					supportedSerialModemInfo = modem;
				} else {
					// do not return this modem if it isn't reachable
					s_logger.debug("The {} modem is not attached", modem.getModemName());
				}
				break;
			}
		}
		
		return supportedSerialModemInfo;
	}
}
