/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.cloud;

import java.util.List;

import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.message.KuraBirthPayload;
import org.eclipse.kura.message.KuraBirthPayload.KuraBirthPayloadBuilder;
import org.eclipse.kura.message.KuraDeviceProfile;
import org.eclipse.kura.message.KuraDisconnectPayload;
import org.eclipse.kura.message.KuraPosition;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to build lifecycle payload messages.
 */
public class LifeCyclePayloadBuilder {

    private static final String ERROR = "ERROR";

    private static final Logger logger = LoggerFactory.getLogger(LifeCyclePayloadBuilder.class);

    private static final String UNKNOWN = "UNKNOWN";

    private final CloudServiceImpl cloudServiceImpl;

    LifeCyclePayloadBuilder(CloudServiceImpl cloudServiceImpl) {
        this.cloudServiceImpl = cloudServiceImpl;
    }

    public KuraBirthPayload buildBirthPayload() {
        // build device profile
        KuraDeviceProfile deviceProfile = buildDeviceProfile();

        // build application IDs
        String appIds = buildApplicationIDs();

        // build accept encoding
        String acceptEncoding = buildAcceptEncoding();

        // build device name
        CloudServiceOptions cso = this.cloudServiceImpl.getCloudServiceOptions();
        String deviceName = cso.getDeviceDisplayName();
        if (deviceName == null) {
            deviceName = this.cloudServiceImpl.getSystemService().getDeviceName();
        }

        String payloadEncoding = this.cloudServiceImpl.getCloudServiceOptions().getPayloadEncoding().name();

        // build birth certificate
        KuraBirthPayloadBuilder birthPayloadBuilder = new KuraBirthPayloadBuilder();
        birthPayloadBuilder.withUptime(deviceProfile.getUptime()).withDisplayName(deviceName)
                .withModelName(deviceProfile.getModelName()).withModelId(deviceProfile.getModelId())
                .withPartNumber(deviceProfile.getPartNumber()).withSerialNumber(deviceProfile.getSerialNumber())
                .withFirmwareVersion(deviceProfile.getFirmwareVersion()).withBiosVersion(deviceProfile.getBiosVersion())
                .withOs(deviceProfile.getOs()).withOsVersion(deviceProfile.getOsVersion())
                .withJvmName(deviceProfile.getJvmName()).withJvmVersion(deviceProfile.getJvmVersion())
                .withJvmProfile(deviceProfile.getJvmProfile()).withKuraVersion(deviceProfile.getApplicationFrameworkVersion())
                .withConnectionInterface(deviceProfile.getConnectionInterface())
                .withConnectionIp(deviceProfile.getConnectionIp()).withAcceptEncoding(acceptEncoding)
                .withApplicationIdentifiers(appIds).withAvailableProcessors(deviceProfile.getAvailableProcessors())
                .withTotalMemory(deviceProfile.getTotalMemory()).withOsArch(deviceProfile.getOsArch())
                .withOsgiFramework(deviceProfile.getOsgiFramework())
                .withOsgiFrameworkVersion(deviceProfile.getOsgiFrameworkVersion()).withPayloadEncoding(payloadEncoding);

        if (this.cloudServiceImpl.imei != null && this.cloudServiceImpl.imei.length() > 0
                && !this.cloudServiceImpl.imei.equals(ERROR)) {
            birthPayloadBuilder.withModemImei(this.cloudServiceImpl.imei);
        }
        if (this.cloudServiceImpl.iccid != null && this.cloudServiceImpl.iccid.length() > 0
                && !this.cloudServiceImpl.iccid.equals(ERROR)) {
            birthPayloadBuilder.withModemIccid(this.cloudServiceImpl.iccid);
        }

        if (this.cloudServiceImpl.imsi != null && this.cloudServiceImpl.imsi.length() > 0
                && !this.cloudServiceImpl.imsi.equals(ERROR)) {
            birthPayloadBuilder.withModemImsi(this.cloudServiceImpl.imsi);
        }

        if (this.cloudServiceImpl.rssi != null && this.cloudServiceImpl.rssi.length() > 0) {
            birthPayloadBuilder.withModemRssi(this.cloudServiceImpl.rssi);
        }

        if (deviceProfile.getLatitude() != null && deviceProfile.getLongitude() != null) {
            KuraPosition kuraPosition = new KuraPosition();
            kuraPosition.setLatitude(deviceProfile.getLatitude());
            kuraPosition.setLongitude(deviceProfile.getLongitude());
            kuraPosition.setAltitude(deviceProfile.getAltitude());
            birthPayloadBuilder.withPosition(kuraPosition);
        }

        return birthPayloadBuilder.build();
    }

    public KuraDisconnectPayload buildDisconnectPayload() {
        SystemService systemService = this.cloudServiceImpl.getSystemService();
        SystemAdminService sysAdminService = this.cloudServiceImpl.getSystemAdminService();
        CloudServiceOptions cloudOptions = this.cloudServiceImpl.getCloudServiceOptions();

        // build device name
        String deviceName = cloudOptions.getDeviceDisplayName();
        if (deviceName == null) {
            deviceName = systemService.getDeviceName();
        }

        return new KuraDisconnectPayload(sysAdminService.getUptime(), deviceName);
    }

