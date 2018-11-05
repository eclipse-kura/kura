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
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo.UsbModemFactoryInfo;
import org.eclipse.kura.net.admin.util.LinuxFileUtil;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PppConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(PppConfigWriter.class);

    private static final String NET_INTERFACE_PPP = "net.interface.ppp";
    public static final String OS_PEERS_DIRECTORY = "/etc/ppp/peers/";
    public static final String OS_PPP_LOG_DIRECTORY = "/var/log/";
    public static final String OS_SCRIPTS_DIRECTORY = "/etc/ppp/scripts/";
    public static final String DNS_DELIM = ",";

    private static PppConfigWriter instance;

    public static PppConfigWriter getInstance() {
        if (instance == null) {
            instance = new PppConfigWriter();
        }

        return instance;
    }

    private PppConfigWriter() {
        File peersDir = new File(OS_PEERS_DIRECTORY);
        if (!peersDir.exists()) {
            if (peersDir.mkdirs()) {
                logger.debug("Created directory: {}", OS_PEERS_DIRECTORY);
            } else {
                logger.warn("Could not create peers directory: {}", OS_PEERS_DIRECTORY);
            }
        }

        File scriptsDir = new File(OS_SCRIPTS_DIRECTORY);
        if (!scriptsDir.exists()) {
            if (scriptsDir.mkdirs()) {
                logger.debug("Created directory: {}", OS_SCRIPTS_DIRECTORY);
            } else {
                logger.warn("Could not create scripts directory: {}", OS_SCRIPTS_DIRECTORY);
            }
        }
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> updatedNetInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();
        List<String> modemNetInterfaceNames = new ArrayList<>();
        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : updatedNetInterfaceConfigs) {
            if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
                writeConfig((ModemInterfaceConfigImpl) netInterfaceConfig);
            }
        }

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> allNetInterfaceConfigs = config
                .getNetInterfaceConfigs();
        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : allNetInterfaceConfigs) {
            if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
                modemNetInterfaceNames.add(netInterfaceConfig.getName());
            }
        }
        removeKuraExtendedCellularConfig(modemNetInterfaceNames);
    }

    private void writeConfig(ModemInterfaceConfigImpl modemInterfaceConfig) throws KuraException {
        String oldInterfaceName = modemInterfaceConfig.getName();
        String newInterfaceName = modemInterfaceConfig.getName();

        // Get the configs
        ModemConfig modemConfig = null;
        NetConfigIP4 netConfigIP4 = null;

        List<NetConfig> netConfigs = modemInterfaceConfig.getNetConfigs();
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof ModemConfig) {
                modemConfig = (ModemConfig) netConfig;
            } else if (netConfig instanceof NetConfigIP4) {
                netConfigIP4 = (NetConfigIP4) netConfig;
            }
        }

        // Use the ppp number for the interface name, if configured
        int pppNum = -1;
        if (modemConfig != null) {
            pppNum = modemConfig.getPppNumber();
            if (pppNum >= 0) {
                newInterfaceName = "ppp" + pppNum;
                modemInterfaceConfig.setName(newInterfaceName);
            }
        }

        // Save the status and priority
        IfcfgConfigWriter.writeKuraExtendedConfig(modemInterfaceConfig);

        Class<? extends ModemPppConfigGenerator> configClass = null;
        UsbDevice usbDevice = modemInterfaceConfig.getUsbDevice();
        int baudRate = -1;
        if (usbDevice != null) {
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            UsbModemFactoryInfo usbFactoryInfo = SupportedUsbModemsFactoryInfo.getModem(modemInfo);
            if (usbFactoryInfo != null) {
                configClass = usbFactoryInfo.getConfigGeneratorClass();
            }
            baudRate = 921600;
        }

        String pppPeerFilename = formPeerFilename(usbDevice);
        String pppLogfile = formPppLogFilename(usbDevice);
        String chatFilename = formChatFilename(usbDevice);
        String disconnectFilename = formDisconnectFilename(usbDevice);

        // Cleanup values associated with the old name if the interface name has changed
        if (!oldInterfaceName.equals(newInterfaceName)) {
            try {
                // Remove the old ppp peers symlink
                logger.debug("Removing old symlinks to {}", pppPeerFilename);
                removeSymbolicLinks(pppPeerFilename, OS_PEERS_DIRECTORY);

                // Remove the old modem identifier
                StringBuilder key = new StringBuilder("net.interface.").append(oldInterfaceName)
                        .append(".modem.identifier");
                logger.debug("Removing modem identifier for {}", oldInterfaceName);
                KuranetConfig.deleteProperty(key.toString());

                // Remove custom dns servers
                key = new StringBuilder("net.interface.").append(oldInterfaceName).append(".config.dnsServers");
                logger.debug("Removing dns servers for {}", oldInterfaceName);
                KuranetConfig.deleteProperty(key.toString());

                // Remove gpsEnabled
                key = new StringBuilder().append("net.interface.").append(oldInterfaceName)
                        .append(".config.gpsEnabled");
                logger.debug("Removing gpsEnabled for {}", oldInterfaceName);
                KuranetConfig.deleteProperty(key.toString());

                // Remove apn
                key = new StringBuilder().append("net.interface.").append(oldInterfaceName).append(".config.apn");
                logger.debug("Removing apn for {}", oldInterfaceName);
                KuranetConfig.deleteProperty(key.toString());

                // Remove pdpType
                key = new StringBuilder().append("net.interface.").append(oldInterfaceName).append(".config.pdpType");
                logger.debug("Removing pdpType for {}", oldInterfaceName);
                KuranetConfig.deleteProperty(key.toString());

                // Remove status
                IfcfgConfigWriter.removeKuraExtendedConfig(oldInterfaceName);
            } catch (IOException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }

        if (configClass != null) {
            try {
                ModemPppConfigGenerator scriptGenerator = configClass.newInstance();

                if (modemConfig != null) {

                    String modemIdentifier = modemInterfaceConfig.getModemIdentifier();
                    if (modemIdentifier != null) {
                        StringBuilder key = new StringBuilder("net.interface.").append(modemInterfaceConfig.getName())
                                .append(".modem.identifier");
                        logger.debug("Storing modem identifier {} using key: {}", modemIdentifier, key);
                        KuranetConfig.setProperty(key.toString(), modemIdentifier);
                    }

                    final StringBuilder gpsEnabledKey = new StringBuilder().append("net.interface.")
                            .append(newInterfaceName).append(".config.gpsEnabled");
                    logger.debug("Setting gpsEnabled for {}", newInterfaceName);
                    KuranetConfig.setProperty(gpsEnabledKey.toString(), Boolean.toString(modemConfig.isGpsEnabled()));

                    final StringBuilder apnKey = new StringBuilder().append("net.interface.").append(newInterfaceName)
                            .append(".config.apn");
                    logger.debug("Setting apn for {}", newInterfaceName);
                    KuranetConfig.setProperty(apnKey.toString(), modemConfig.getApn());

                    final StringBuilder pdpTypeKey = new StringBuilder().append("net.interface.")
                            .append(newInterfaceName).append(".config.pdpType");
                    logger.debug("Setting pdpType for {}", newInterfaceName);
                    KuranetConfig.setProperty(pdpTypeKey.toString(), modemConfig.getPdpType().name());

                    logger.debug("Writing connect scripts for {} using {}", modemInterfaceConfig.getName(),
                            configClass);

                    logger.debug("Writing {}", pppPeerFilename);
                    PppPeer pppPeer = scriptGenerator.getPppPeer(getDeviceId(usbDevice), modemConfig, pppLogfile,
                            chatFilename, disconnectFilename);
                    pppPeer.setBaudRate(baudRate);
                    pppPeer.write(pppPeerFilename);

                    if (pppNum >= 0) {
                        logger.debug("Linking peer file using ppp number: {}", pppNum);
                        String symlinkFilename = formPeerLinkAbsoluteName(pppNum);
                        LinuxFileUtil.createSymbolicLink(pppPeerFilename, symlinkFilename);
                    } else {
                        logger.error("Can't create symbolic link to {}, invalid ppp number: {}", pppPeerFilename,
                                pppNum);
                    }

                    logger.debug("Writing {}", chatFilename);
                    ModemXchangeScript connectScript = scriptGenerator.getConnectScript(modemConfig);
                    connectScript.writeScript(chatFilename);

                    logger.debug("Writing {}", disconnectFilename);
                    ModemXchangeScript disconnectScript = scriptGenerator.getDisconnectScript(modemConfig);
                    disconnectScript.writeScript(disconnectFilename);

                    // Custom dns servers
                    if (netConfigIP4 != null) {
                        StringBuilder key = new StringBuilder("net.interface.").append(modemInterfaceConfig.getName())
                                .append(".config.dnsServers");

                        List<IP4Address> dnsServers = netConfigIP4.getDnsServers();
                        if (dnsServers != null && !dnsServers.isEmpty()) {
                            StringBuilder serversSB = new StringBuilder();

                            Iterator<IP4Address> it = dnsServers.iterator();
                            serversSB.append(it.next().getHostAddress());
                            while (it.hasNext()) {
                                serversSB.append(DNS_DELIM).append(it.next().getHostAddress());
                            }

                            logger.debug("Storing DNS servers {} using key: {}", serversSB, key);
                            KuranetConfig.setProperty(key.toString(), serversSB.toString());
                        } else {
                            KuranetConfig.deleteProperty(key.toString());
                        }
                    }

                    final StringBuilder resetTimeoutKey = new StringBuilder().append("net.interface.")
                            .append(newInterfaceName).append(".config.resetTimeout");
                    logger.debug("Setting modem resetTimeout for {}", newInterfaceName);
                    KuranetConfig.setProperty(resetTimeoutKey.toString(),
                            Integer.toString(modemConfig.getResetTimeout()));
                } else {
                    logger.error("Error writing connect scripts - modemConfig is null");
                }
            } catch (Exception e) {
                logger.error("Could not write modem config", e);
            }
        }
    }

    public static String formPeerFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_PEERS_DIRECTORY);
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public static String formPppLogFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_PPP_LOG_DIRECTORY);
        buf.append("kura-");
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public static String formChatFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_SCRIPTS_DIRECTORY);
        buf.append("chat");
        buf.append('_');
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public static String formPeerLinkName(int pppUnitNo) {
        StringBuilder peerLinkName = new StringBuilder();
        peerLinkName.append("ppp");
        peerLinkName.append(pppUnitNo);

        return peerLinkName.toString();
    }

    public static String formPeerLinkAbsoluteName(int pppUnitNo) {
        StringBuilder peerLink = new StringBuilder();
        peerLink.append(OS_PEERS_DIRECTORY);
        peerLink.append(formPeerLinkName(pppUnitNo));
        return peerLink.toString();
    }

    public static String formDisconnectFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_SCRIPTS_DIRECTORY);
        buf.append("disconnect");
        buf.append('_');
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    private static String formBaseFilename(UsbDevice usbDevice) {
        StringBuilder sb = new StringBuilder();

        if (usbDevice != null) {
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            if (modemInfo != null) {
                sb.append(modemInfo.getDeviceName());
                sb.append('_');
                sb.append(usbDevice.getUsbPort());
            }
        }
        return sb.toString();
    }

    private static String getDeviceId(UsbDevice usbDevice) {
        StringBuilder sb = new StringBuilder();
        if (usbDevice != null) {
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            if (modemInfo != null) {
                sb.append(modemInfo.getDeviceName());
            }
        }

        return sb.toString();
    }

    // Delete all symbolic links to the specified target file in the specified directory
    private void removeSymbolicLinks(String target, String directory) throws IOException {
        File targetFile = new File(target);
        File dir = new File(directory);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                    // this is the target file
                    continue;
                }

                if (file.getCanonicalPath().equals(targetFile.getAbsolutePath())) {
                    logger.debug("Deleting {}", file.getAbsolutePath());
                    file.delete();
                }
            }
        }
    }

    private void removeKuraExtendedCellularConfig(List<String> modemNetInterfaceNames) throws KuraException {
        Properties props = KuranetConfig.getProperties();
        if (props.isEmpty()) {
            return;
        }

        List<String> keysToRemove = new ArrayList<>();
        Set<Object> keys = props.keySet();
        for (Object obj : keys) {
            String key = (String) obj;
            boolean matchFound = false;
            for (String modemNetInterfaceName : modemNetInterfaceNames) {
                if (key.contains(NET_INTERFACE_PPP) && key.contains(modemNetInterfaceName)) {
                    matchFound = true;
                    break;
                }
            }
            if (key.contains(NET_INTERFACE_PPP) && !matchFound) {
                keysToRemove.add(key);
            }
        }
        if (!keysToRemove.isEmpty()) {
            for (String key : keysToRemove) {
                props.remove(key);
            }
            try {
                KuranetConfig.storeProperties(props);
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }
    }
}
