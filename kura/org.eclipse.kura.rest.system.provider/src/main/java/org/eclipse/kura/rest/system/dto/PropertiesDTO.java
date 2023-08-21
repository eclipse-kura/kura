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

import org.eclipse.kura.system.SystemService;

public class PropertiesDTO {

    // hardware
    private String biosVersion;
    private String cpuVersion;
    private String deviceName;
    private String modelId;
    private String modelName;
    private String partNumber;
    private String platform;
    private int numberOfProcessors;
    private long totalMemory;
    private long freeMemory;
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
    private boolean isLegacyBluetoothBeaconScan;
    private boolean isLegacyPPPLoggingEnabled;
    private String primaryMacAddress;
    private String primaryNetworkInterfaceName;
    private String fileSeparator;
    private String firmwareVersion;

    // kura
    private String kuraDataDirectory;
    private String kuraFrameworkConfigDirectory;
    private String kuraHomeDirectory;
    private String kuraMarketplaceCompatibilityVersion;
    private int kuraSnapshotsCount;
    private String kuraSnapshotsDirectory;
    private String kuraStyleDirectory;
    private String kuraTemporaryConfigDirectory;
    private String kuraUserConfigDirectory;
    private String kuraVersion;
    private String kuraWebEnabled;
    private int kuraWifiTopChannel;
    private String kuraDefaultNetVirtualDevicesConfig;

    // osgi
    private String osgiFirmwareName;
    private String osgiFirmwareVersion;

    // command
    private String commandUser;
    private int commandZipMaxUploadNumber;
    private int commandZipMaxUploadSize;

    public PropertiesDTO(SystemService systemService) {
        this.biosVersion = systemService.getBiosVersion();
        this.cpuVersion = systemService.getCpuVersion();
        this.deviceName = systemService.getDeviceName();
        this.modelId = systemService.getModelId();
        this.modelName = systemService.getModelName();
        this.partNumber = systemService.getPartNumber();
        this.platform = systemService.getPlatform();
        this.numberOfProcessors = systemService.getNumberOfProcessors();
        this.totalMemory = systemService.getTotalMemory();
        this.freeMemory = systemService.getFreeMemory();
        this.serialNumber = systemService.getSerialNumber();

        this.javaHome = systemService.getJavaHome();
        this.javaVendor = systemService.getJavaVendor();
        this.javaVersion = systemService.getJavaVersion();
        this.javaVmInfo = systemService.getJavaVmInfo();
        this.javaVmName = systemService.getJavaVmName();
        this.javaVmVersion = systemService.getJavaVmVersion();

        this.osArch = systemService.getOsArch();
        this.osDistro = systemService.getOsDistro();
        this.osDistroVersion = systemService.getOsDistroVersion();
        this.osName = systemService.getOsName();
        this.osVersion = systemService.getOsVersion();
        this.isLegacyBluetoothBeaconScan = systemService.isLegacyBluetoothBeaconScan();
        this.isLegacyPPPLoggingEnabled = systemService.isLegacyPPPLoggingEnabled();
        this.primaryMacAddress = systemService.getPrimaryMacAddress();
        this.primaryNetworkInterfaceName = systemService.getPrimaryNetworkInterfaceName();
        this.fileSeparator = systemService.getFileSeparator();
        this.firmwareVersion = systemService.getFirmwareVersion();

        this.kuraDataDirectory = systemService.getKuraDataDirectory();
        this.kuraFrameworkConfigDirectory = systemService.getKuraFrameworkConfigDirectory();
        this.kuraHomeDirectory = systemService.getKuraHome();
        this.kuraMarketplaceCompatibilityVersion = systemService.getKuraMarketplaceCompatibilityVersion();
        this.kuraSnapshotsCount = systemService.getKuraSnapshotsCount();
        this.kuraSnapshotsDirectory = systemService.getKuraSnapshotsDirectory();
        this.kuraStyleDirectory = systemService.getKuraStyleDirectory();
        this.kuraTemporaryConfigDirectory = systemService.getKuraTemporaryConfigDirectory();
        this.kuraUserConfigDirectory = systemService.getKuraUserConfigDirectory();
        this.kuraWebEnabled = systemService.getKuraWebEnabled();
        this.kuraWifiTopChannel = systemService.getKuraWifiTopChannel();
        this.kuraDefaultNetVirtualDevicesConfig = systemService.getNetVirtualDevicesConfig();

        this.osgiFirmwareName = systemService.getOsgiFwName();
        this.osgiFirmwareVersion = systemService.getOsgiFwVersion();

        this.commandUser = systemService.getCommandUser();
        this.commandZipMaxUploadNumber = systemService.getFileCommandZipMaxUploadNumber();
        this.commandZipMaxUploadSize = systemService.getFileCommandZipMaxUploadSize();
    }

    /*
     * Left out properties
     */

    // systemService.getProperties()
    // systemService.getDeviceManagementServiceIgnore()
    // systemService.getExtendedProperties()
    // systemService.getJavaKeyStorePassword()
    // systemService.getJavaTrustStorePassword()
    // systemService.getSystemPackages()

}
