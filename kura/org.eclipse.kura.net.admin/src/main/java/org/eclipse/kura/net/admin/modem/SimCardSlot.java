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
			ret = A;
			break;
		case 1:
			ret = B;
			break;
		}
		return ret;
	}
}
