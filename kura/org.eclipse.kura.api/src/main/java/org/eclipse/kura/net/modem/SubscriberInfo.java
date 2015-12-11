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
package org.eclipse.kura.net.modem;

public class SubscriberInfo {

	private boolean m_isActive;
	
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
	private String m_subscriberNumber;
	
	/**
	 * Default constructor
	 */
	public SubscriberInfo() {
		m_isActive = false;
		m_imsi = "";
		m_iccid = "";
		m_subscriberNumber = "";
	}
	
	/**
	 * Constructor
	 * 
	 * @param imsi - International Mobile Subscriber Identity as {@link String}
	 * @param iccid - Integrated Circuit Card Identification as {@link String}
	 * @param subscriberNumber - Subscriber Number as {@link String}
	 */
	public SubscriberInfo(String imsi, String iccid, String subscriberNumber) {
		m_isActive = false;
		m_imsi = imsi;
		m_iccid = iccid;
		m_subscriberNumber = subscriberNumber;
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
				+ ((m_subscriberNumber == null) ? 0 : m_subscriberNumber.hashCode());
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
		}
		if (!m_iccid.equals(other.m_iccid)) {
			return false;
		} 
		/* don't compare subscriber number
		if (!m_subscriberNumber.equals(other.m_subscriberNumber)) {
			return false;
		}
		*/
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IMSI=").append(m_imsi);
		sb.append("; ICCID=").append(m_iccid);
		sb.append("; Subscriber Number=").append(m_subscriberNumber);
		return sb.toString();
	}
	
	public boolean isActive() {
		return m_isActive;
	}

	public void setActive(boolean isActive) {
		m_isActive = isActive;
	}

	/**
	 * Reports International Mobile Subscriber Identity
	 * 
	 * @return - International Mobile Subscriber Identity as {@link String}
	 */
	public String getInternationalMobileSubscriberIdentity() {
		return m_imsi;
	}

	/**
	 * Reports Integrated Circuit Card Identification
	 * 
	 * @return Integrated Circuit Card Identification as {@link String}
	 */
	public String getIntegratedCircuitCardIdentification() {
		return m_iccid;
	}

	/**
	 * Reports Subscriber Number
	 * 
	 * @return Subscriber Number as {@link String}
	 */
	public String getSubscriberNumber() {
		return m_subscriberNumber;
	}
	
	/**
	 * Sets International Mobile Subscriber Identity
	 * 
	 * @param imsi - International Mobile Subscriber Identity as {@link String}
	 */
	public void setInternationalMobileSubscriberIdentity(String imsi) {
		m_imsi = imsi;
	}

	/**
	 * Sets Integrated Circuit Card Identification
	 * 
	 * @param iccid - Integrated Circuit Card Identification as {@link String}
	 */
	public void setIntegratedCircuitCardIdentification(String iccid) {
		m_iccid = iccid;
	}

	/**
	 * Sets Subscriber Number
	 * 
	 * @param subscriber - Subscriber Number as {@link String}
	 */
	public void setSubscriberNumber(String subscriberNumber) {
		m_subscriberNumber = subscriberNumber;
	}
}
