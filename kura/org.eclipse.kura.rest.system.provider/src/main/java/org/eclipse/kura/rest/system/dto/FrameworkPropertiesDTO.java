/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.system.dto;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.kura.system.SystemService;

@SuppressWarnings("unused")
public class FrameworkPropertiesDTO {

    // hardware
    private String biosVersion;
    private String cpuVersion;
    private String deviceName;
    private String modelId;
    private String modelName;
    private String partNumber;
    private String platform;
    private Integer numberOfProcessors;
    private Long totalMemory;
    private Long freeMemory;
    private String serialNumber;

    // java
    private String javaHome;
    private String javaVendor;
    private String javaVersion;
    private String javaVmInfo;
    private String javaVmName;
    private String javaVmVersion;

    // os
    private String osArch;
    private String osDistro;
    private String osDistroVersion;
    private String osName;
    private String osVersion;
    private Boolean isLegacyBluetoothBeaconScan;
    private Boolean isLegacyPPPLoggingEnabled;
    private String primaryMacAddress;
    private String primaryNetworkInterfaceName;
    private String fileSeparator;
    private String firmwareVersion;

    // kura
    private String kuraDataDirectory;
    private String kuraFrameworkConfigDirectory;
    private String kuraHomeDirectory;
    private String kuraMarketplaceCompatibilityVersion;
    private Integer kuraSnapshotsCount;
    private String kuraSnapshotsDirectory;
    private String kuraStyleDirectory;
    private String kuraTemporaryConfigDirectory;
    private String kuraUserConfigDirectory;
    private String kuraVersion;
    private Boolean kuraHaveWebInterface;
    private Boolean kuraHaveNetAdmin;
    private Integer kuraWifiTopChannel;
    private String kuraDefaultNetVirtualDevicesConfig;

    // osgi
    private String osgiFirmwareName;
    private String osgiFirmwareVersion;

    // command
    private String commandUser;
    private Integer commandZipMaxUploadNumber;
    private Integer commandZipMaxUploadSize;

    public FrameworkPropertiesDTO(SystemService systemService) {
        populateProperties(systemService, "[\\w]+");
    }

    public FrameworkPropertiesDTO(SystemService systemService, List<String> names) {
        for (String name : names) {
            populateProperties(systemService, name);
        }
    }

    private void populateProperties(SystemService systemService, String regex) {
        populateHardwareProperties(systemService, regex);
        populateJavaProperties(systemService, regex);
        populateOsProperties(systemService, regex);
        populateKuraProperties(systemService, regex);
        populateOsgiProperties(systemService, regex);
        populateCommandProperties(systemService, regex);
    }

    private void populateHardwareProperties(SystemService systemService, String regex) {
        if (Pattern.matches(regex, "biosVersion")) {
            this.biosVersion = systemService.getBiosVersion();
        }

        if (Pattern.matches(regex, "cpuVersion")) {
            this.cpuVersion = systemService.getCpuVersion();
        }

        if (Pattern.matches(regex, "deviceName")) {
            this.deviceName = systemService.getDeviceName();
        }

        if (Pattern.matches(regex, "modelId")) {
            this.modelId = systemService.getModelId();
        }

        if (Pattern.matches(regex, "modelName")) {
            this.modelName = systemService.getModelName();
        }

        if (Pattern.matches(regex, "partNumber")) {
            this.partNumber = systemService.getPartNumber();
        }

        if (Pattern.matches(regex, "platform")) {
            this.platform = systemService.getPlatform();
        }

        if (Pattern.matches(regex, "numberOfProcessors")) {
            this.numberOfProcessors = systemService.getNumberOfProcessors();
        }

        if (Pattern.matches(regex, "totalMemory")) {
            this.totalMemory = systemService.getTotalMemory();
        }

        if (Pattern.matches(regex, "freeMemory")) {
            this.freeMemory = systemService.getFreeMemory();
        }

        if (Pattern.matches(regex, "serialNumber")) {
            this.serialNumber = systemService.getSerialNumber();
        }
    }

    private void populateJavaProperties(SystemService systemService, String regex) {
        if (Pattern.matches(regex, "javaHome")) {
            this.javaHome = systemService.getJavaHome();
        }

        if (Pattern.matches(regex, "javaVendor")) {
            this.javaVendor = systemService.getJavaVendor();
        }

        if (Pattern.matches(regex, "javaVersion")) {
            this.javaVersion = systemService.getJavaVersion();
        }

        if (Pattern.matches(regex, "javaVmInfo")) {
            this.javaVmInfo = systemService.getJavaVmInfo();
        }

        if (Pattern.matches(regex, "javaVmName")) {
            this.javaVmName = systemService.getJavaVmName();
        }

        if (Pattern.matches(regex, "javaVmVersion")) {
            this.javaVmVersion = systemService.getJavaVmVersion();
        }
    }

