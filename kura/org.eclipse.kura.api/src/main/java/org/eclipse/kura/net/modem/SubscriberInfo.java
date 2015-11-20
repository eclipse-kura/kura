package org.eclipse.kura.net.modem;

public class SubscriberInfo {

	/*
	 * International Mobile Subscriber Identity
	 * 
	 */
	private String m_imsi;
	
	/*
	 * Integrated Circuit Card Identification
	 * This is card identification number that provides a unique identification number for the SIM
	 */
	private String m_iccid;
	
	/*
	 * Subscriber Number
	 * This is phone number of the device
	 */
	private String m_subscriber;
	
	public SubscriberInfo(String imsi, String iccid, String subscriber) {
		m_imsi = imsi;
		m_iccid = iccid;
		m_subscriber = subscriber;
	}
	
	@Override
	public int hashCode() {
		final int prime = 59;
		int result = super.hashCode();
		result = prime * result
				+ ((m_imsi == null) ? 0 : m_imsi.hashCode());
		result = prime * result
				+ ((m_iccid == null) ? 0 : m_iccid.hashCode());
		result = prime * result
				+ ((m_subscriber == null) ? 0 : m_subscriber.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof SubscriberInfo)) {
			return false;
		}
		SubscriberInfo other = (SubscriberInfo)obj;
		if (!m_imsi.equals(other.m_imsi)) {
			return false;
		} else if (!m_iccid.equals(other.m_iccid)) {
			return false;
		} else if (!m_subscriber.equals(other.m_subscriber)) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IMSI=").append(m_imsi);
		sb.append("; ICCID=").append(m_iccid);
		sb.append("; Subscriber Number=").append(m_subscriber);
		return sb.toString();
	}

	public String getInternationalMobileSubscriberIdentity() {
		return m_imsi;
	}

	public String getIntegratedCircuitCardIdentification() {
		return m_iccid;
	}

	public String getSubscriberNumber() {
		return m_subscriber;
	}
	
	public void setInternationalMobileSubscriberIdentity(String imsi) {
		m_imsi = imsi;
	}

	public void setIntegratedCircuitCardIdentification(String iccid) {
		m_iccid = iccid;
	}

	public void setSubscriberNumber(String subscriber) {
		m_subscriber = subscriber;
	}
}
