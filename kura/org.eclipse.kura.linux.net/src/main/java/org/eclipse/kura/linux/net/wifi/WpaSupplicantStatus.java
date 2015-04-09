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
package org.eclipse.kura.linux.net.wifi;

import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WpaSupplicantStatus {
	
	private static Logger s_logger = LoggerFactory.getLogger(WpaSupplicantStatus.class);
	
	private static final String PROP_MODE = "mode";
	private static final String PROP_BSSID = "bssid";
	private static final String PROP_SSID = "ssid";
	private static final String PROP_PAIRWISE_CIPHER = "pairwise_cipher";
	private static final String PROP_GROUP_CIPHER = "group_cipher";
	private static final String PROP_KEY_MGMT = "key_mgmt";
	private static final String PROP_WPA_STATE = "wpa_state";
	private static final String PROP_IP_ADDRESS = "ip_address";
	private static final String PROP_ADDRESS = "address";
	
	private Properties m_props = null;
	
	public WpaSupplicantStatus (String iface) throws KuraException {
		
		m_props = new Properties();
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec(formSupplicantStatusCommand(iface));
			if (proc.waitFor() != 0) {
				s_logger.error("error executing command --- {} --- exit value = {}", formSupplicantStatusCommand(iface), proc.exitValue());
				throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
			}

			m_props.load(proc.getInputStream());
			
			Enumeration<Object> keys = m_props.keys();
			while (keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				s_logger.trace("[WpaSupplicant Status] {} = {}", key, m_props.getProperty(key));
			}
		} catch (Exception e) {
			throw new KuraException (KuraErrorCode.INTERNAL_ERROR, e);
		}
		finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}		
	}
	
	public String getMode () {
		return m_props.getProperty(PROP_MODE);
	}
	
	public String getBssid () {	
		return m_props.getProperty(PROP_BSSID);
	}
	
	public String getSsid () {
		return m_props.getProperty(PROP_SSID);
	}
	
	public String getPairwiseCipher () {
		return m_props.getProperty(PROP_PAIRWISE_CIPHER);
	}
	
	public String getGroupCipher () {
		return m_props.getProperty(PROP_GROUP_CIPHER);
	}
	
	public String getKeyMgmt () {
		return m_props.getProperty(PROP_KEY_MGMT);
	}
	
	public String getWpaState () {
		return m_props.getProperty(PROP_WPA_STATE);
	}
	
	public String getIpAddress () {
		return m_props.getProperty(PROP_IP_ADDRESS);
	}
	
	public String getAddress () {
		return m_props.getProperty(PROP_ADDRESS);
	}
	
	private static String formSupplicantStatusCommand (String iface) {
		StringBuilder sb = new StringBuilder();
		sb.append("wpa_cli -i ");
		sb.append(iface);
		sb.append(" status");
		return sb.toString();
	}
}