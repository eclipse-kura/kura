/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.admin.modem.ModemPppConfigGenerator;
import org.eclipse.kura.net.admin.modem.PppPeer;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo.UsbModemFactoryInfo;
import org.eclipse.kura.net.admin.util.LinuxFileUtil;
import org.eclipse.kura.net.admin.visitor.linux.util.ModemXchangeScript;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PppConfigWriter implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(PppConfigWriter.class);
    private static final String WRITING = "Writing {}";

    public static final String OS_PPP_DIRECTORY = "/etc/ppp/";
    public static final String OS_PEERS_DIRECTORY = OS_PPP_DIRECTORY + "peers/";
    public static final String OS_PPP_LOG_DIRECTORY = "/var/log/";
    public static final String OS_SCRIPTS_DIRECTORY = OS_PPP_DIRECTORY + "scripts/";
    public static final String DNS_DELIM = ",";

    public PppConfigWriter() {
        createSystemFolders();
    }

    protected void createSystemFolders() {
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
    public void setExecutorService(CommandExecutorService executorService) {
        // Not needed
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> updatedNetInterfaceConfigs = config
                .getModifiedNetInterfaceConfigs();
        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : updatedNetInterfaceConfigs) {
            if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
                writeConfig((ModemInterfaceConfigImpl) netInterfaceConfig);
            }
        }
    }

    private void writeConfig(ModemInterfaceConfigImpl modemInterfaceConfig) throws KuraException {
        String interfaceName = "ppp" + modemInterfaceConfig.getPppNum();
        String modemInterfaceName = modemInterfaceConfig.getName();

        if (!((AbstractNetInterface<?>) modemInterfaceConfig).isInterfaceEnabled()) {
            logger.info("Network interface status for {} ({}) is {} - not overwriting ppp configuration file",
                    interfaceName, modemInterfaceConfig.getName(),
                    ((AbstractNetInterface<?>) modemInterfaceConfig).getInterfaceStatus());
            return;
        }

        ModemConfig modemConfig = getModemConfig(modemInterfaceConfig);

        // Save the status and priority
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

        try {
            removeSymbolicLink(formPeerLinkAbsoluteName(modemInterfaceName));
        } catch (IOException e) {
            throw new KuraIOException(e, "Failed to remove symbolic links");
        }

        if (configClass != null) {
            writePppConfigFiles(modemConfig, modemInterfaceName, configClass, usbDevice, baudRate);
        }
    }

    private void writePppConfigFiles(ModemConfig modemConfig, String modemInterfaceName,
            Class<? extends ModemPppConfigGenerator> configClass, UsbDevice usbDevice, int baudRate) {
        try {
            String pppPeerFilename = formPeerFilename(usbDevice);
            String pppLogfile = formPppLogFilename(usbDevice);
            String chatFilename = formChatFilename(usbDevice);
            String disconnectFilename = formDisconnectFilename(usbDevice);
            String chapAuthSecretsFilename = formChapAuthSecretsFilename();
            String papAuthSecretsFilename = formPapAuthSecretsFilename();
            ModemPppConfigGenerator scriptGenerator = configClass.newInstance();

            if (modemConfig != null) {
                logger.debug("Writing connect scripts for ppp{} ({}) using {}", modemConfig.getPppNumber(),
                        modemInterfaceName, configClass);

                logger.debug(WRITING, pppPeerFilename);
                PppPeer pppPeer = scriptGenerator.getPppPeer(getDeviceId(usbDevice), modemConfig, pppLogfile,
                        chatFilename, disconnectFilename);
                pppPeer.setBaudRate(baudRate);
                pppPeer.write(pppPeerFilename, chapAuthSecretsFilename, papAuthSecretsFilename);

                String symlinkFilename = formPeerLinkAbsoluteName(modemInterfaceName);
                if (!Files.isSymbolicLink(Paths.get(symlinkFilename))) {
                    logger.debug("Linking peer file for interface {}", modemInterfaceName);
                    LinuxFileUtil.createSymbolicLink(pppPeerFilename, symlinkFilename);
                }

                logger.debug(WRITING, chatFilename);
                ModemXchangeScript connectScript = scriptGenerator.getConnectScript(modemConfig);
                connectScript.writeScript(chatFilename);

                logger.debug(WRITING, disconnectFilename);
                ModemXchangeScript disconnectScript = scriptGenerator.getDisconnectScript(modemConfig);
                disconnectScript.writeScript(disconnectFilename);
            } else {
                logger.error("Error writing connect scripts - modemConfig is null");
            }
        } catch (Exception e) {
            logger.error("Could not write modem config", e);
        }
    }

    private ModemConfig getModemConfig(ModemInterfaceConfigImpl modemInterfaceConfig) {
        // Get the config
        ModemConfig modemConfig = null;
        List<NetConfig> netConfigs = modemInterfaceConfig.getNetConfigs();
        Optional<NetConfig> netConfig = netConfigs.stream().filter(ModemConfig.class::isInstance).findFirst();
        if (netConfig.isPresent()) {
            modemConfig = (ModemConfig) netConfig.get();
        }
        return modemConfig;
    }

    public String formPeerFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_PEERS_DIRECTORY);
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public String formPppLogFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_PPP_LOG_DIRECTORY);
        buf.append("kura-");
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public String formChatFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_SCRIPTS_DIRECTORY);
        buf.append("chat");
        buf.append('_');
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public String formPeerLinkAbsoluteName(String interfaceName) {
        StringBuilder peerLink = new StringBuilder();
        peerLink.append(OS_PEERS_DIRECTORY);
        peerLink.append(interfaceName);
        return peerLink.toString();
    }

    public String formDisconnectFilename(UsbDevice usbDevice) {
        StringBuilder buf = new StringBuilder();
        buf.append(OS_SCRIPTS_DIRECTORY);
        buf.append("disconnect");
        buf.append('_');
        buf.append(formBaseFilename(usbDevice));
        return buf.toString();
    }

    public String formChapAuthSecretsFilename() {
        return OS_PPP_DIRECTORY + "chap-secrets";
    }

    public String formPapAuthSecretsFilename() {
        return OS_PPP_DIRECTORY + "pap-secrets";
    }

    private String formBaseFilename(UsbDevice usbDevice) {
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

    private String getDeviceId(UsbDevice usbDevice) {
        StringBuilder sb = new StringBuilder();
        if (usbDevice != null) {
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbDevice);
            if (modemInfo != null) {
                sb.append(modemInfo.getDeviceName());
            }
        }

        return sb.toString();
    }

    private void removeSymbolicLink(String symlinkFilename) throws IOException {
        Path path = Paths.get(symlinkFilename);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }
}