    private void populateOsProperties(SystemService systemService, String regex) {
        if (Pattern.matches(regex, "osArch")) {
            this.osArch = systemService.getOsArch();
        }

        if (Pattern.matches(regex, "osDistro")) {
            this.osDistro = systemService.getOsDistro();
        }

        if (Pattern.matches(regex, "osDistroVersion")) {
            this.osDistroVersion = systemService.getOsDistroVersion();
        }

        if (Pattern.matches(regex, "osName")) {
            this.osName = systemService.getOsName();
        }

        if (Pattern.matches(regex, "osVersion")) {
            this.osVersion = systemService.getOsVersion();
        }

        if (Pattern.matches(regex, "isLegacyBluetoothBeaconScan")) {
            this.isLegacyBluetoothBeaconScan = systemService.isLegacyBluetoothBeaconScan();
        }

        if (Pattern.matches(regex, "isLegacyPPPLoggingEnabled")) {
            this.isLegacyPPPLoggingEnabled = systemService.isLegacyPPPLoggingEnabled();
        }

        if (Pattern.matches(regex, "primaryMacAddress")) {
            this.primaryMacAddress = systemService.getPrimaryMacAddress();
        }

        if (Pattern.matches(regex, "primaryNetworkInterfaceName")) {
            this.primaryNetworkInterfaceName = systemService.getPrimaryNetworkInterfaceName();
        }

        if (Pattern.matches(regex, "fileSeparator")) {
            this.fileSeparator = systemService.getFileSeparator();
        }

        if (Pattern.matches(regex, "firmwareVersion")) {
            this.firmwareVersion = systemService.getFirmwareVersion();
        }
    }

    private void populateKuraProperties(SystemService systemService, String regex) {
        if (Pattern.matches(regex, "kuraDataDirectory")) {
            this.kuraDataDirectory = systemService.getKuraDataDirectory();
        }

        if (Pattern.matches(regex, "kuraFrameworkConfigDirectory")) {
            this.kuraFrameworkConfigDirectory = systemService.getKuraFrameworkConfigDirectory();
        }

        if (Pattern.matches(regex, "kuraHomeDirectory")) {
            this.kuraHomeDirectory = systemService.getKuraHome();
        }

        if (Pattern.matches(regex, "kuraMarketplaceCompatibilityVersion")) {
            this.kuraMarketplaceCompatibilityVersion = systemService.getKuraMarketplaceCompatibilityVersion();
        }

        if (Pattern.matches(regex, "kuraSnapshotsCount")) {
            this.kuraSnapshotsCount = systemService.getKuraSnapshotsCount();
        }

        if (Pattern.matches(regex, "kuraSnapshotsDirectory")) {
            this.kuraSnapshotsDirectory = systemService.getKuraSnapshotsDirectory();
        }

        if (Pattern.matches(regex, "kuraStyleDirectory")) {
            this.kuraStyleDirectory = systemService.getKuraStyleDirectory();
        }

        if (Pattern.matches(regex, "kuraTemporaryConfigDirectory")) {
            this.kuraTemporaryConfigDirectory = systemService.getKuraTemporaryConfigDirectory();
        }

        if (Pattern.matches(regex, "kuraUserConfigDirectory")) {
            this.kuraUserConfigDirectory = systemService.getKuraUserConfigDirectory();
        }

        if (Pattern.matches(regex, "kuraVersion")) {
            this.kuraVersion = systemService.getKuraVersion();
        }

        if (Pattern.matches(regex, "kuraHaveWebInterface")) {
            this.kuraHaveWebInterface = Boolean.parseBoolean(systemService.getKuraWebEnabled());
        }

        if (Pattern.matches(regex, "kuraHaveNetAdmin")) {
            this.kuraHaveNetAdmin = (Boolean) systemService.getProperties().get(SystemService.KEY_KURA_HAVE_NET_ADMIN);
        }

        if (Pattern.matches(regex, "kuraWifiTopChannel")) {
            this.kuraWifiTopChannel = systemService.getKuraWifiTopChannel();
        }

        if (Pattern.matches(regex, "kuraDefaultNetVirtualDevicesConfig")) {
            this.kuraDefaultNetVirtualDevicesConfig = systemService.getNetVirtualDevicesConfig();
        }
    }

    private void populateOsgiProperties(SystemService systemService, String regex) {
        if (Pattern.matches(regex, "osgiFirmwareName")) {
            this.osgiFirmwareName = systemService.getOsgiFwName();
        }

        if (Pattern.matches(regex, "osgiFirmwareVersion")) {
            this.osgiFirmwareVersion = systemService.getOsgiFwVersion();
        }
    }

    private void populateCommandProperties(SystemService systemService, String regex) {
        if (Pattern.matches(regex, "commandUser")) {
            this.commandUser = systemService.getCommandUser();
        }

        if (Pattern.matches(regex, "commandZipMaxUploadNumber")) {
            this.commandZipMaxUploadNumber = systemService.getFileCommandZipMaxUploadNumber();
        }

        if (Pattern.matches(regex, "commandZipMaxUploadSize")) {
            this.commandZipMaxUploadSize = systemService.getFileCommandZipMaxUploadSize();
        }
    }

}
