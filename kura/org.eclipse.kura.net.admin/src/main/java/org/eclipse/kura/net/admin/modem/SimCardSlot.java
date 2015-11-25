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
package org.eclipse.kura.net.admin.modem;

public enum SimCardSlot {

	A(0),
	B(1);
	
	private int m_slot;
	private SimCardSlot(int slot) {
		m_slot = slot;
	}
	
	public int getValue() {
		return m_slot;
	}
	
	public static SimCardSlot getSimCardSlot(int slot) {
		SimCardSlot ret = null;
		switch (slot) {
		case 0:
			ret = B;
			break;
		case 1:
			ret = A;
			break;
		}
		return ret;
	}
}
