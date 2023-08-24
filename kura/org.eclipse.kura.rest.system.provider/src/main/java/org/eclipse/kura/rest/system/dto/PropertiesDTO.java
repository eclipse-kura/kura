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
import java.util.Optional;

import org.eclipse.kura.system.ExtendedProperties;
import org.eclipse.kura.system.SystemService;

@SuppressWarnings("unused")
public class PropertiesDTO {

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

    // extended properties
    private ExtendedPropertiesDTO extendedProperties;

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
        this.kuraVersion = systemService.getKuraVersion();
        this.kuraHaveWebInterface = new Boolean(systemService.getKuraWebEnabled());
        this.kuraHaveNetAdmin = (Boolean) systemService.getProperties().get(SystemService.KEY_KURA_HAVE_NET_ADMIN);
        this.kuraWifiTopChannel = systemService.getKuraWifiTopChannel();
        this.kuraDefaultNetVirtualDevicesConfig = systemService.getNetVirtualDevicesConfig();

        this.osgiFirmwareName = systemService.getOsgiFwName();
        this.osgiFirmwareVersion = systemService.getOsgiFwVersion();

        this.commandUser = systemService.getCommandUser();
        this.commandZipMaxUploadNumber = systemService.getFileCommandZipMaxUploadNumber();
        this.commandZipMaxUploadSize = systemService.getFileCommandZipMaxUploadSize();
        
