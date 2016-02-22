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

package org.eclipse.kura.net.admin.modem.telit.le910;

public enum TelitLe910AtCommands {

	gpsPowerUp("AT$GPSSLSR=2,3\r\n");
	
	private String m_command;
	
	private TelitLe910AtCommands(String atCommand) {
		m_command = atCommand;
	}
	
	public String getCommand () {
		return m_command;
	}
}
