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
package org.eclipse.kura.net.admin.modem.telit.de910;

public enum TelitDe910ServiceProviders {
	
	UNKNOWN(-1),
	SPRINT(0),
	AERIS(1),
	VERIZON(2);
	
	private int m_provider;
	
	private TelitDe910ServiceProviders(int provider) {
		m_provider = provider;
	}
	
	public int getProvider () {
		return m_provider;
	}
}
