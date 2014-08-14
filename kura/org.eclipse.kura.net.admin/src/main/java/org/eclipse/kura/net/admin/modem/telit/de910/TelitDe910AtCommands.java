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
package org.eclipse.kura.net.admin.modem.telit.de910;

public enum TelitDe910AtCommands {

	at("at\r\n"),
	getModelNumber("at+gmm\r\n"),
	getManufacturer("at+gmi\r\n"),
	getSerialNumber("at#cgsn\r\n"),
	getRevision("at+gmr\r\n"),
	getSignalStrength("at+csq\r\n"),
	getNetRegistrationStatus("at+creg?\r\n"), 
	getMdn("at#modem=0?\r\n"),
	getMsid("at#modem=1?\r\n"),
	getServiceType("at+service?\r\n"),
	getSessionDataVolume("at#gdatavol=1\r\n"),
	provisionVerizon("atd*22899;\r\n"),
	isGpsLocked("AT$GPSLOCK?\r\n"),
	unlockGps("AT$GPSLOCK=0\r\n"),
	isGpsPowered("AT$GPSP?\r\n"),
	gpsPowerUp("AT$GPSP=1\r\n"),
	gpsPowerDown("AT$GPSP=0\r\n"),
	gpsEnableNMEA("AT$GPSNMUN=3,0,0,1,1,1,1\r\n"),
	gpsDisableNMEA("+++");
	
	private String m_command;
	
	private TelitDe910AtCommands (String atCommand) {
		m_command = atCommand;
	}
	
	public String getCommand () {
		return m_command;
	}
}
