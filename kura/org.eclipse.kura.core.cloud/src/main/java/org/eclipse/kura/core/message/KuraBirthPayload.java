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

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;

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
	private final static String MODEM_IMEI = "modem_imei";
	private final static String MODEM_IMSI = "modem_imsi";
	private final static String MODEM_ICCID = "modem_iccid";
	private final static String MODEM_RSSI = "modem_rssi";


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
	public String getModemImei() {
		return (String) getMetric(MODEM_IMEI);
	}
	public String getModemImsi() {
		return (String) getMetric(MODEM_IMSI);
	}
	public String getModemIccid() {
		return (String) getMetric(MODEM_ICCID);
	}
	public String getModemRssi() {
		return (String) getMetric(MODEM_RSSI);
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

	public static class KuraBirthPayloadBuilder
	{
		private String uptime;
		private String displayName;
		private String availableProcessors;
		private String totalMemory;
		private String osArch;
		private String modelName;
		private String modelId;
		private String partNumber;
		private String serialNumber;
		private String firmwareVersion;
		private String biosVersion;
		private String os;
		private String osVersion;
		private String jvmName;
		private String jvmVersion;
		private String jvmProfile;
		private String kuraVersion;
		private String connectionInterface;
		private String connectionIp;
		private String acceptEncoding;
		private String applicationIdentifiers;
		private String osgiFramework;
		private String osgiFrameworkVersion;
		private String modemImei;
		private String modemIccid;
		private String modemImsi;
		private String modemRssi;

		private KuraPosition position;


		public KuraBirthPayloadBuilder withUptime(String uptime) {
			this.uptime = uptime;
			return this;
		}
		public KuraBirthPayloadBuilder withDisplayName(String displayName) {
			this.displayName = displayName;
			return this;
		}
		public KuraBirthPayloadBuilder withAvailableProcessors(String availableProcessors) {
			this.availableProcessors = availableProcessors;
			return this;
		}
		public KuraBirthPayloadBuilder withTotalMemory(String totalMemory) {
			this.totalMemory = totalMemory;
			return this;
		}
		public KuraBirthPayloadBuilder withOsArch(String osArch) {
			this.osArch = osArch;
			return this;
		}
		public KuraBirthPayloadBuilder withOsgiFramework(String osgiFramework) {
			this.osgiFramework = osgiFramework;
			return this;
		}		
		public KuraBirthPayloadBuilder withOsgiFrameworkVersion(String osgiFrameworkVersion) {
			this.osgiFrameworkVersion = osgiFrameworkVersion;
			return this;
		}
		public KuraBirthPayloadBuilder withModelName(String modelName) {
			this.modelName = modelName;
			return this;
		}
		public KuraBirthPayloadBuilder withModelId(String modelId) {
			this.modelId = modelId;
			return this;
		}
		public KuraBirthPayloadBuilder withPartNumber(String partNumber) {
			this.partNumber = partNumber;
			return this;
		}
		public KuraBirthPayloadBuilder withSerialNumber(String serialNumber) {
			this.serialNumber = serialNumber;
			return this;
		}
		public KuraBirthPayloadBuilder withFirmwareVersion(String firmwareVersion) {
			this.firmwareVersion = firmwareVersion;
			return this;
		}
		public KuraBirthPayloadBuilder withBiosVersion(String biosVersion) {
			this.biosVersion = biosVersion;
			return this;
		}
		public KuraBirthPayloadBuilder withOs(String os) {
			this.os = os;
			return this;
		}
		public KuraBirthPayloadBuilder withOsVersion(String osVersion) {
			this.osVersion = osVersion;
			return this;
		}
		public KuraBirthPayloadBuilder withJvmName(String jvmName) {
			this.jvmName = jvmName;
			return this;
		}
		public KuraBirthPayloadBuilder withJvmVersion(String jvmVersion) {
			this.jvmVersion = jvmVersion;
			return this;
		}
		public KuraBirthPayloadBuilder withJvmProfile(String jvmProfile) {
			this.jvmProfile = jvmProfile;
			return this;
		}
		public KuraBirthPayloadBuilder withKuraVersion(String kuraVersion) {
			this.kuraVersion = kuraVersion;
			return this;
		}
		public KuraBirthPayloadBuilder withConnectionInterface(String connectionInterface) {
			this.connectionInterface = connectionInterface;
			return this;
		}
		public KuraBirthPayloadBuilder withConnectionIp(String connectionIp) {
			this.connectionIp = connectionIp;
			return this;
		}
		public KuraBirthPayloadBuilder withAcceptEncoding(String acceptEncoding) {
			this.acceptEncoding = acceptEncoding;
			return this;
		}
		public KuraBirthPayloadBuilder withApplicationIdentifiers(String applicationIdentifiers) {
			this.applicationIdentifiers = applicationIdentifiers;
			return this;
		}
		public KuraBirthPayloadBuilder withModemImei(String modemImei) {
			this.modemImei = modemImei;
			return this;
		}
		public KuraBirthPayloadBuilder withModemIccid(String modemIccid) {
			this.modemIccid = modemIccid;
			return this;
		}
		public KuraBirthPayloadBuilder withModemImsi(String modemImsi) {
			this.modemImsi = modemImsi;
			return this;
		}
		public KuraBirthPayloadBuilder withModemRssi(String modemRssi) {
			this.modemRssi = modemRssi;
			return this;
		}
		public KuraBirthPayloadBuilder withPosition(KuraPosition position) {
			this.position = position;
			return this;			
		}

		public KuraBirthPayload build() {
			KuraBirthPayload birthPayload = new KuraBirthPayload();

			if (uptime != null) {
				birthPayload.addMetric(UPTIME, uptime);
			}
			if (displayName != null) {
				birthPayload.addMetric(DISPLAY_NAME, displayName);
			}
			if (modelName != null) {
				birthPayload.addMetric(MODEL_NAME, modelName);
			}
			if (modelId != null) {
				birthPayload.addMetric(MODEL_ID, modelId);
			}
			if (partNumber != null) {
				birthPayload.addMetric(PART_NUMBER, partNumber);
			}
			if (serialNumber != null) {
				birthPayload.addMetric(SERIAL_NUMBER, serialNumber);
			}
			if (firmwareVersion != null) {
				birthPayload.addMetric(FIRMWARE_VERSION, firmwareVersion);
			}
			if (biosVersion != null) {
				birthPayload.addMetric(BIOS_VERSION, biosVersion);
			}
			if (os != null) {
				birthPayload.addMetric(OS, os);
			}
			if (osVersion != null) {
				birthPayload.addMetric(OS_VERSION, osVersion);
			}
			if (jvmName != null) {
				birthPayload.addMetric(JVM_NAME, jvmName);
			}
			if (jvmVersion != null) {
				birthPayload.addMetric(JVM_VERSION, jvmVersion);
			}
			if (jvmProfile != null) {
				birthPayload.addMetric(JVM_PROFILE, jvmProfile);
			}
			if (kuraVersion != null) {
				birthPayload.addMetric(KURA_VERSION, kuraVersion);
			}
			if (connectionInterface != null) {
				birthPayload.addMetric(CONNECTION_INTERFACE, connectionInterface);
			}
			if (connectionIp != null) {
				birthPayload.addMetric(CONNECTION_IP, connectionIp);
			}
			if (acceptEncoding != null) {
				birthPayload.addMetric(ACCEPT_ENCODING, acceptEncoding);
			}
			if (applicationIdentifiers != null) {
				birthPayload.addMetric(APPLICATION_IDS, applicationIdentifiers);
			}
			if (availableProcessors != null) {
				birthPayload.addMetric(AVAILABLE_PROCESSORS, availableProcessors);
			}
			if (totalMemory != null) {
				birthPayload.addMetric(TOTAL_MEMORY, totalMemory);
			}
			if (osArch != null) {
				birthPayload.addMetric(OS_ARCH, osArch);
			}
			if (osgiFramework != null) {
				birthPayload.addMetric(OSGI_FRAMEWORK, osgiFramework);
			}
			if (osgiFrameworkVersion != null) {
				birthPayload.addMetric(OSGI_FRAMEWORK_VERSION, osgiFrameworkVersion);
			}
			if (modemImei != null) {
				birthPayload.addMetric(MODEM_IMEI, modemImei);
			}
			if (modemIccid != null) {
				birthPayload.addMetric(MODEM_ICCID, modemIccid);
			}
			if (modemImsi != null) {
				birthPayload.addMetric(MODEM_IMSI, modemImsi);
			}
			if (modemRssi != null) {
				birthPayload.addMetric(MODEM_RSSI, modemRssi);
			}
			if (position != null) {
				birthPayload.setPosition(position);
			}

			return birthPayload;
		}
	}
}
