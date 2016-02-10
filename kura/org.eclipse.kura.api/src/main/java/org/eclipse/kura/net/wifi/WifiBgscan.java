/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.wifi;

/**
 * Background Scan container class
 */
public class WifiBgscan {

	private WifiBgscanModule m_module = null;
	private int m_shortInterval = 0;
	private int m_longInterval = 0;
	private int m_rssiThreshold = 0;
	
	public WifiBgscan(WifiBgscanModule module, int shortInterval,
			int rssiThreshold, int longInterval) {

		m_module = module;
		m_shortInterval = shortInterval;
		m_rssiThreshold = rssiThreshold;
		m_longInterval = longInterval;
	}
	
	public WifiBgscan (WifiBgscan bgscan) {
		
		m_module = bgscan.m_module;
		m_shortInterval = bgscan.m_shortInterval;
		m_rssiThreshold = bgscan.m_rssiThreshold;
		m_longInterval = bgscan.m_longInterval;
	}
	
	public WifiBgscan(String str) {
		
		if ((str == null) || (str.length() == 0)) {
			m_module = WifiBgscanModule.NONE;
		} else {
			String [] sa = str.split(":");
			if(sa[0].equals("simple")) {
				m_module = WifiBgscanModule.SIMPLE;
			} else if (sa[0].equals("learn")) {
				m_module = WifiBgscanModule.LEARN;
			}
			
			m_shortInterval = Integer.parseInt(sa[1]);
			m_rssiThreshold = Integer.parseInt(sa[2]);
			m_longInterval = Integer.parseInt(sa[3]);
		}
	}
	
	public WifiBgscanModule getModule() {
		return m_module;
	}

	public int getShortInterval() {
		return m_shortInterval;
	}

	public int getLongInterval() {
		return m_longInterval;
	}

	public int getRssiThreshold() {
		return m_rssiThreshold;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals (Object obj) {
		
		if (!(obj instanceof WifiBgscan)) {
			return false;
		}
		
		WifiBgscan bgscan = (WifiBgscan)obj;
		
		if (this.m_module != bgscan.m_module) {
			return false;
		}
		
		if (this.m_rssiThreshold != bgscan.m_rssiThreshold) {
			return false;
		}
		
		if (this.m_shortInterval != bgscan.m_shortInterval) {
			return false;
		}
		
		if (this.m_longInterval != bgscan.m_longInterval) {
			return false;
		}
		
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		
		StringBuffer sb = new StringBuffer();
		if (m_module == WifiBgscanModule.SIMPLE) {
			sb.append("simple:");
		} else if (m_module == WifiBgscanModule.LEARN) {
			sb.append("learn:");
		} else {
			sb.append("");
			return sb.toString();
		}
		
		sb.append(m_shortInterval);
		sb.append(':');
		sb.append(m_rssiThreshold);
		sb.append(':');
		sb.append(m_longInterval);
		
		return sb.toString();
	}
}
