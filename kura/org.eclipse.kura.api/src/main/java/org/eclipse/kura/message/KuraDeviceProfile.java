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

/**
 * The KuraDeviceProfile is a container class that holds the parameters that make up the from of a device.
 * This information is used to build the birth and disconnect certificates that are published when
 * connecting to and disconnecting from the broker.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.1
 *
 */
public class KuraDeviceProfile {

    public static final String UPTIME_KEY = "uptime";
    public static final String DISPLAY_NAME_KEY = "display_name";
    public static final String MODEL_NAME_KEY = "model_name";
    public static final String MODEL_ID_KEY = "model_id";
    public static final String PART_NUMBER_KEY = "part_number";
    public static final String SERIAL_NUMBER_KEY = "serial_number";
    public static final String AVAILABLE_PROCESSORS_KEY = "available_processors";
    public static final String TOTAL_MEMORY_KEY = "total_memory";
    public static final String FIRMWARE_VERSION_KEY = "firmware_version";
    public static final String BIOS_VERSION_KEY = "bios_version";
    public static final String OS_KEY = "os";
    public static final String OS_VERSION_KEY = "os_version";
    public static final String OS_ARCH_KEY = "os_arch";
    public static final String JVM_NAME_KEY = "jvm_name";
    public static final String JVM_VERSION_KEY = "jvm_version";
    public static final String JVM_PROFILE_KEY = "jvm_profile";
    public static final String KURA_VERSION_KEY = "kura_version";
    public static final String APPLICATION_FRAMEWORK_KEY = "application_framework";
    public static final String APPLICATION_FRAMEWORK_VERSION_KEY = "application_framework_version";
    public static final String OSGI_FRAMEWORK_KEY = "osgi_framework";
    public static final String OSGI_FRAMEWORK_VERSION_KEY = "osgi_framework_version";
    public static final String CONNECTION_INTERFACE_KEY = "connection_interface";
    public static final String CONNECTION_IP_KEY = "connection_ip";

    public static final String DEFAULT_APPLICATION_FRAMEWORK = "Kura";

    private String uptime;
    private String displayName;
    private String modelName;
    private String modelId;
    private String partNumber;
    private String serialNumber;
    private String availableProcessors;
    private String totalMemory;
    private String firmwareVersion;
    private String biosVersion;
    private String os;
    private String osVersion;
    private String osArch;
    private String jvmName;
    private String jvmVersion;
    private String jvmProfile;
    private String applicationFramework;
    private String applicationFrameworkVersion;
    private String osgiFramework;
    private String osgiFrameworkVersion;
    private String connectionInterface;
    private String connectionIp;
    private Double latitude;
    private Double longitude;
    private Double altitude;

    /**
     * Empty constructor for a KuraDeviceProfile.
     */
    public KuraDeviceProfile() {
        // Values filled with setters
    }

    /**
     * Returns The length of time the unit has been powered on.
     *
     * @return A String representing the length of time the device has been powered on.
     */
    public String getUptime() {
        return this.uptime;
    }

    /**
     * Returns the readable display name for the device.
     *
     * @return A String representing the readable display name for the device.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Returns the device model name
     *
     * @return A String representing the device model name
     */
    public String getModelName() {
        return this.modelName;
    }

    /**
     * Returns the device model ID.
     *
     * @return A String representing the device model ID.
     */
    public String getModelId() {
        return this.modelId;
    }

    /**
     * Returns the part number of the device.
     *
     * @return A String representing the part number of the device.
     */
    public String getPartNumber() {
        return this.partNumber;
    }

    /**
     * Returns the serial number of the device.
     *
     * @return A String representing the serial number of the device.
     */
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * Returns the version of firmware running on the device.
     *
     * @return A String representing the version of firmware running on the device.
     */
    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Returns the version of the BIOS on the device.
     *
     * @return A String representing the version of the BIOS on the device.
     */
    public String getBiosVersion() {
        return this.biosVersion;
    }

    /**
     * Returns the name of the operating system.
     *
     * @return A String representing the name of the operating system.
     */
    public String getOs() {
        return this.os;
    }

    /**
     * Returns the version of the operating system.
     *
     * @return A String representing the version of the operating system.
     */
    public String getOsVersion() {
        return this.osVersion;
    }

    /**
     * Returns the name of the JVM.
     *
     * @return A String representing the name of the JVM.
     */
    public String getJvmName() {
        return this.jvmName;
    }

    /**
     * Returns the version of the JVM.
     *
     * @return A String representing the version of the JVM.
     */
    public String getJvmVersion() {
        return this.jvmVersion;
    }

    /**
     * Returns the profile of the JVM.
     *
     * @return A String representing the profile of the JVM.
     */
    public String getJvmProfile() {
        return this.jvmProfile;
    }

    /**
     * Returns the Kura version.
     *
     * @return A String representing the Kura version
     * @deprecated use {@link #getApplicationFrameworkVersion()} instead
     */
    @Deprecated
    public String getKuraVersion() {
        return this.applicationFrameworkVersion;
    }

