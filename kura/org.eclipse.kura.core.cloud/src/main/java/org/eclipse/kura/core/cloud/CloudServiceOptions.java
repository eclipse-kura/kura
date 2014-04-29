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

import java.util.Map;

public class CloudServiceOptions 
{
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
	private static final String  ENCODE_GZIP                  = "encode.gzip";
	private static final String  REPUB_BIRTH_ON_GPS_LOCK	  = "republish.mqtt.birth.cert.on.gps.lock";
	
	private static final int     LIFECYCLE_QOS                = 0;
	private static final int     LIFECYCLE_PRIORITY           = 0;
	private static final boolean LIFECYCLE_RETAIN             = false;
	
	
	private Map<String,Object> m_properties;
	
	CloudServiceOptions(Map<String,Object> properties) {
		m_properties = properties;
	}
	
	/**
	 * Returns the display name for the device.
	 * @return
	 */
	public String getDeviceDisplayName() {
		String displayName = DEVICE_DISPLAY_NAME;
		if (m_properties != null &&
			m_properties.get(DEVICE_DISPLAY_NAME) != null &&
			m_properties.get(DEVICE_DISPLAY_NAME) instanceof String) {
			displayName = (String) m_properties.get(DEVICE_DISPLAY_NAME);
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
}