        Optional<ExtendedProperties> properties = systemService.getExtendedProperties();
        if (properties.isPresent()) {
            this.extendedProperties = new ExtendedPropertiesDTO(properties.get());
        }
    }

    public PropertiesDTO(SystemService systemService, List<String> names) {
        for (String name : names) {
            populatePropertyBasedOnName(systemService, name);
        }
    }

    private void populatePropertyBasedOnName(SystemService systemService, String name) {
        populateHardwarePropertiesBasedOnName(systemService, name);
        populateJavaPropertiesBasedOnName(systemService, name);
        populateOsPropertiesBasedOnName(systemService, name);
        populateKuraPropertiesBasedOnName(systemService, name);
        populateOsgiPropertiesBasedOnName(systemService, name);
        populateCommandPropertiesBasedOnName(systemService, name);

        if (name.equals("extendedProperties")) {
            Optional<ExtendedProperties> properties = systemService.getExtendedProperties();
            if (properties.isPresent()) {
                this.extendedProperties = new ExtendedPropertiesDTO(properties.get());
            }
        }
    }

    private void populateHardwarePropertiesBasedOnName(SystemService systemService, String name) {
        if (name.equals("biosVersion")) {
            this.biosVersion = systemService.getBiosVersion();
        }

        if (name.equals("cpuVersion")) {
            this.cpuVersion = systemService.getCpuVersion();
        }

        if (name.equals("deviceName")) {
            this.deviceName = systemService.getDeviceName();
        }

        if (name.equals("modelId")) {
            this.modelId = systemService.getModelId();
        }

        if (name.equals("modelName")) {
            this.modelName = systemService.getModelName();
        }

        if (name.equals("partNumber")) {
            this.partNumber = systemService.getPartNumber();
        }

        if (name.equals("platform")) {
            this.platform = systemService.getPlatform();
        }

        if (name.equals("numberOfProcessors")) {
            this.numberOfProcessors = systemService.getNumberOfProcessors();
        }

        if (name.equals("totalMemory")) {
            this.totalMemory = systemService.getTotalMemory();
        }

        if (name.equals("freeMemory")) {
            this.freeMemory = systemService.getFreeMemory();
        }

        if (name.equals("serialNumber")) {
            this.serialNumber = systemService.getSerialNumber();
        }
    }

    private void populateJavaPropertiesBasedOnName(SystemService systemService, String name) {
        if (name.equals("javaHome")) {
            this.javaHome = systemService.getJavaHome();
        }

        if (name.equals("javaVendor")) {
            this.javaVendor = systemService.getJavaVendor();
        }

        if (name.equals("javaVersion")) {
            this.javaVersion = systemService.getJavaVersion();
        }

        if (name.equals("javaVmInfo")) {
            this.javaVmInfo = systemService.getJavaVmInfo();
        }

        if (name.equals("javaVmName")) {
            this.javaVmName = systemService.getJavaVmName();
        }

        if (name.equals("javaVmVersion")) {
            this.javaVmVersion = systemService.getJavaVmVersion();
        }
    }

    private void populateOsPropertiesBasedOnName(SystemService systemService, String name) {
        if (name.equals("osArch")) {
            this.osArch = systemService.getOsArch();
        }

        if (name.equals("osDistro")) {
            this.osDistro = systemService.getOsDistro();
        }

        if (name.equals("osDistroVersion")) {
            this.osDistroVersion = systemService.getOsDistroVersion();
        }

        if (name.equals("osName")) {
            this.osName = systemService.getOsName();
        }

        if (name.equals("osVersion")) {
            this.osVersion = systemService.getOsVersion();
        }

        if (name.equals("isLegacyBluetoothBeaconScan")) {
            this.isLegacyBluetoothBeaconScan = systemService.isLegacyBluetoothBeaconScan();
        }

        if (name.equals("isLegacyPPPLoggingEnabled")) {
            this.isLegacyPPPLoggingEnabled = systemService.isLegacyPPPLoggingEnabled();
        }

        if (name.equals("primaryMacAddress")) {
            this.primaryMacAddress = systemService.getPrimaryMacAddress();
        }

        if (name.equals("primaryNetworkInterfaceName")) {
            this.primaryNetworkInterfaceName = systemService.getPrimaryNetworkInterfaceName();
        }

        if (name.equals("fileSeparator")) {
            this.fileSeparator = systemService.getFileSeparator();
        }

        if (name.equals("firmwareVersion")) {
            this.firmwareVersion = systemService.getFirmwareVersion();
        }
    }

    private void populateKuraPropertiesBasedOnName(SystemService systemService, String name) {
        if (name.equals("kuraDataDirectory")) {
            this.kuraDataDirectory = systemService.getKuraDataDirectory();
        }

        if (name.equals("kuraFrameworkConfigDirectory")) {
            this.kuraFrameworkConfigDirectory = systemService.getKuraFrameworkConfigDirectory();
        }

        if (name.equals("kuraHomeDirectory")) {
            this.kuraHomeDirectory = systemService.getKuraHome();
        }

        if (name.equals("kuraMarketplaceCompatibilityVersion")) {
            this.kuraMarketplaceCompatibilityVersion = systemService.getKuraMarketplaceCompatibilityVersion();
        }

        if (name.equals("kuraSnapshotsCount")) {
            this.kuraSnapshotsCount = systemService.getKuraSnapshotsCount();
        }

        if (name.equals("kuraSnapshotsDirectory")) {
            this.kuraSnapshotsDirectory = systemService.getKuraSnapshotsDirectory();
        }

        if (name.equals("kuraStyleDirectory")) {
            this.kuraStyleDirectory = systemService.getKuraStyleDirectory();
        }

        if (name.equals("kuraTemporaryConfigDirectory")) {
            this.kuraTemporaryConfigDirectory = systemService.getKuraTemporaryConfigDirectory();
        }

        if (name.equals("kuraUserConfigDirectory")) {
            this.kuraUserConfigDirectory = systemService.getKuraUserConfigDirectory();
        }

        if (name.equals("kuraVersion")) {
            this.kuraVersion = systemService.getKuraVersion();
        }

        if (name.equals("kuraHaveWebInterface")) {
            this.kuraHaveWebInterface = new Boolean(systemService.getKuraWebEnabled());
        }

        if (name.equals("kuraHaveNetAdmin")) {
            this.kuraHaveNetAdmin = (Boolean) systemService.getProperties().get(SystemService.KEY_KURA_HAVE_NET_ADMIN);
        }

        if (name.equals("kuraWifiTopChannel")) {
            this.kuraWifiTopChannel = systemService.getKuraWifiTopChannel();
        }

        if (name.equals("kuraDefaultNetVirtualDevicesConfig")) {
            this.kuraDefaultNetVirtualDevicesConfig = systemService.getNetVirtualDevicesConfig();
        }
    }

    private void populateOsgiPropertiesBasedOnName(SystemService systemService, String name) {
        if (name.equals("osgiFirmwareName")) {
            this.osgiFirmwareName = systemService.getOsgiFwName();
        }

        if (name.equals("osgiFirmwareVersion")) {
            this.osgiFirmwareVersion = systemService.getOsgiFwVersion();
        }
    }

    private void populateCommandPropertiesBasedOnName(SystemService systemService, String name) {
        if (name.equals("commandUser")) {
            this.commandUser = systemService.getCommandUser();
        }

        if (name.equals("commandZipMaxUploadNumber")) {
            this.commandZipMaxUploadNumber = systemService.getFileCommandZipMaxUploadNumber();
        }

        if (name.equals("commandZipMaxUploadSize")) {
            this.commandZipMaxUploadSize = systemService.getFileCommandZipMaxUploadSize();
        }
    }

}