    /**
     * Returns the Application Framework.
     *
     * @return A String representing the Application Framework
     */
    public String getApplicationFramework() {
        return this.applicationFramework;
    }

    /**
     * Returns the Application Framework version.
     *
     * @return A String representing the Application Framework version
     */
    public String getApplicationFrameworkVersion() {
        return this.applicationFrameworkVersion;
    }

    /**
     * Returns the name of the interface used to connect to the cloud.
     *
     * @return A String representing the name of the interface used to connect to the cloud.
     */
    public String getConnectionInterface() {
        return this.connectionInterface;
    }

    /**
     * Returns the IP address of the interface used to connect to the cloud.
     *
     * @return A String representing the IP address of the interface used to connect to the cloud.
     */
    public String getConnectionIp() {
        return this.connectionIp;
    }

    /**
     * Returns the latitude of the device's location.
     *
     * @return A String representing the latitude of the device's location.
     */
    public Double getLatitude() {
        return this.latitude;
    }

    /**
     * Returns the longitude of the device's location.
     *
     * @return A String representing the longitude of the device's location.
     */
    public Double getLongitude() {
        return this.longitude;
    }

    /**
     * Returns the altitude of the device's location.
     *
     * @return A String representing thealtitude of the device's location.
     */
    public Double getAltitude() {
        return this.altitude;
    }

    /**
     * Sets the length of time the unit has been powered on.
     *
     * @param uptime
     *            A String representing the length of time the unit has been powered on.
     */
    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    /**
     * Sets the readable display name for the device
     *
     * @param displayName
     *            A String representing the readable display name for the device
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the device model name.
     *
     * @param modelName
     *            A String representing the device model name.
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Sets the device model ID.
     *
     * @param modelId
     *            A String representing the device model ID.
     */
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    /**
     * Sets the part number of the device.
     *
     * @param partNumber
     *            A String representing the part number of the device.
     */
    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Sets the serial number of the device.
     *
     * @param serialNumber
     *            A String representing the serial number of the device.
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Sets the version of firmware running on the device.
     *
     * @param firmwareVersion
     *            A String representing the version of firmware running on the device.
     */
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Sets the version of the BIOS on the device.
     *
     * @param biosVersion
     *            A String representing the version of the BIOS on the device.
     */
    public void setBiosVersion(String biosVersion) {
        this.biosVersion = biosVersion;
    }

    /**
     * Sets the name of the operating system.
     *
     * @param os
     *            A String representing the name of the operating system.
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * Sets the version of the operating system.
     *
     * @param osVersion
     *            A String representing the version of the operating system.
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * Sets the name of the JVM.
     *
     * @param jvmName
     *            A String representing the name of the JVM.
     */
    public void setJvmName(String jvmName) {
        this.jvmName = jvmName;
    }

    /**
     * Sets the version of the JVM.
     *
     * @param jvmVersion
     *            A String representing the version of the JVM.
     */
    public void setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
    }

    /**
     * Sets the profile of the JVM.
     *
     * @param jvmProfile
     *            A String representing the profile of the JVM.
     */
    public void setJvmProfile(String jvmProfile) {
        this.jvmProfile = jvmProfile;
    }

    /**
     * Sets the name of the interface used to connect to the cloud.
     *
     * @param connectionInterface
     *            A String representing the name of the interface used to connect to the cloud.
     */
    public void setConnectionInterface(String connectionInterface) {
        this.connectionInterface = connectionInterface;
    }

    /**
     * Sets the IP address of the interface used to connect to the cloud.
     *
     * @param connectionIp
     *            A String representing the IP address of the interface used to connect to the cloud.
     */
    public void setConnectionIp(String connectionIp) {
        this.connectionIp = connectionIp;
    }

    /**
     * Sets the latitude of the device's location.
     *
     * @param latitude
     *            A String representing the latitude of the device's location.
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Sets the longitude of the device's location.
     *
     * @param longitude
     *            A String representing the longitude of the device's location.
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * Sets the altitude of the device's location.
     *
     * @param altitude
     *            A String representing the altitude of the device's location.
     */
    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public String getAvailableProcessors() {
        return this.availableProcessors;
    }

    public void setAvailableProcessors(String availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public void setApplicationFramework(String applicationFramework) {
        this.applicationFramework = applicationFramework;
    }

    public void setApplicationFrameworkVersion(String applicationFrameworkVersion) {
        this.applicationFrameworkVersion = applicationFrameworkVersion;
    }

    public String getTotalMemory() {
        return this.totalMemory;
    }

    public void setTotalMemory(String totalMemory) {
        this.totalMemory = totalMemory;
    }

    public String getOsArch() {
        return this.osArch;
    }

    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }

    public String getOsgiFramework() {
        return this.osgiFramework;
    }

    public void setOsgiFramework(String osgiFramework) {
        this.osgiFramework = osgiFramework;
    }

    public String getOsgiFrameworkVersion() {
        return this.osgiFrameworkVersion;
    }

    public void setOsgiFrameworkVersion(String osgiFrameworkVersion) {
        this.osgiFrameworkVersion = osgiFrameworkVersion;
    }
}
