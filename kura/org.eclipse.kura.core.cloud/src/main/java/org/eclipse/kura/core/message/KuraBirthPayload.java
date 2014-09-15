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
package org.eclipse.kura.core.message;

import java.util.Iterator;

import org.eclipse.kura.message.KuraPayload;

public class KuraBirthPayload extends KuraPayload 
{
	private final static String UPTIME = "uptime";
	private final static String DISPLAY_NAME = "display_name";
	private final static String MODEL_NAME = "model_name";
	private final static String MODEL_ID = "model_id";
	private final static String PART_NUMBER = "part_number";
	private final static String SERIAL_NUMBER = "serial_number";
	private final static String AVAILABLE_PROCESSORS = "available_processors";
	private final static String TOTAL_MEMORY = "total_memory";
	private final static String FIRMWARE_VERSION = "firmware_version";
	private final static String BIOS_VERSION = "bios_version";
	private final static String OS = "os";
	private final static String OS_VERSION = "os_version";
	private final static String OS_ARCH = "os_arch";
	private final static String JVM_NAME = "jvm_name";
	private final static String JVM_VERSION = "jvm_version";
	private final static String JVM_PROFILE = "jvm_profile";
	private final static String KURA_VERSION = "kura_version";
	private final static String OSGI_FRAMEWORK = "osgi_framework";
	private final static String OSGI_FRAMEWORK_VERSION = "osgi_framework_version";
	private final static String CONNECTION_INTERFACE = "connection_interface";
	private final static String CONNECTION_IP = "connection_ip";
	private final static String ACCEPT_ENCODING = "accept_encoding";
	private final static String APPLICATION_IDS = "application_ids";
	public final static String MODEM_IMEI = "modem_imei";
	public final static String MODEM_IMSI = "modem_imsi";
	public final static String MODME_ICCID = "modem_iccid";
	
	public KuraBirthPayload(String uptime, String displayName,
			String modelName, String modelId, String partNumber,
			String serialNumber, String firmwareVersion, String biosVersion,
			String os, String osVersion, String jvmName, String jvmVersion,
			String jvmProfile, String kuraVersion, String connectionInterface, String connectionIp) {
		
		this(uptime, displayName, modelName, modelId, partNumber,
			serialNumber, firmwareVersion, biosVersion,
			os, osVersion, jvmName, jvmVersion,
			jvmProfile, kuraVersion, connectionInterface, connectionIp,
			null, null, null, null, null, null, null);
	}
	
	public KuraBirthPayload(String uptime, String displayName,
			String modelName, String modelId, String partNumber,
			String serialNumber, String firmwareVersion, String biosVersion,
			String os, String osVersion, String jvmName, String jvmVersion,
			String jvmProfile, String kuraVersion, String connectionInterface, String connectionIp,
			String acceptEncoding) {
		
		this(uptime, displayName, modelName, modelId, partNumber,
			serialNumber, firmwareVersion, biosVersion,
			os, osVersion, jvmName, jvmVersion,
			jvmProfile, kuraVersion, connectionInterface, connectionIp,
			acceptEncoding, null, null, null, null, null, null);
	}
	
	public KuraBirthPayload(String uptime, String displayName,
			String modelName, String modelId, String partNumber,
			String serialNumber, String firmwareVersion, String biosVersion,
			String os, String osVersion, String jvmName, String jvmVersion,
			String jvmProfile, String kuraVersion, String connectionInterface, String connectionIp,
			String acceptEncoding, String applicationIdentifiers) {
		
		this(uptime, displayName, modelName, modelId, partNumber,
				serialNumber, firmwareVersion, biosVersion,
				os, osVersion, jvmName, jvmVersion,
				jvmProfile, kuraVersion, connectionInterface, connectionIp,
				acceptEncoding, applicationIdentifiers,
				null, null, null, null, null);
	}
	
	public KuraBirthPayload(String uptime, String displayName,
			String modelName, String modelId, String partNumber,
			String serialNumber, String firmwareVersion, String biosVersion,
			String os, String osVersion, String jvmName, String jvmVersion,
			String jvmProfile, String kuraVersion, String connectionInterface, String connectionIp,
			String acceptEncoding, String applicationIdentifiers,
			String availableProcessors, String totalMemory, String osArch,
			String osgiFramework, String osgiFrameworkVersion) {
		super();
		
		if (uptime != null) {
			addMetric(UPTIME, uptime);
		}
		if (displayName != null) {
			addMetric(DISPLAY_NAME, displayName);
		}
		if (modelName != null) {
			addMetric(MODEL_NAME, modelName);
		}
		if (modelId != null) {
			addMetric(MODEL_ID, modelId);
		}
		if (partNumber != null) {
			addMetric(PART_NUMBER, partNumber);
		}
		if (serialNumber != null) {
			addMetric(SERIAL_NUMBER, serialNumber);
		}
		if (firmwareVersion != null) {
			addMetric(FIRMWARE_VERSION, firmwareVersion);
		}
		if (biosVersion != null) {
			addMetric(BIOS_VERSION, biosVersion);
		}
		if (os != null) {
			addMetric(OS, os);
		}
		if (osVersion != null) {
			addMetric(OS_VERSION, osVersion);
		}
		if (jvmName != null) {
			addMetric(JVM_NAME, jvmName);
		}
		if (jvmVersion != null) {
			addMetric(JVM_VERSION, jvmVersion);
		}
		if (jvmProfile != null) {
			addMetric(JVM_PROFILE, jvmProfile);
		}
		if (kuraVersion != null) {
			addMetric(KURA_VERSION, kuraVersion);
		}
		if (connectionInterface != null) {
			addMetric(CONNECTION_INTERFACE, connectionInterface);
		}
		if (connectionIp != null) {
			addMetric(CONNECTION_IP, connectionIp);
		}
		if (acceptEncoding != null) {
			addMetric(ACCEPT_ENCODING, acceptEncoding);
		}
		if (applicationIdentifiers != null) {
			addMetric(APPLICATION_IDS, applicationIdentifiers);
		}
		if (availableProcessors != null) {
			addMetric(AVAILABLE_PROCESSORS, availableProcessors);
		}
		if (totalMemory != null) {
			addMetric(TOTAL_MEMORY, totalMemory);
		}
		if (osArch != null) {
			addMetric(OS_ARCH, osArch);
		}
		if (osgiFramework != null) {
			addMetric(OSGI_FRAMEWORK, osgiFramework);
		}
		if (osgiFrameworkVersion != null) {
			addMetric(OSGI_FRAMEWORK_VERSION, osgiFrameworkVersion);
		}
	}
	
