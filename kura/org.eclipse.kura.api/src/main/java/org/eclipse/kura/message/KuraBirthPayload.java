/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.message;

import static org.eclipse.kura.message.KuraDeviceProfile.APPLICATION_FRAMEWORK_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.APPLICATION_FRAMEWORK_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.AVAILABLE_PROCESSORS_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.BIOS_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.CONNECTION_INTERFACE_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.CONNECTION_IP_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.DEFAULT_APPLICATION_FRAMEWORK;
import static org.eclipse.kura.message.KuraDeviceProfile.DISPLAY_NAME_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.FIRMWARE_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.JVM_NAME_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.JVM_PROFILE_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.JVM_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.KURA_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.MODEL_ID_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.MODEL_NAME_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.OSGI_FRAMEWORK_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.OSGI_FRAMEWORK_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.OS_ARCH_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.OS_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.OS_VERSION_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.PART_NUMBER_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.SERIAL_NUMBER_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.TOTAL_MEMORY_KEY;
import static org.eclipse.kura.message.KuraDeviceProfile.UPTIME_KEY;

/**
 * The KuraBirthPayload is an extension of {@link KuraPayload} that contains the parameters that allow to define the
 * form of a device. The message is usually published when connecting to the broker.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.1
 */
public class KuraBirthPayload extends KuraPayload {

    private static final String ACCEPT_ENCODING_KEY = "accept_encoding";
    private static final String APPLICATION_IDS_KEY = "application_ids";
    private static final String MODEM_IMEI_KEY = "modem_imei";
    private static final String MODEM_IMSI_KEY = "modem_imsi";
    private static final String MODEM_ICCID_KEY = "modem_iccid";
    private static final String MODEM_RSSI_KEY = "modem_rssi";
    private static final String PAYLOAD_ENCODING_KEY = "payload_encoding";

    public String getUptime() {
        return (String) getMetric(UPTIME_KEY);
    }

    public String getDisplayName() {
        return (String) getMetric(DISPLAY_NAME_KEY);
    }

    public String getModelName() {
        return (String) getMetric(MODEL_NAME_KEY);
    }

    public String getModelId() {
        return (String) getMetric(MODEL_ID_KEY);
    }

    public String getPartNumber() {
        return (String) getMetric(PART_NUMBER_KEY);
    }

    public String getSerialNumber() {
        return (String) getMetric(SERIAL_NUMBER_KEY);
    }

    public String getFirmwareVersion() {
        return (String) getMetric(FIRMWARE_VERSION_KEY);
    }

    public String getBiosVersion() {
        return (String) getMetric(BIOS_VERSION_KEY);
    }

    public String getOs() {
        return (String) getMetric(OS_KEY);
    }

    public String getOsVersion() {
        return (String) getMetric(OS_VERSION_KEY);
    }

    public String getJvmName() {
        return (String) getMetric(JVM_NAME_KEY);
    }

    public String getJvmVersion() {
        return (String) getMetric(JVM_VERSION_KEY);
    }

    public String getJvmProfile() {
        return (String) getMetric(JVM_PROFILE_KEY);
    }

    /**
     * @deprecated Use {@link #getApplicationFrameworkVersion()}
     */
    @Deprecated
    public String getKuraVersion() {
        return (String) getMetric(KURA_VERSION_KEY);
    }

    public String getApplicationFramework() {
        final String value = (String) getMetric(APPLICATION_FRAMEWORK_KEY);
        if (value != null) {
            return value;
        }
        return DEFAULT_APPLICATION_FRAMEWORK;
    }

    public String getApplicationFrameworkVersion() {
        final String value = (String) getMetric(APPLICATION_FRAMEWORK_VERSION_KEY);
        if (value != null) {
            return value;
        }
        return (String) getMetric(KURA_VERSION_KEY);
    }

    public String getConnectionInterface() {
        return (String) getMetric(CONNECTION_INTERFACE_KEY);
    }

    public String getConnectionIp() {
        return (String) getMetric(CONNECTION_IP_KEY);
    }

    public String getAcceptEncoding() {
        return (String) getMetric(ACCEPT_ENCODING_KEY);
    }

    public String getApplicationIdentifiers() {
        return (String) getMetric(APPLICATION_IDS_KEY);
    }

    public String getAvailableProcessors() {
        return (String) getMetric(AVAILABLE_PROCESSORS_KEY);
    }

