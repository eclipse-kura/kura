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

import java.util.Properties;

/**
 * The KuraDeviceProfile is a container class that holds the parameters that make up the from of a device.
 * This information is used to build the birth and disconnect certificates that are published when
 * connecting to and disconnecting from the broker.
 *
 */
public class KuraDeviceProfile {

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

    private static final String DEFAULT_APPLICATION_FRAMEWORK = "Kura";

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
     * Empty constructor for an KuraDeviceProfile.
     */
    public KuraDeviceProfile() {
    }

    /**
     * Constructs an KuraDeviceProfile from a Properties object that contains all the parameters
     * that make up the profile.
     *
     * @param props
     *            A Properties object containing all the device parameters.
     */
    public KuraDeviceProfile(Properties props) {
        this(props.getProperty(UPTIME), props.getProperty(DISPLAY_NAME), props.getProperty(MODEL_NAME),
                props.getProperty(MODEL_ID), props.getProperty(PART_NUMBER), props.getProperty(SERIAL_NUMBER),
                props.getProperty(FIRMWARE_VERSION), props.getProperty(BIOS_VERSION), props.getProperty(OS),
                props.getProperty(OS_VERSION), props.getProperty(JVM_NAME), props.getProperty(JVM_VERSION),
                props.getProperty(JVM_PROFILE), extractApplicationFramework(props),
                extractApplicationFrameworkVersion(props), props.getProperty(CONNECTION_INTERFACE),
                props.getProperty(CONNECTION_IP), props.getProperty(AVAILABLE_PROCESSORS),
                props.getProperty(TOTAL_MEMORY), props.getProperty(OS_ARCH), props.getProperty(OSGI_FRAMEWORK),
                props.getProperty(OSGI_FRAMEWORK_VERSION));
    }

    private static String extractApplicationFramework(Properties props) {
        return props.getProperty(APPLICATION_FRAMEWORK, DEFAULT_APPLICATION_FRAMEWORK);
    }

    private static String extractApplicationFrameworkVersion(Properties props) {
        return props.getProperty(APPLICATION_FRAMEWORK_VERSION, props.getProperty(KURA_VERSION));
    }

