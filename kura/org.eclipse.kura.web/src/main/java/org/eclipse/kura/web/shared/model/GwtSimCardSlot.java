package org.eclipse.kura.web.shared.model;

public enum GwtSimCardSlot {

	A(0),
	B(1);
	
	private int m_slot;
	private GwtSimCardSlot(int slot) {
		m_slot = slot;
	}
	
	public int getValue() {
		return m_slot;
	}
}
