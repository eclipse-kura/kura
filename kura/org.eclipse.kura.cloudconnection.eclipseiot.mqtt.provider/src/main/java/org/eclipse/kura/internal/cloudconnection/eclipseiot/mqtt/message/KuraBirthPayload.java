/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraPosition;

public class KuraBirthPayload extends KuraPayload {

    private static final String UPTIME = "uptime";
    private static final String DISPLAY_NAME = "display_name";
    private static final String MODEL_NAME = "model_name";
    private static final String MODEL_ID = "model_id";
    private static final String PART_NUMBER = "part_number";
    private static final String SERIAL_NUMBER = "serial_number";
    private static final String AVAILABLE_PROCESSORS = "available_processors";
    private static final String TOTAL_MEMORY = "total_memory";
    private static final String FIRMWARE_VERSION = "firmware_version";
    private static final String BIOS_VERSION = "bios_version";
    private static final String OS = "os";
    private static final String OS_VERSION = "os_version";
    private static final String OS_ARCH = "os_arch";
    private static final String JVM_NAME = "jvm_name";
    private static final String JVM_VERSION = "jvm_version";
    private static final String JVM_PROFILE = "jvm_profile";
    private static final String KURA_VERSION = "kura_version";
    private static final String APPLICATION_FRAMEWORK = "application_framework";
    private static final String APPLICATION_FRAMEWORK_VERSION = "application_framework_version";
    private static final String OSGI_FRAMEWORK = "osgi_framework";
    private static final String OSGI_FRAMEWORK_VERSION = "osgi_framework_version";
    private static final String CONNECTION_INTERFACE = "connection_interface";
    private static final String CONNECTION_IP = "connection_ip";
    private static final String ACCEPT_ENCODING = "accept_encoding";
    private static final String APPLICATION_IDS = "application_ids";
    private static final String MODEM_IMEI = "modem_imei";
    private static final String MODEM_IMSI = "modem_imsi";
    private static final String MODEM_ICCID = "modem_iccid";
    private static final String MODEM_RSSI = "modem_rssi";
    private static final String PAYLOAD_ENCODING = "payload_encoding";

    private static final String DEFAULT_APPLICATION_FRAMEWORK = "Kura";

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

    /**
     * @deprecated Use {@link #getApplicationFrameworkVersion()}
     */
    @Deprecated
    public String getKuraVersion() {
        return (String) getMetric(KURA_VERSION);
    }

    public String getApplicationFramework() {
        final String value = (String) getMetric(APPLICATION_FRAMEWORK);
        if (value != null) {
            return value;
        }
        return DEFAULT_APPLICATION_FRAMEWORK;
    }