    /**
     * Creates an KuraDeviceProfile using the values of the supplied parameters. This constructor
     * also contains values for the GPS location of the device.
     *
     * @param uptime
     *            The length of time the unit has been powered on.
     * @param displayName
     *            A readable display name for the device.
     * @param modelName
     *            The device model name.
     * @param modelId
     *            The device model ID.
     * @param partNumber
     *            The part number of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param firmwareVersion
     *            The version of firmware running on the device.
     * @param biosVersion
     *            The version of the BIOS on the device.
     * @param os
     *            The name of the operating system
     * @param osVersion
     *            The version of the operating system
     * @param jvmName
     *            The name of the JVM
     * @param jvmVersion
     *            The version of the JVM
     * @param jvmProfile
     *            The profile of the JVM
     * @param kuraVersion
     *            The Kura version
     * @param connectionInterface
     *            The name of the interface used to connect to the cloud
     * @param connectionIp
     *            The IP address of the interface used to connect to the cloud
     * @param latitude
     *            The latitude of the device's location
     * @param longitude
     *            The longitude of the device's location
     * @param altitude
     *            The altitude of the device's location
     */
    public KuraDeviceProfile(String uptime, String displayName, String modelName, String modelId, String partNumber,
            String serialNumber, String firmwareVersion, String biosVersion, String os, String osVersion,
            String jvmName, String jvmVersion, String jvmProfile, String kuraVersion, String connectionInterface,
            String connectionIp, Double latitude, Double longitude, Double altitude) {
        this(uptime, displayName, modelName, modelId, partNumber, serialNumber, firmwareVersion, biosVersion, os,
                osVersion, jvmName, jvmVersion, jvmProfile, kuraVersion, connectionInterface, connectionIp);
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    /**
     * Creates an KuraDeviceProfile using the values of the supplied parameters.
     *
     * @param uptime
     *            The length of time the unit has been powered on.
     * @param displayName
     *            A readable display name for the device.
     * @param modelName
     *            The device model name.
     * @param modelId
     *            The device model ID.
     * @param partNumber
     *            The part number of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param firmwareVersion
     *            The version of firmware running on the device.
     * @param biosVersion
     *            The version of the BIOS on the device.
     * @param os
     *            The name of the operating system
     * @param osVersion
     *            The version of the operating system
     * @param jvmName
     *            The name of the JVM
     * @param jvmVersion
     *            The version of the JVM
     * @param jvmProfile
     *            The profile of the JVM
     * @param kuraVersion
     *            The Kura version
     * @param connectionInterface
     *            The name of the interface used to connect to the cloud
     * @param connectionIp
     *            The IP address of the interface used to connect to the cloud
     */
    public KuraDeviceProfile(String uptime, String displayName, String modelName, String modelId, String partNumber,
            String serialNumber, String firmwareVersion, String biosVersion, String os, String osVersion,
            String jvmName, String jvmVersion, String jvmProfile, String kuraVersion, String connectionInterface,
            String connectionIp) {
        this(uptime, displayName, modelName, modelId, partNumber, serialNumber, firmwareVersion, biosVersion, os,
                osVersion, jvmName, jvmVersion, jvmProfile, DEFAULT_APPLICATION_FRAMEWORK, kuraVersion,
                connectionInterface, connectionIp);
    }

    /**
     * Creates an KuraDeviceProfile using the values of the supplied parameters.
     *
     * @param uptime
     *            The length of time the unit has been powered on.
     * @param displayName
     *            A readable display name for the device.
     * @param modelName
     *            The device model name.
     * @param modelId
     *            The device model ID.
     * @param partNumber
     *            The part number of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param firmwareVersion
     *            The version of firmware running on the device.
     * @param biosVersion
     *            The version of the BIOS on the device.
     * @param os
     *            The name of the operating system
     * @param osVersion
     *            The version of the operating system
     * @param jvmName
     *            The name of the JVM
     * @param jvmVersion
     *            The version of the JVM
     * @param jvmProfile
     *            The profile of the JVM
     * @param applicationFramework
     *            The application framework
     * @param applicationFrameworkVersion
     *            The application framework version
     * @param connectionInterface
     *            The name of the interface used to connect to the cloud
     * @param connectionIp
     *            The IP address of the interface used to connect to the cloud
     */
    public KuraDeviceProfile(String uptime, String displayName, String modelName, String modelId, String partNumber,
            String serialNumber, String firmwareVersion, String biosVersion, String os, String osVersion,
            String jvmName, String jvmVersion, String jvmProfile, String applicationFramework,
            String applicationFrameworkVersion, String connectionInterface, String connectionIp) {
        this.uptime = uptime;
        this.displayName = displayName;
        this.modelName = modelName;
        this.modelId = modelId;
        this.partNumber = partNumber;
        this.serialNumber = serialNumber;
        this.firmwareVersion = firmwareVersion;
        this.biosVersion = biosVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.jvmName = jvmName;
        this.jvmVersion = jvmVersion;
        this.jvmProfile = jvmProfile;
        this.applicationFramework = applicationFramework;
        this.applicationFrameworkVersion = applicationFrameworkVersion;
        this.connectionInterface = connectionInterface;
        this.connectionIp = connectionIp;
    }

    /**
     * Creates an KuraDeviceProfile using the values of the supplied parameters.
     *
     * @param uptime
     *            The length of time the unit has been powered on.
     * @param displayName
     *            A readable display name for the device.
     * @param modelName
     *            The device model name.
     * @param modelId
     *            The device model ID.
     * @param partNumber
     *            The part number of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param firmwareVersion
     *            The version of firmware running on the device.
     * @param biosVersion
     *            The version of the BIOS on the device.
     * @param os
     *            The name of the operating system
     * @param osVersion
     *            The version of the operating system
     * @param jvmName
     *            The name of the JVM
     * @param jvmVersion
     *            The version of the JVM
     * @param jvmProfile
     *            The profile of the JVM
     * @param kuraVersion
     *            The Kura version
     * @param connectionInterface
     *            The name of the interface used to connect to the cloud
     * @param connectionIp
     *            The IP address of the interface used to connect to the cloud
     * @param availableProcessors
     *            The number of available processors for the JVM
     * @param totalMemory
     *            The total memory available for the JVM
     * @param osArch
     *            The architecture of the JVM (32 or 64)
     * @param osgiFramework
     *            The OSGI Framework in use
     * @param osgiFrameworkVersion
     *            The version of the OSGI Framework in use
     */
    public KuraDeviceProfile(String uptime, String displayName, String modelName, String modelId, String partNumber,
            String serialNumber, String firmwareVersion, String biosVersion, String os, String osVersion,
            String jvmName, String jvmVersion, String jvmProfile, String kuraVersion, String connectionInterface,
            String connectionIp, String availableProcessors, String totalMemory, String osArch, String osgiFramework,
            String osgiFrameworkVersion) {
        this(uptime, displayName, modelName, modelId, partNumber, serialNumber, firmwareVersion, biosVersion, os,
                osVersion, jvmName, jvmVersion, jvmProfile, DEFAULT_APPLICATION_FRAMEWORK, kuraVersion,
                connectionInterface, connectionIp, availableProcessors, totalMemory, osArch, osgiFramework,
                osgiFrameworkVersion);
    }

    /**
     * Creates an KuraDeviceProfile using the values of the supplied parameters.
     *
     * @param uptime
     *            The length of time the unit has been powered on.
     * @param displayName
     *            A readable display name for the device.
     * @param modelName
     *            The device model name.
     * @param modelId
     *            The device model ID.
     * @param partNumber
     *            The part number of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param firmwareVersion
     *            The version of firmware running on the device.
     * @param biosVersion
     *            The version of the BIOS on the device.
     * @param os
     *            The name of the operating system
     * @param osVersion
     *            The version of the operating system
     * @param jvmName
     *            The name of the JVM
     * @param jvmVersion
     *            The version of the JVM
     * @param jvmProfile
     *            The profile of the JVM
     * @param applicationFramework
     *            The application framework
     * @param applicationFrameworkVersion
     *            The application framework version
     * @param connectionInterface
     *            The name of the interface used to connect to the cloud
     * @param connectionIp
     *            The IP address of the interface used to connect to the cloud
     * @param availableProcessors
     *            The number of available processors for the JVM
     * @param totalMemory
     *            The total memory available for the JVM
     * @param osArch
     *            The architecture of the JVM (32 or 64)
     * @param osgiFramework
     *            The OSGI Framework in use
     * @param osgiFrameworkVersion
     *            The version of the OSGI Framework in use
     */
    public KuraDeviceProfile(String uptime, String displayName, String modelName, String modelId, String partNumber,
            String serialNumber, String firmwareVersion, String biosVersion, String os, String osVersion,
            String jvmName, String jvmVersion, String jvmProfile, String applicationFramework,
            String applicationFrameworkVersion, String connectionInterface, String connectionIp,
            String availableProcessors, String totalMemory, String osArch, String osgiFramework,
            String osgiFrameworkVersion) {
        this.uptime = uptime;
        this.displayName = displayName;
        this.modelName = modelName;
        this.modelId = modelId;
        this.partNumber = partNumber;
        this.serialNumber = serialNumber;
        this.firmwareVersion = firmwareVersion;
        this.biosVersion = biosVersion;
        this.os = os;
        this.osVersion = osVersion;
        this.jvmName = jvmName;
        this.jvmVersion = jvmVersion;
        this.jvmProfile = jvmProfile;
        this.applicationFramework = applicationFramework;
        this.applicationFrameworkVersion = applicationFrameworkVersion;
        this.connectionInterface = connectionInterface;
        this.connectionIp = connectionIp;
        setAvailableProcessors(availableProcessors);
        setTotalMemory(totalMemory);
        setOsArch(osArch);
        setOsgiFramework(osgiFramework);
        setOsgiFrameworkVersion(osgiFrameworkVersion);
    }

    /**
     * Creates an KuraDeviceProfile using the values of the supplied parameters. This constructor
     * also contains values for the GPS location of the device.
     *
     * @param uptime
     *            The length of time the unit has been powered on.
     * @param displayName
     *            A readable display name for the device.
     * @param modelName
     *            The device model name.
     * @param modelId
     *            The device model ID.
     * @param partNumber
     *            The part number of the device.
     * @param serialNumber
     *            The serial number of the device.
     * @param firmwareVersion
     *            The version of firmware running on the device.
     * @param biosVersion
     *            The version of the BIOS on the device.
     * @param os
     *            The name of the operating system
     * @param osVersion
     *            The version of the operating system
     * @param jvmName
     *            The name of the JVM
     * @param jvmVersion
     *            The version of the JVM
     * @param jvmProfile
     *            The profile of the JVM
     * @param kuraVersion
     *            The Kura version
     * @param connectionInterface
     *            The name of the interface used to connect to the cloud
     * @param connectionIp
     *            The IP address of the interface used to connect to the cloud
     * @param latitude
     *            The latitude of the device's location
     * @param longitude
     *            The longitude of the device's location
     * @param altitude
     *            The altitude of the device's location
     */
    public KuraDeviceProfile(String uptime, String displayName, String modelName, String modelId, String partNumber,
            String serialNumber, String firmwareVersion, String biosVersion, String os, String osVersion,
            String jvmName, String jvmVersion, String jvmProfile, String kuraVersion, String connectionInterface,
            String connectionIp, Double latitude, Double longitude, Double altitude, String availableProcessors,
            String totalMemory, String osArch, String osgiFramework, String osgiFrameworkVersion) {
        this(uptime, displayName, modelName, modelId, partNumber, serialNumber, firmwareVersion, biosVersion, os,
                osVersion, jvmName, jvmVersion, jvmProfile, kuraVersion, connectionInterface, connectionIp,
                availableProcessors, totalMemory, osArch, osgiFramework, osgiFrameworkVersion);
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
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
