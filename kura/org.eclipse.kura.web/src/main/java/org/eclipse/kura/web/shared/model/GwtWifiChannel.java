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

public enum GwtWifiChannel {
	netWifiChannelAuto,
	netWifiChannel1,
	netWifiChannel2,
	netWifiChannel3,
	netWifiChannel4,
	netWifiChannel5,
	netWifiChannel6,
	netWifiChannel7,
	netWifiChannel8,
	netWifiChannel9,
	netWifiChannel10,
	netWifiChannel11,
	netWifiChannel12,
	netWifiChannel13;
	
	public static GwtWifiChannel valueOf(short channel) {
		try {
			return GwtWifiChannel.valueOf("netWifiChannel" + channel);
		} catch (Exception e) {
			return GwtWifiChannel.netWifiChannelAuto;
		}
	}
	
	public static short toShort(GwtWifiChannel channel) {
		
		switch(channel) {
			case netWifiChannel1:
				return 1;
			case netWifiChannel2:
				return 2;
			case netWifiChannel3:
				return 3;
			case netWifiChannel4:
				return 4;
			case netWifiChannel5:
				return 5;
			case netWifiChannel6:
				return 6;
			case netWifiChannel7:
				return 7;
			case netWifiChannel8:
				return 8;
			case netWifiChannel9:
				return 9;
			case netWifiChannel10:
				return 10;
			case netWifiChannel11:
				return 11;
			case netWifiChannel12:
				return 13;
			case netWifiChannel13:
				return 13;
			default:
		}
		
		// TODO: what value to return for auto?
		return -1;							
	}
}
