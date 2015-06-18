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
package org.eclipse.kura.web.shared.model;

public enum GwtBSWifiRadioMode {
	netWifiRadioModeBGN,
	netWifiRadioModeBG,
	netWifiRadioModeB,
	netWifiRadioModeA;
	
	/**
	 * Return mode based on given string
	 * 
	 * @param mode - "a", "b", "g", or "n"
	 * @return
	 */
	public static GwtBSWifiRadioMode getRadioMode(String mode) {

		if ("a".equals(mode)) {
			return GwtBSWifiRadioMode.netWifiRadioModeA;
		} else if ("b".equals(mode)) {
			return GwtBSWifiRadioMode.netWifiRadioModeB;
		} else if ("g".equals(mode)) {
			return GwtBSWifiRadioMode.netWifiRadioModeBG;
		} else if ("n".equals(mode)) {
			return GwtBSWifiRadioMode.netWifiRadioModeBGN;
		}
		
		return null;
	}
}