    public KuraDeviceProfile buildDeviceProfile() {
        SystemService systemService = this.cloudServiceImpl.getSystemService();
        SystemAdminService sysAdminService = this.cloudServiceImpl.getSystemAdminService();
        NetworkService networkService = this.cloudServiceImpl.getNetworkService();
        PositionService positionService = this.cloudServiceImpl.getPositionService();

        //
        // get the network information
        StringBuilder sbConnectionIp = null;
        StringBuilder sbConnectionInterface = null;
        try {
            List<NetInterface<? extends NetInterfaceAddress>> nis = networkService.getActiveNetworkInterfaces();
            if (!nis.isEmpty()) {
                sbConnectionIp = new StringBuilder();
                sbConnectionInterface = new StringBuilder();

                for (NetInterface<? extends NetInterfaceAddress> ni : nis) {
                    List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
                    if (nias != null && !nias.isEmpty()) {
                        sbConnectionInterface.append(buildConnectionInterface(ni)).append(",");
                        sbConnectionIp.append(buildConnectionIp(ni)).append(",");
                    }
                }

                // Remove trailing comma
                sbConnectionIp.deleteCharAt(sbConnectionIp.length() - 1);
                sbConnectionInterface.deleteCharAt(sbConnectionInterface.length() - 1);
            }
        } catch (Exception se) {
            logger.warn("Error while getting ConnetionIP and ConnectionInterface", se);
        }

        String connectionIp = sbConnectionIp != null ? sbConnectionIp.toString() : UNKNOWN;
        String connectionInterface = sbConnectionInterface != null ? sbConnectionInterface.toString() : UNKNOWN;

        //
        // get the position information
        double latitude = 0.0;
        double longitude = 0.0;
        double altitude = 0.0;
        if (positionService != null) {
            NmeaPosition position = positionService.getNmeaPosition();
            if (position != null) {
                latitude = position.getLatitude();
                longitude = position.getLongitude();
                altitude = position.getAltitude();
            } else {
                logger.warn("Unresolved PositionService reference.");
            }
        }

        return buildKuraDeviceProfile(systemService, sysAdminService, connectionIp,
                connectionInterface, latitude, longitude, altitude);
    }

    private KuraDeviceProfile buildKuraDeviceProfile(SystemService systemService, SystemAdminService sysAdminService,
            String connectionIp, String connectionInterface, double latitude, double longitude, double altitude) {
        KuraDeviceProfile kuraDeviceProfile = new KuraDeviceProfile();
        kuraDeviceProfile.setUptime(sysAdminService.getUptime());
        kuraDeviceProfile.setDisplayName(systemService.getDeviceName());
        kuraDeviceProfile.setModelName(systemService.getModelName());
        kuraDeviceProfile.setModelId(systemService.getModelId());
        kuraDeviceProfile.setPartNumber(systemService.getPartNumber());
        kuraDeviceProfile.setSerialNumber(systemService.getSerialNumber());
        kuraDeviceProfile.setFirmwareVersion(systemService.getFirmwareVersion());
        kuraDeviceProfile.setBiosVersion(systemService.getBiosVersion());
        kuraDeviceProfile.setOs(systemService.getOsName());
        kuraDeviceProfile.setOsVersion(systemService.getOsVersion());
        kuraDeviceProfile.setJvmName(systemService.getJavaVmName());
        kuraDeviceProfile.setJvmVersion(systemService.getJavaVmVersion() + " " + systemService.getJavaVmInfo());
        kuraDeviceProfile.setJvmProfile(systemService.getJavaVendor() + " " + systemService.getJavaVersion());
        kuraDeviceProfile.setApplicationFramework(KuraDeviceProfile.DEFAULT_APPLICATION_FRAMEWORK);
        kuraDeviceProfile.setApplicationFrameworkVersion(systemService.getKuraVersion());
        kuraDeviceProfile.setConnectionInterface(connectionInterface);
        kuraDeviceProfile.setConnectionIp(connectionIp);
        kuraDeviceProfile.setLatitude(latitude);
        kuraDeviceProfile.setLongitude(longitude);
        kuraDeviceProfile.setAltitude(altitude);
        kuraDeviceProfile.setAvailableProcessors(String.valueOf(systemService.getNumberOfProcessors()));
        kuraDeviceProfile.setTotalMemory(String.valueOf(systemService.getTotalMemory()));
        kuraDeviceProfile.setOsArch(systemService.getOsArch());
        kuraDeviceProfile.setOsgiFramework(systemService.getOsgiFwName());
        kuraDeviceProfile.setOsgiFrameworkVersion(systemService.getOsgiFwVersion());
        return kuraDeviceProfile;
    }

    private String buildConnectionIp(NetInterface<? extends NetInterfaceAddress> ni) {
        String connectionIp = UNKNOWN;
        List<? extends NetInterfaceAddress> nias = ni.getNetInterfaceAddresses();
        if (nias != null && !nias.isEmpty() && nias.get(0).getAddress() != null) {
            connectionIp = nias.get(0).getAddress().getHostAddress();
        }
        return connectionIp;
    }

    private String buildConnectionInterface(NetInterface<? extends NetInterfaceAddress> ni) {
        StringBuilder sb = new StringBuilder();
        sb.append(ni.getName()).append(" (").append(NetUtil.hardwareAddressToString(ni.getHardwareAddress()))
                .append(")");
        return sb.toString();
    }

    private String buildApplicationIDs() {
        String[] appIdArray = this.cloudServiceImpl.getCloudApplicationIdentifiers();
        StringBuilder sbAppIDs = new StringBuilder();
        for (int i = 0; i < appIdArray.length; i++) {
            if (i != 0) {
                sbAppIDs.append(",");
            }
            sbAppIDs.append(appIdArray[i]);
        }
        return sbAppIDs.toString();
    }

    private String buildAcceptEncoding() {
        String acceptEncoding = "";
        CloudServiceOptions options = this.cloudServiceImpl.getCloudServiceOptions();
        if (options.getEncodeGzip()) {
            acceptEncoding = "gzip";
        }
        return acceptEncoding;
    }
}
