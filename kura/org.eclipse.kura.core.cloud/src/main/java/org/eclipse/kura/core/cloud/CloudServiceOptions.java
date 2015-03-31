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
package org.eclipse.kura.core.cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServiceOptions 
{
	private static final Logger s_logger = LoggerFactory.getLogger(CloudServiceOptions.class);
	
	private static final String  TOPIC_SEPARATOR              = "/";
	private static final String  TOPIC_ACCOUNT_TOKEN          = "#account-name";
	private static final String  TOPIC_CLIENT_ID_TOKEN        = "#client-id";
	private static final String  TOPIC_BIRTH_SUFFIX           = "MQTT/BIRTH";
	private static final String  TOPIC_DISCONNECT_SUFFIX      = "MQTT/DC";	
	private static final String  TOPIC_APPS_SUFFIX            = "MQTT/APPS";
	private static final String  TOPIC_CONTROL_PREFIX         = "topic.control-prefix";
	private static final String  TOPIC_CONTROL_PREFIX_DEFAULT = "$EDC";
	private static final String  TOPIC_WILD_CARD              = "#";
	
	private static final String  DEVICE_DISPLAY_NAME          = "device.display-name";
	private static final String  DEVICE_CUSTOM_NAME			  = "device.custom-name";
	private static final String  ENCODE_GZIP                  = "encode.gzip";
	private static final String  REPUB_BIRTH_ON_GPS_LOCK	  = "republish.mqtt.birth.cert.on.gps.lock";
	private static final String  REPUB_BIRTH_ON_MODEM_DETECT  = "republish.mqtt.birth.cert.on.modem.detect";
	
	private static final int     LIFECYCLE_QOS                = 0;
	private static final int     LIFECYCLE_PRIORITY           = 0;
	private static final boolean LIFECYCLE_RETAIN             = false;
	
	
	private Map<String,Object> m_properties;
	private SystemService	   m_systemService;
	
	CloudServiceOptions(Map<String,Object> properties, SystemService systemService) {
		m_properties = properties;
		m_systemService = systemService;
	}
	
	/**
	 * Returns the display name for the device.
	 * @return
	 */
	public String getDeviceDisplayName() {
		String displayName = "";
		if (m_properties != null) {
			String deviceDisplayNameOption = (String) m_properties.get(DEVICE_DISPLAY_NAME);
			
			// Use the device name from SystemService. This should be kura.device.name from 
			// the properties file.
			if (deviceDisplayNameOption.equals("device-name")) {
				displayName = m_systemService.getDeviceName();
				return displayName;
			}
			// Try to get the device hostname
			else if (deviceDisplayNameOption.equals("hostname")) {
				displayName = "UNKNOWN";
				if(SystemService.OS_MAC_OSX.equals(m_systemService.getOsName())) {
					String displayTmp = getHostname("scutil --get ComputerName");
					if(displayTmp.length() > 0) {
						displayName = displayTmp;
					}
				}
				else if (SystemService.OS_LINUX.equals(m_systemService.getOsName()) || SystemService.OS_CLOUDBEES.equals(m_systemService.getOsName())) {
					String displayTmp = getHostname("hostname");
					if(displayTmp.length() > 0) {
						displayName = displayTmp;
					}
				}	
				return displayName;
			}
			// Return the custom field defined by the user
			else if (deviceDisplayNameOption.equals("custom")) {
				if (m_properties.get(DEVICE_CUSTOM_NAME) != null &&
					m_properties.get(DEVICE_CUSTOM_NAME) instanceof String) {
						displayName = (String) m_properties.get(DEVICE_CUSTOM_NAME);
				}
				return displayName;
			}
			// Return empty string to the server
			else if (deviceDisplayNameOption.equals("server")) {
				displayName = "";
				return displayName;
			}
		}
		
		return displayName;
	}
		
	/**
	 * Returns true if the current CloudService configuration 
	 * specifies Gzip compression enabled for outgoing payloads. 
	 * @return
	 */
	public boolean getEncodeGzip() {
		boolean encodeGzip = false;
		if (m_properties != null &&
			m_properties.get(ENCODE_GZIP) != null &&
			m_properties.get(ENCODE_GZIP) instanceof Boolean) {
			encodeGzip = (Boolean) m_properties.get(ENCODE_GZIP);
		}
		return encodeGzip;
	}	
	
	/**
	 * Returns true if the current CloudService configuration 
	 * specifies the cloud client should republish the MQTT birth
	 * certificate on GPS lock events 
	 * @return
	 */
	public boolean getRepubBirthCertOnGpsLock() {
		boolean repubBirth = false;
		if (m_properties != null &&
			m_properties.get(REPUB_BIRTH_ON_GPS_LOCK) != null &&
			m_properties.get(REPUB_BIRTH_ON_GPS_LOCK) instanceof Boolean) {
			repubBirth = (Boolean) m_properties.get(REPUB_BIRTH_ON_GPS_LOCK);
		}
		return repubBirth;
	}
	
	/**
	 * Returns true if the current CloudService configuration 
	 * specifies the cloud client should republish the MQTT birth
	 * certificate on modem detection events 
	 * @return
	 */
	public boolean getRepubBirthCertOnModemDetection() {
		boolean repubBirth = false;
		if (m_properties != null &&
			m_properties.get(REPUB_BIRTH_ON_MODEM_DETECT) != null &&
			m_properties.get(REPUB_BIRTH_ON_MODEM_DETECT) instanceof Boolean) {
			repubBirth = (Boolean) m_properties.get(REPUB_BIRTH_ON_MODEM_DETECT);
		}
		return repubBirth;
	}	

	/**
	 * Returns the prefix to be used when publishing messages to control topics.
	 * @return
	 */
	public String getTopicControlPrefix() {
		String prefix = TOPIC_CONTROL_PREFIX_DEFAULT;
		if (m_properties != null &&
			m_properties.get(TOPIC_CONTROL_PREFIX) != null &&
			m_properties.get(TOPIC_CONTROL_PREFIX) instanceof String) {
			prefix = (String) m_properties.get(TOPIC_CONTROL_PREFIX);
		}
		return prefix;
	}

	public String getTopicSeparator() {
		return TOPIC_SEPARATOR;
	}

	public String getTopicAccountToken() {
		return TOPIC_ACCOUNT_TOKEN;
	}

	public String getTopicClientIdToken() {
		return TOPIC_CLIENT_ID_TOKEN;
	}

	public String getTopicBirthSuffix() {
		return TOPIC_BIRTH_SUFFIX;
	}
	
	public String getTopicDisconnectSuffix() {
		return TOPIC_DISCONNECT_SUFFIX;
	}
	
	public String getTopicAppsSuffix() {
		return TOPIC_APPS_SUFFIX;
	}

	public String getTopicWildCard() {
		return TOPIC_WILD_CARD;
	}

	public int getLifeCycleMessageQos() {
		return LIFECYCLE_QOS;
	}

	public int getLifeCycleMessagePriority() {
		return LIFECYCLE_PRIORITY;
	}

	public boolean getLifeCycleMessageRetain() {
		return LIFECYCLE_RETAIN;
	}
	
	private String getHostname(String command) {
		StringBuffer response = new StringBuffer(); 
		SafeProcess proc = null;
		BufferedReader br = null;
		try {
			proc = ProcessUtil.exec(command);
			proc.waitFor();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			String newLine = "";
			while ((line = br.readLine()) != null) {
				response.append(newLine);
				response.append(line);
				newLine = "\n";
			}
		} catch(Exception e) {
			s_logger.error("failed to run commands " + command, e);
		}
		finally {
			if(br != null){
				try{
					br.close();
				}catch(IOException ex){
					s_logger.error("I/O Exception while closing BufferedReader!");
				}
			}
			if (proc != null) ProcessUtil.destroy(proc);
		}
		return response.toString();
	}
}