	public KuraBirthPayload(KuraPayload edcMessage) {	    
		Iterator<String> hdrIterator = edcMessage.metricsIterator();		
		while (hdrIterator.hasNext()) {
			String hdrName = hdrIterator.next();
			String hdrVal = (String)edcMessage.getMetric(hdrName);			
			addMetric(hdrName, hdrVal);
		}		
		setBody(edcMessage.getBody());
		setPosition(edcMessage.getPosition());
	}

	public String getUptime() {
		return (String) getMetric(UPTIME);
	}
	public String getDisplayName() {
		return (String) getMetric(DISPLAY_NAME);
	}
	public String getModelName() {
		return (String) getMetric(MODEL_NAME);
	}
	public String getModelId() {
		return (String) getMetric(MODEL_ID);
	}
	public String getPartNumber() {
		return (String) getMetric(PART_NUMBER);
	}
	public String getSerialNumber() {
		return (String) getMetric(SERIAL_NUMBER);
	}
	public String getFirmwareVersion() {
		return (String) getMetric(FIRMWARE_VERSION);
	}
	public String getBiosVersion() {
		return (String) getMetric(BIOS_VERSION);
	}
	public String getOs() {
		return (String) getMetric(OS);
	}
	public String getOsVersion() {
		return (String) getMetric(OS_VERSION);
	}
	public String getJvmName() {
		return (String) getMetric(JVM_NAME);
	}
	public String getJvmVersion() {
		return (String) getMetric(JVM_VERSION);
	}
	public String getJvmProfile() {
		return (String) getMetric(JVM_PROFILE);
	}
	public String getKuraVersion() {
		return (String) getMetric(KURA_VERSION);
	}
	public String getConnectionInterface() {
		return (String) getMetric(CONNECTION_INTERFACE);
	}
	public String getConnectionIp() {
		return (String) getMetric(CONNECTION_IP);
	}
	public String getAcceptEncoding() {
		return (String) getMetric(ACCEPT_ENCODING);
	}
	public String getApplicationIdentifiers() {
		return (String) getMetric(APPLICATION_IDS);
	}

	public String getAvailableProcessors() {
		return (String) getMetric(AVAILABLE_PROCESSORS);
	}

	public String getTotalMemory() {
		return (String) getMetric(TOTAL_MEMORY);
	}

	public String getOsArch() {
		return (String) getMetric(OS_ARCH);
	}

	public String getOsgiFramework() {
		return (String) getMetric(OSGI_FRAMEWORK);
	}

	public String getOsgiFrameworkVersion() {
		return (String) getMetric(OSGI_FRAMEWORK_VERSION);
	}

	@Override
	public String toString() {
		return "EdcBirthMessage [getUptime()=" + getUptime() + ", getDisplayName()="
				+ getDisplayName() + ", getModelName()=" + getModelName()
				+ ", getModelId()=" + getModelId() + ", getPartNumber()="
				+ getPartNumber() + ", getSerialNumber()=" + getSerialNumber()
				+ ", getFirmwareVersion()=" + getFirmwareVersion()
				+ ", getAvailableProcessors()=" + getAvailableProcessors()
				+ ", getTotalMemory()=" + getTotalMemory()
				+ ", getBiosVersion()=" + getBiosVersion() + ", getOs()="
				+ getOs() + ", getOsVersion()=" + getOsVersion()
				+ ", getOsArch()=" + getOsArch()
				+ ", getJvmName()=" + getJvmName() + ", getJvmVersion()="
				+ getJvmVersion() + ", getJvmProfile()=" + getJvmProfile()
				+ ", getKuraVersion()=" + getKuraVersion()
				+ ", getOsgiFramework()=" + getOsgiFramework()
				+ ", getOsgiFrameworkVersion()=" + getOsgiFrameworkVersion()
				+ ", getConnectionInterface()=" + getConnectionInterface()
				+ ", getConnectionIp()=" + getConnectionIp()
				+ ", getAcceptEncoding()=" + getAcceptEncoding()
				+ ", getApplicationIdentifiers()=" + getApplicationIdentifiers() + "]";
	}
}