    public String getApplicationFrameworkVersion() {
        final String value = (String) getMetric(APPLICATION_FRAMEWORK_VERSION);
        if (value != null) {
            return value;
        }
        return getKuraVersion();
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

    public String getPayloadEncoding() {
        return (String) getMetric(PAYLOAD_ENCODING);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KuraBirthPayload [");

        sb.append("getUptime()=").append(getUptime()).append(", ");
        sb.append("getDisplayName()=").append(getDisplayName()).append(", ");
        sb.append("getModelName()=").append(getModelName()).append(", ");
        sb.append("getModelId()=").append(getModelId()).append(", ");
        sb.append("getPartNumber()=").append(getPartNumber()).append(", ");
        sb.append("getSerialNumber()=").append(getSerialNumber()).append(", ");
        sb.append("getFirmwareVersion()=").append(getFirmwareVersion()).append(", ");
        sb.append("getAvailableProcessors()=").append(getAvailableProcessors()).append(", ");
        sb.append("getTotalMemory()=").append(getTotalMemory()).append(", ");
        sb.append("getBiosVersion()=").append(getBiosVersion()).append(", ");
        sb.append("getOs()=").append(getOs()).append(", ");
        sb.append("getOsVersion()=").append(getOsVersion()).append(", ");
        sb.append("getOsArch()=").append(getOsArch()).append(", ");
        sb.append("getJvmName()=").append(getJvmName()).append(", ");
        sb.append("getJvmVersion()=").append(getJvmVersion()).append(", ");
        sb.append("getJvmProfile()=").append(getJvmProfile()).append(", ");
        sb.append("getKuraVersion()=").append(getKuraVersion()).append(", ");
        sb.append("getApplicationFramework()=").append(getApplicationFramework()).append(", ");
        sb.append("getApplicationFrameworkVersion()=").append(getApplicationFrameworkVersion()).append(", ");
        sb.append("getOsgiFramework()=").append(getOsgiFramework()).append(", ");
        sb.append("getOsgiFrameworkVersion()=").append(getOsgiFrameworkVersion()).append(", ");
        sb.append("getConnectionInterface()=").append(getConnectionInterface()).append(", ");
        sb.append("getConnectionIp()=").append(getConnectionIp()).append(", ");
        sb.append("getAcceptEncoding()=").append(getAcceptEncoding()).append(", ");
        sb.append("getApplicationIdentifiers()=").append(getApplicationIdentifiers()).append(", ");
        sb.append("getPayloadEncoding()=").append(getPayloadEncoding());

        sb.append("]");

        return sb.toString();
    }

    public static class KuraBirthPayloadBuilder {

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
        private String applicationFramework;
        private String applicationFrameworkVersion;
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
        private String payloadEncoding;

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
            withApplicationFramework(DEFAULT_APPLICATION_FRAMEWORK);
            withApplicationFrameworkVersion(kuraVersion);
            return this;
        }

        public KuraBirthPayloadBuilder withApplicationFramework(String applicationFramework) {
            this.applicationFramework = applicationFramework;
            return this;
        }

        public KuraBirthPayloadBuilder withApplicationFrameworkVersion(String applicationFrameworkVersion) {
            this.applicationFrameworkVersion = applicationFrameworkVersion;
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

        public KuraBirthPayloadBuilder withPayloadEncoding(String payloadEncoding) {
            this.payloadEncoding = payloadEncoding;
            return this;
        }

        public KuraBirthPayload build() {
            KuraBirthPayload birthPayload = new KuraBirthPayload();

            if (this.uptime != null) {
                birthPayload.addMetric(UPTIME, this.uptime);
            }
            if (this.displayName != null) {
                birthPayload.addMetric(DISPLAY_NAME, this.displayName);
            }
            if (this.modelName != null) {
                birthPayload.addMetric(MODEL_NAME, this.modelName);
            }
            if (this.modelId != null) {
                birthPayload.addMetric(MODEL_ID, this.modelId);
            }
            if (this.partNumber != null) {
                birthPayload.addMetric(PART_NUMBER, this.partNumber);
            }
            if (this.serialNumber != null) {
                birthPayload.addMetric(SERIAL_NUMBER, this.serialNumber);
            }
            if (this.firmwareVersion != null) {
                birthPayload.addMetric(FIRMWARE_VERSION, this.firmwareVersion);
            }
            if (this.biosVersion != null) {
                birthPayload.addMetric(BIOS_VERSION, this.biosVersion);
            }
            if (this.os != null) {
                birthPayload.addMetric(OS, this.os);
            }
            if (this.osVersion != null) {
                birthPayload.addMetric(OS_VERSION, this.osVersion);
            }
            if (this.jvmName != null) {
                birthPayload.addMetric(JVM_NAME, this.jvmName);
            }
            if (this.jvmVersion != null) {
                birthPayload.addMetric(JVM_VERSION, this.jvmVersion);
            }
            if (this.jvmProfile != null) {
                birthPayload.addMetric(JVM_PROFILE, this.jvmProfile);
            }
            if (this.kuraVersion != null) {
                birthPayload.addMetric(KURA_VERSION, this.kuraVersion);
            }
            if (this.applicationFramework != null) {
                birthPayload.addMetric(APPLICATION_FRAMEWORK, this.applicationFramework);
            } else {
                birthPayload.addMetric(APPLICATION_FRAMEWORK, DEFAULT_APPLICATION_FRAMEWORK);
            }
            if (this.applicationFrameworkVersion != null) {
                birthPayload.addMetric(KURA_VERSION, this.applicationFrameworkVersion);
                birthPayload.addMetric(APPLICATION_FRAMEWORK_VERSION, this.applicationFrameworkVersion);
            }
            if (this.connectionInterface != null) {
                birthPayload.addMetric(CONNECTION_INTERFACE, this.connectionInterface);
            }
            if (this.connectionIp != null) {
                birthPayload.addMetric(CONNECTION_IP, this.connectionIp);
            }
            if (this.acceptEncoding != null) {
                birthPayload.addMetric(ACCEPT_ENCODING, this.acceptEncoding);
            }
            if (this.applicationIdentifiers != null) {
                birthPayload.addMetric(APPLICATION_IDS, this.applicationIdentifiers);
            }
            if (this.availableProcessors != null) {
                birthPayload.addMetric(AVAILABLE_PROCESSORS, this.availableProcessors);
            }
            if (this.totalMemory != null) {
                birthPayload.addMetric(TOTAL_MEMORY, this.totalMemory);
            }
            if (this.osArch != null) {
                birthPayload.addMetric(OS_ARCH, this.osArch);
            }
            if (this.osgiFramework != null) {
                birthPayload.addMetric(OSGI_FRAMEWORK, this.osgiFramework);
            }
            if (this.osgiFrameworkVersion != null) {
                birthPayload.addMetric(OSGI_FRAMEWORK_VERSION, this.osgiFrameworkVersion);
            }
            if (this.modemImei != null) {
                birthPayload.addMetric(MODEM_IMEI, this.modemImei);
            }
            if (this.modemIccid != null) {
                birthPayload.addMetric(MODEM_ICCID, this.modemIccid);
            }
            if (this.modemImsi != null) {
                birthPayload.addMetric(MODEM_IMSI, this.modemImsi);
            }
            if (this.modemRssi != null) {
                birthPayload.addMetric(MODEM_RSSI, this.modemRssi);
            }
            if (this.payloadEncoding != null) {
                birthPayload.addMetric(PAYLOAD_ENCODING, this.payloadEncoding);
            }
            if (this.position != null) {
                birthPayload.setPosition(this.position);
            }

            return birthPayload;
        }
    }
}
