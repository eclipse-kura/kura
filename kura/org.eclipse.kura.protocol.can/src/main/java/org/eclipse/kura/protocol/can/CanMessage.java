package org.eclipse.kura.protocol.can;

public class CanMessage {
	private int m_canId;
    private byte[] m_data;

	public byte[] getData() {
		return m_data;
	}

	public void setData(byte[] data) {
		this.m_data = data;
	}

	public int getCanId() {
		return m_canId;
	}

	public void setCanId(int canId) {
		this.m_canId = canId;
	}

}
