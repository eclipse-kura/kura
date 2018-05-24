/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import java.util.List;

import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.KuraBirthPayload;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.KuraDeviceProfile;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.KuraDisconnectPayload;
import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.KuraBirthPayload.KuraBirthPayloadBuilder;
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

    private final CloudConnectionManagerImpl cloudConnectionManagerImpl;

    LifeCyclePayloadBuilder(CloudConnectionManagerImpl cloudConnectionManagerImpl) {
        this.cloudConnectionManagerImpl = cloudConnectionManagerImpl;
    }

    public KuraBirthPayload buildBirthPayload() {
        // build device profile
        KuraDeviceProfile deviceProfile = buildDeviceProfile();

        // build accept encoding
        String acceptEncoding = buildAcceptEncoding();

        // build device name
        CloudConnectionManagerOptions cso = this.cloudConnectionManagerImpl.getCloudServiceOptions();
        String deviceName = cso.getDeviceDisplayName();
        if (deviceName == null) {
            deviceName = this.cloudConnectionManagerImpl.getSystemService().getDeviceName();
        }

        String payloadEncoding = this.cloudConnectionManagerImpl.getCloudServiceOptions().getPayloadEncoding().name();

        // build birth certificate
        KuraBirthPayloadBuilder birthPayloadBuilder = new KuraBirthPayloadBuilder();
        birthPayloadBuilder.withUptime(deviceProfile.getUptime()).withDisplayName(deviceName)
                .withModelName(deviceProfile.getModelName()).withModelId(deviceProfile.getModelId())
                .withPartNumber(deviceProfile.getPartNumber()).withSerialNumber(deviceProfile.getSerialNumber())
                .withFirmwareVersion(deviceProfile.getFirmwareVersion()).withBiosVersion(deviceProfile.getBiosVersion())
                .withOs(deviceProfile.getOs()).withOsVersion(deviceProfile.getOsVersion())
                .withJvmName(deviceProfile.getJvmName()).withJvmVersion(deviceProfile.getJvmVersion())
                .withJvmProfile(deviceProfile.getJvmProfile()).withKuraVersion(deviceProfile.getKuraVersion())
                .withConnectionInterface(deviceProfile.getConnectionInterface())
                .withConnectionIp(deviceProfile.getConnectionIp()).withAcceptEncoding(acceptEncoding)
                .withAvailableProcessors(deviceProfile.getAvailableProcessors())
                .withTotalMemory(deviceProfile.getTotalMemory()).withOsArch(deviceProfile.getOsArch())
                .withOsgiFramework(deviceProfile.getOsgiFramework())
                .withOsgiFrameworkVersion(deviceProfile.getOsgiFrameworkVersion()).withPayloadEncoding(payloadEncoding);

        if (this.cloudConnectionManagerImpl.imei != null && this.cloudConnectionManagerImpl.imei.length() > 0
                && !this.cloudConnectionManagerImpl.imei.equals(ERROR)) {
            birthPayloadBuilder.withModemImei(this.cloudConnectionManagerImpl.imei);
        }
        if (this.cloudConnectionManagerImpl.iccid != null && this.cloudConnectionManagerImpl.iccid.length() > 0
                && !this.cloudConnectionManagerImpl.iccid.equals(ERROR)) {
            birthPayloadBuilder.withModemIccid(this.cloudConnectionManagerImpl.iccid);
        }

        if (this.cloudConnectionManagerImpl.imsi != null && this.cloudConnectionManagerImpl.imsi.length() > 0
                && !this.cloudConnectionManagerImpl.imsi.equals(ERROR)) {
            birthPayloadBuilder.withModemImsi(this.cloudConnectionManagerImpl.imsi);
        }

        if (this.cloudConnectionManagerImpl.rssi != null && this.cloudConnectionManagerImpl.rssi.length() > 0) {
            birthPayloadBuilder.withModemRssi(this.cloudConnectionManagerImpl.rssi);
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
        SystemService systemService = this.cloudConnectionManagerImpl.getSystemService();
        SystemAdminService sysAdminService = this.cloudConnectionManagerImpl.getSystemAdminService();
        CloudConnectionManagerOptions cloudOptions = this.cloudConnectionManagerImpl.getCloudServiceOptions();

        // build device name
        String deviceName = cloudOptions.getDeviceDisplayName();
        if (deviceName == null) {
            deviceName = systemService.getDeviceName();
        }

        return new KuraDisconnectPayload(sysAdminService.getUptime(), deviceName);
    }

    public KuraDeviceProfile buildDeviceProfile() {
        SystemService systemService = this.cloudConnectionManagerImpl.getSystemService();
        SystemAdminService sysAdminService = this.cloudConnectionManagerImpl.getSystemAdminService();
        NetworkService networkService = this.cloudConnectionManagerImpl.getNetworkService();
        PositionService positionService = this.cloudConnectionManagerImpl.getPositionService();

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

        //
        // build the profile
        return new KuraDeviceProfile(sysAdminService.getUptime(), systemService.getDeviceName(),
                systemService.getModelName(), systemService.getModelId(), systemService.getPartNumber(),
                systemService.getSerialNumber(), systemService.getFirmwareVersion(), systemService.getBiosVersion(),
                systemService.getOsName(), systemService.getOsVersion(), systemService.getJavaVmName(),
                systemService.getJavaVmVersion() + " " + systemService.getJavaVmInfo(),
                systemService.getJavaVendor() + " " + systemService.getJavaVersion(), systemService.getKuraVersion(),
                connectionInterface, connectionIp, latitude, longitude, altitude,
                String.valueOf(systemService.getNumberOfProcessors()), String.valueOf(systemService.getTotalMemory()),
                systemService.getOsArch(), systemService.getOsgiFwName(), systemService.getOsgiFwVersion());
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

    private String buildAcceptEncoding() {
        String acceptEncoding = "";
        CloudConnectionManagerOptions options = this.cloudConnectionManagerImpl.getCloudServiceOptions();
        if (options.getEncodeGzip()) {
            acceptEncoding = "gzip";
        }
        return acceptEncoding;
    }
}