    public String getTotalMemory() {
        return (String) getMetric(TOTAL_MEMORY_KEY);
    }

    public String getOsArch() {
        return (String) getMetric(OS_ARCH_KEY);
    }

    public String getOsgiFramework() {
        return (String) getMetric(OSGI_FRAMEWORK_KEY);
    }

    public String getOsgiFrameworkVersion() {
        return (String) getMetric(OSGI_FRAMEWORK_VERSION_KEY);
    }

    public String getModemImei() {
        return (String) getMetric(MODEM_IMEI_KEY);
    }

    public String getModemImsi() {
        return (String) getMetric(MODEM_IMSI_KEY);
    }

    public String getModemIccid() {
        return (String) getMetric(MODEM_ICCID_KEY);
    }

    public String getModemRssi() {
        return (String) getMetric(MODEM_RSSI_KEY);
    }

    public String getPayloadEncoding() {
        return (String) getMetric(PAYLOAD_ENCODING_KEY);
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
        sb.append("getKuraVersion()=").append(getApplicationFrameworkVersion()).append(", ");
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

    @Override
    public void addMetric(String name, Object value) {
        if (value != null) {
            super.addMetric(name, value);
        }
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

            birthPayload.addMetric(UPTIME_KEY, this.uptime);

            birthPayload.addMetric(DISPLAY_NAME_KEY, this.displayName);
            birthPayload.addMetric(MODEL_NAME_KEY, this.modelName);
            birthPayload.addMetric(MODEL_ID_KEY, this.modelId);
            birthPayload.addMetric(PART_NUMBER_KEY, this.partNumber);
            birthPayload.addMetric(SERIAL_NUMBER_KEY, this.serialNumber);
            birthPayload.addMetric(FIRMWARE_VERSION_KEY, this.firmwareVersion);
            birthPayload.addMetric(BIOS_VERSION_KEY, this.biosVersion);
            birthPayload.addMetric(OS_KEY, this.os);
            birthPayload.addMetric(OS_VERSION_KEY, this.osVersion);
            birthPayload.addMetric(JVM_NAME_KEY, this.jvmName);
            birthPayload.addMetric(JVM_VERSION_KEY, this.jvmVersion);
            birthPayload.addMetric(JVM_PROFILE_KEY, this.jvmProfile);
            birthPayload.addMetric(KURA_VERSION_KEY, this.kuraVersion);
            if (this.applicationFramework != null) {
                birthPayload.addMetric(APPLICATION_FRAMEWORK_KEY, this.applicationFramework);
            } else {
                birthPayload.addMetric(APPLICATION_FRAMEWORK_KEY, DEFAULT_APPLICATION_FRAMEWORK);
            }
            birthPayload.addMetric(KURA_VERSION_KEY, this.applicationFrameworkVersion);
            birthPayload.addMetric(APPLICATION_FRAMEWORK_VERSION_KEY, this.applicationFrameworkVersion);
            birthPayload.addMetric(CONNECTION_INTERFACE_KEY, this.connectionInterface);
            birthPayload.addMetric(CONNECTION_IP_KEY, this.connectionIp);
            birthPayload.addMetric(ACCEPT_ENCODING_KEY, this.acceptEncoding);
            birthPayload.addMetric(APPLICATION_IDS_KEY, this.applicationIdentifiers);
            birthPayload.addMetric(AVAILABLE_PROCESSORS_KEY, this.availableProcessors);
            birthPayload.addMetric(TOTAL_MEMORY_KEY, this.totalMemory);
            birthPayload.addMetric(OS_ARCH_KEY, this.osArch);
            birthPayload.addMetric(OSGI_FRAMEWORK_KEY, this.osgiFramework);
            birthPayload.addMetric(OSGI_FRAMEWORK_VERSION_KEY, this.osgiFrameworkVersion);
            birthPayload.addMetric(MODEM_IMEI_KEY, this.modemImei);
            birthPayload.addMetric(MODEM_ICCID_KEY, this.modemIccid);
            birthPayload.addMetric(MODEM_IMSI_KEY, this.modemImsi);
            birthPayload.addMetric(MODEM_RSSI_KEY, this.modemRssi);
            birthPayload.addMetric(PAYLOAD_ENCODING_KEY, this.payloadEncoding);
            birthPayload.setPosition(this.position);

            return birthPayload;
        }
    }
}
