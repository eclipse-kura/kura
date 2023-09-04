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
import java.util.function.Predicate;

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
    private String javaVmVendor;
    private String jdkVendorVersion;

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
        populateProperties(systemService, s -> true);
    }

    public FrameworkPropertiesDTO(SystemService systemService, List<String> names) {
        for (String name : names) {
            populateProperties(systemService, names::contains);
        }
    }

    private void populateProperties(SystemService systemService, Predicate<String> filter) {
        populateHardwareProperties(systemService, filter);
        populateJavaProperties(systemService, filter);
        populateOsProperties(systemService, filter);
        populateKuraProperties(systemService, filter);
        populateOsgiProperties(systemService, filter);
        populateCommandProperties(systemService, filter);
    }

    private void populateHardwareProperties(SystemService systemService, Predicate<String> filter) {
        if (filter.test("biosVersion")) {
            this.biosVersion = systemService.getBiosVersion();
        }

        if (filter.test("cpuVersion")) {
            this.cpuVersion = systemService.getCpuVersion();
        }

        if (filter.test("deviceName")) {
            this.deviceName = systemService.getDeviceName();
        }

        if (filter.test("modelId")) {
            this.modelId = systemService.getModelId();
        }

        if (filter.test("modelName")) {
            this.modelName = systemService.getModelName();
        }

        if (filter.test("partNumber")) {
            this.partNumber = systemService.getPartNumber();
        }

        if (filter.test("platform")) {
            this.platform = systemService.getPlatform();
        }

        if (filter.test("numberOfProcessors")) {
            this.numberOfProcessors = systemService.getNumberOfProcessors();
        }

        if (filter.test("totalMemory")) {
            this.totalMemory = systemService.getTotalMemory();
        }

        if (filter.test("freeMemory")) {
            this.freeMemory = systemService.getFreeMemory();
        }

        if (filter.test("serialNumber")) {
            this.serialNumber = systemService.getSerialNumber();
        }
    }

    private void populateJavaProperties(SystemService systemService, Predicate<String> filter) {
        if (filter.test("javaHome")) {
            this.javaHome = systemService.getJavaHome();
        }

        if (filter.test("javaVendor")) {
            this.javaVendor = systemService.getJavaVendor();
        }

        if (filter.test("javaVersion")) {
            this.javaVersion = systemService.getJavaVersion();
        }

        if (filter.test("javaVmInfo")) {
            this.javaVmInfo = systemService.getJavaVmInfo();
        }

        if (filter.test("javaVmName")) {
            this.javaVmName = systemService.getJavaVmName();
        }

        if (filter.test("javaVmVersion")) {
            this.javaVmVersion = systemService.getJavaVmVersion();
        }

        if (filter.test("javaVmVendor")) {
            this.javaVmVendor = systemService.getJavaVmVendor();
        }

        if (filter.test("jdkVendorVersion")) {
            this.jdkVendorVersion = systemService.getJdkVendorVersion();
        }
    }

    private void populateOsProperties(SystemService systemService, Predicate<String> filter) {
        if (filter.test("osArch")) {
            this.osArch = systemService.getOsArch();
        }

        if (filter.test("osDistro")) {
            this.osDistro = systemService.getOsDistro();
        }

        if (filter.test("osDistroVersion")) {
            this.osDistroVersion = systemService.getOsDistroVersion();
        }

        if (filter.test("osName")) {
            this.osName = systemService.getOsName();
        }

        if (filter.test("osVersion")) {
            this.osVersion = systemService.getOsVersion();
        }

        if (filter.test("isLegacyBluetoothBeaconScan")) {
            this.isLegacyBluetoothBeaconScan = systemService.isLegacyBluetoothBeaconScan();
        }

        if (filter.test("isLegacyPPPLoggingEnabled")) {
            this.isLegacyPPPLoggingEnabled = systemService.isLegacyPPPLoggingEnabled();
        }

        if (filter.test("primaryMacAddress")) {
            this.primaryMacAddress = systemService.getPrimaryMacAddress();
        }

        if (filter.test("primaryNetworkInterfaceName")) {
            this.primaryNetworkInterfaceName = systemService.getPrimaryNetworkInterfaceName();
        }

        if (filter.test("fileSeparator")) {
            this.fileSeparator = systemService.getFileSeparator();
        }

        if (filter.test("firmwareVersion")) {
            this.firmwareVersion = systemService.getFirmwareVersion();
        }
    }

    private void populateKuraProperties(SystemService systemService, Predicate<String> filter) {
        if (filter.test("kuraDataDirectory")) {
            this.kuraDataDirectory = systemService.getKuraDataDirectory();
        }

        if (filter.test("kuraFrameworkConfigDirectory")) {
            this.kuraFrameworkConfigDirectory = systemService.getKuraFrameworkConfigDirectory();
        }

        if (filter.test("kuraHomeDirectory")) {
            this.kuraHomeDirectory = systemService.getKuraHome();
        }

        if (filter.test("kuraMarketplaceCompatibilityVersion")) {
            this.kuraMarketplaceCompatibilityVersion = systemService.getKuraMarketplaceCompatibilityVersion();
        }

        if (filter.test("kuraSnapshotsCount")) {
            this.kuraSnapshotsCount = systemService.getKuraSnapshotsCount();
        }

        if (filter.test("kuraSnapshotsDirectory")) {
            this.kuraSnapshotsDirectory = systemService.getKuraSnapshotsDirectory();
        }

        if (filter.test("kuraStyleDirectory")) {
            this.kuraStyleDirectory = systemService.getKuraStyleDirectory();
        }

        if (filter.test("kuraTemporaryConfigDirectory")) {
            this.kuraTemporaryConfigDirectory = systemService.getKuraTemporaryConfigDirectory();
        }

        if (filter.test("kuraUserConfigDirectory")) {
            this.kuraUserConfigDirectory = systemService.getKuraUserConfigDirectory();
        }

        if (filter.test("kuraVersion")) {
            this.kuraVersion = systemService.getKuraVersion();
        }

        if (filter.test("kuraHaveWebInterface")) {
            this.kuraHaveWebInterface = Boolean.parseBoolean(systemService.getKuraWebEnabled());
        }

        if (filter.test("kuraHaveNetAdmin")) {
            this.kuraHaveNetAdmin = (Boolean) systemService.getProperties().get(SystemService.KEY_KURA_HAVE_NET_ADMIN);
        }

        if (filter.test("kuraWifiTopChannel")) {
            this.kuraWifiTopChannel = systemService.getKuraWifiTopChannel();
        }

        if (filter.test("kuraDefaultNetVirtualDevicesConfig")) {
            this.kuraDefaultNetVirtualDevicesConfig = systemService.getNetVirtualDevicesConfig();
        }
    }

    private void populateOsgiProperties(SystemService systemService, Predicate<String> filter) {
        if (filter.test("osgiFirmwareName")) {
            this.osgiFirmwareName = systemService.getOsgiFwName();
        }

        if (filter.test("osgiFirmwareVersion")) {
            this.osgiFirmwareVersion = systemService.getOsgiFwVersion();
        }
    }

    private void populateCommandProperties(SystemService systemService, Predicate<String> filter) {
        if (filter.test("commandUser")) {
            this.commandUser = systemService.getCommandUser();
        }

        if (filter.test("commandZipMaxUploadNumber")) {
            this.commandZipMaxUploadNumber = systemService.getFileCommandZipMaxUploadNumber();
        }

        if (filter.test("commandZipMaxUploadSize")) {
            this.commandZipMaxUploadSize = systemService.getFileCommandZipMaxUploadSize();
        }
    }

}
