/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin.visitor.linux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemConfig.PdpType;
import org.eclipse.kura.net.modem.ModemInterfaceAddressConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.junit.Test;

public class PppConfigTest {

    private final String PPP_DIR = "/tmp/kura/ppp/";
    private final String PPP_PEERS_DIR = "peers/";
    private final String PPP_SCRIPTS_DIR = "scripts/";
    private final String PPP2 = "ppp2";
    private final String MODEM_MODEL = "IK41VE";
    private final String USB_BUS_NUMBER = "1";
    private final String USB_DEVICE_PATH = "1.3";
    private PppConfigWriter writer;
    private NetworkConfiguration config;
    private final UsbDevice usbDevice = new UsbModemDevice("1bbb", "00b6", "TELEFONICA", this.MODEM_MODEL,
            this.USB_BUS_NUMBER, this.USB_DEVICE_PATH);

    @Test
    public void shouldWritePeersFile() throws KuraException, IOException {
        givenFoldersClean();
        givenPppConfigWriterLogEnabled();
        givenNetworkConfigurationWithPpp(this.PPP2);

        whenPppIsVisited();

        thenPppPeersFileExists();
        thenPppPeersFileIsCorrect();
    }

    @Test
    public void shouldWriteChatScriptFile() throws KuraException, IOException {
        givenFoldersClean();
        givenPppConfigWriterLogEnabled();
        givenNetworkConfigurationWithPpp(this.PPP2);

        whenPppIsVisited();

        thenPppChatScriptFileExists();
        thenPppChatScriptFileIsCorrect();
    }

    @Test
    public void shouldWriteDisconnectScriptFile() throws KuraException, IOException {
        givenFoldersClean();
        givenPppConfigWriterLogEnabled();
        givenNetworkConfigurationWithPpp(this.PPP2);

        whenPppIsVisited();

        thenPppDisconnectScriptFileExists();
        thenPppDisconnectScriptFileIsCorrect();
    }

    @Test
    public void shouldNotWriteLogDirective() throws KuraException, IOException {
        givenFoldersClean();
        givenPppConfigWriterLogDisabled();
        givenNetworkConfigurationWithPpp(this.PPP2);

        whenPppIsVisited();

        thenPppPeersFileExists();
        thenPppPeersFileNoLoggingEnabled();
    }

    @Test
    public void shouldReturnPeerFilename() {
        givenOriginalPppConfigWriterLogEnabled();
        thenPeerFilenameIsReturned();
    }

    @Test
    public void shouldReturnPppLogFilename() {
        givenOriginalPppConfigWriterLogEnabled();
        thenPppLogFilenameIsReturned();
    }

    @Test
    public void shouldReturnChatFilename() {
        givenOriginalPppConfigWriterLogEnabled();
        thenChatFilenameIsReturned();
    }

    @Test
    public void shouldReturnPeerLinkAbsoluteName() {
        givenOriginalPppConfigWriterLogEnabled();
        thenPeerLinkAbsoluteNameIsReturned();
    }

    @Test
    public void shouldReturnDisconnectFilename() {
        givenOriginalPppConfigWriterLogEnabled();
        thenDisconnectFilenameIsReturned();
    }

    private void givenPppConfigWriterLogEnabled() {
        this.writer = new PppConfigWriter() {

            @Override
            protected boolean getPPPLogStatus() {
                return true;
            }

            @Override
            protected void createSystemFolders() {
                File peersDir = new File(PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR);
                if (!peersDir.exists()) {
                    peersDir.mkdirs();
                }

                File scriptsDir = new File(PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR);
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs();
                }
            }

            @Override
            public String formPeerFilename(UsbDevice usbDevice) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR + PppConfigTest.this.MODEM_MODEL
                        + "_" + PppConfigTest.this.USB_BUS_NUMBER + "-" + PppConfigTest.this.USB_DEVICE_PATH;
            }

            @Override
            public String formChatFilename(UsbDevice usbDevice) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR + "chat" + "_"
                        + PppConfigTest.this.MODEM_MODEL + "_" + PppConfigTest.this.USB_BUS_NUMBER + "-"
                        + PppConfigTest.this.USB_DEVICE_PATH;
            }

            @Override
            public String formDisconnectFilename(UsbDevice usbDevice) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR + "disconnect" + "_"
                        + PppConfigTest.this.MODEM_MODEL + "_" + PppConfigTest.this.USB_BUS_NUMBER + "-"
                        + PppConfigTest.this.USB_DEVICE_PATH;
            }

            @Override
            public String formPeerLinkAbsoluteName(String pppInterfaceName) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR + PppConfigTest.this.PPP2;
            }

            @Override
            public String formChapAuthSecretsFilename() {
                return PppConfigTest.this.PPP_DIR + "chap-secrets";
            }

            @Override
            public String formPapAuthSecretsFilename() {
                return PppConfigTest.this.PPP_DIR + "pap-secrets";
            }
        };
    }

    private void givenPppConfigWriterLogDisabled() {
        this.writer = new PppConfigWriter() {

            @Override
            protected boolean getPPPLogStatus() {
                return false;
            }

            @Override
            protected void createSystemFolders() {
                File peersDir = new File(PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR);
                if (!peersDir.exists()) {
                    peersDir.mkdirs();
                }

                File scriptsDir = new File(PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR);
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs();
                }
            }

            @Override
            public String formPeerFilename(UsbDevice usbDevice) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR + PppConfigTest.this.MODEM_MODEL
                        + "_" + PppConfigTest.this.USB_BUS_NUMBER + "-" + PppConfigTest.this.USB_DEVICE_PATH;
            }

            @Override
            public String formChatFilename(UsbDevice usbDevice) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR + "chat" + "_"
                        + PppConfigTest.this.MODEM_MODEL + "_" + PppConfigTest.this.USB_BUS_NUMBER + "-"
                        + PppConfigTest.this.USB_DEVICE_PATH;
            }

            @Override
            public String formDisconnectFilename(UsbDevice usbDevice) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR + "disconnect" + "_"
                        + PppConfigTest.this.MODEM_MODEL + "_" + PppConfigTest.this.USB_BUS_NUMBER + "-"
                        + PppConfigTest.this.USB_DEVICE_PATH;
            }

            @Override
            public String formPeerLinkAbsoluteName(String pppInterfaceName) {
                return PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR + PppConfigTest.this.PPP2;
            }

            @Override
            public String formChapAuthSecretsFilename() {
                return PppConfigTest.this.PPP_DIR + "chap-secrets";
            }

            @Override
            public String formPapAuthSecretsFilename() {
                return PppConfigTest.this.PPP_DIR + "pap-secrets";
            }
        };
    }

    private void givenOriginalPppConfigWriterLogEnabled() {
        this.writer = new PppConfigWriter() {

            @Override
            protected boolean getPPPLogStatus() {
                return true;
            }

            @Override
            protected void createSystemFolders() {
                File peersDir = new File(PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_PEERS_DIR);
                if (!peersDir.exists()) {
                    peersDir.mkdirs();
                }

                File scriptsDir = new File(PppConfigTest.this.PPP_DIR + PppConfigTest.this.PPP_SCRIPTS_DIR);
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs();
                }
            }
        };
    }

    private void givenFoldersClean() {
        deleteAllFilesInFolder(new File(this.PPP_DIR + this.PPP_PEERS_DIR));
        deleteAllFilesInFolder(new File(this.PPP_DIR + this.PPP_SCRIPTS_DIR));
    }

    private void deleteAllFilesInFolder(File folder) {
        for (File file : folder.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    private void givenNetworkConfigurationWithPpp(String interfaceName) throws UnknownHostException {
        this.config = new NetworkConfiguration();

        List<String> interfaces = new ArrayList<>();
        interfaces.add(interfaceName);
        this.config.setModifiedInterfaceNames(interfaces);

        ModemInterfaceConfigImpl netInterfaceConfig = new ModemInterfaceConfigImpl(interfaceName);
        netInterfaceConfig.setUsbDevice(this.usbDevice);
        this.config.addNetInterfaceConfig(netInterfaceConfig);

        List<ModemInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        ModemInterfaceAddressConfigImpl modemInterfaceAddressConfig = new ModemInterfaceAddressConfigImpl();
        List<NetConfig> netConfigs = new ArrayList<>();

        int profileId = 0;
        PdpType pdpType = PdpType.PPP;
        String apn = "web.eurotech.com";
        IP4Address address = (IP4Address) IPAddress.getByAddress(new byte[] { 0x0A, 0x0A, 0x00, (byte) 0xFA });
        int dataCompression = 0;
        int headerCompression = 0;
        ModemConfig modemConfig = new ModemConfig(profileId, pdpType, apn, address, dataCompression, headerCompression);
        modemConfig.setPppNumber(2);
        modemConfig.setDialString("atd*99***4#");
        netConfigs.add(modemConfig);

        NetConfigIP4 netConfigIP4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
        netConfigs.add(netConfigIP4);

        modemInterfaceAddressConfig.setNetConfigs(netConfigs);
        interfaceAddressConfigs.add(modemInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);
    }

    private void whenPppIsVisited() throws KuraException {
        this.writer.visit(this.config);
    }

    private void thenPppPeersFileExists() throws IOException {
        File f = new File(this.PPP_DIR + this.PPP_PEERS_DIR + this.MODEM_MODEL + "_" + this.USB_BUS_NUMBER + "-"
                + this.USB_DEVICE_PATH);
        assertTrue(f.exists());
    }

    private void thenPppPeersFileIsCorrect() throws IOException {
        String peersFileContent = readFile(this.PPP_DIR + this.PPP_PEERS_DIR + this.MODEM_MODEL + "_"
                + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH);

        List<String> expectedPeersFileContent = Arrays
                .asList(new String[] { "921600", "logfile /var/log/kura-IK41VE_1-1.3", "debug",
                        "connect 'chat -v -f /tmp/kura/ppp/scripts/chat_IK41VE_1-1.3'",
                        "disconnect 'chat -v -f /tmp/kura/ppp/scripts/disconnect_IK41VE_1-1.3'", "modem", "lock",
                        "noauth", "noipdefault", "defaultroute", "usepeerdns", "noproxyarp", "novj", "novjccomp",
                        "nobsdcomp", "nodeflate", "nomagic", "opersist", "maxfail 0", "connect-delay 1000" });

        expectedPeersFileContent.forEach(s -> assertTrue(peersFileContent.contains(s)));
    }

    private void thenPppPeersFileNoLoggingEnabled() throws IOException {
        String peersFileContent = readFile(this.PPP_DIR + this.PPP_PEERS_DIR + this.MODEM_MODEL + "_"
                + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH);

        List<String> expectedPeersFileContent = Arrays.asList(
                new String[] { "921600", "debug", "connect 'chat -v -f /tmp/kura/ppp/scripts/chat_IK41VE_1-1.3'",
                        "disconnect 'chat -v -f /tmp/kura/ppp/scripts/disconnect_IK41VE_1-1.3'", "modem", "lock",
                        "noauth", "noipdefault", "defaultroute", "usepeerdns", "noproxyarp", "novj", "novjccomp",
                        "nobsdcomp", "nodeflate", "nomagic", "opersist", "maxfail 0", "connect-delay 1000" });

        expectedPeersFileContent.forEach(s -> assertTrue(peersFileContent.contains(s)));
    }

    private void thenPppChatScriptFileExists() throws IOException {
        File f = new File(this.PPP_DIR + this.PPP_SCRIPTS_DIR + "chat_" + this.MODEM_MODEL + "_" + this.USB_BUS_NUMBER
                + "-" + this.USB_DEVICE_PATH);
        assertTrue(f.exists());
    }

    private void thenPppChatScriptFileIsCorrect() throws IOException {
        String chatFileContent = readFile(this.PPP_DIR + this.PPP_SCRIPTS_DIR + "chat_" + this.MODEM_MODEL + "_"
                + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH);

        List<String> expectedChatFileContent = Arrays.asList(
                new String[] { "ABORT\t\"BUSY\"", "ABORT\t\"VOICE\"", "ABORT\t\"NO CARRIER\"", "ABORT\t\"NO DIALTONE\"",
                        "ABORT\t\"NO DIAL TONE\"", "ABORT\t\"ERROR\"", "\"\"\t\\rAT", "TIMEOUT\t1",
                        "\"OK-+++\\c-OK\"\tATH0", "TIMEOUT\t45", "OK\tat+cgdcont=4,\"IP\",\"web.eurotech.com\"",
                        "OK\t\"\\d\\d\\d\"", "\"\"\t\"atd*99***4#\"", "CONNECT\t\"\\c\"" });

        expectedChatFileContent.forEach(s -> assertTrue(chatFileContent.contains(s.trim())));
    }

    private void thenPppDisconnectScriptFileExists() throws IOException {
        File f = new File(this.PPP_DIR + this.PPP_SCRIPTS_DIR + "disconnect_" + this.MODEM_MODEL + "_"
                + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH);
        assertTrue(f.exists());
    }

    private void thenPppDisconnectScriptFileIsCorrect() throws IOException {
        String disconnectFileContent = readFile(this.PPP_DIR + this.PPP_SCRIPTS_DIR + "disconnect_" + this.MODEM_MODEL
                + "_" + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH);

        List<String> expectedDisconnectFileContent = Arrays
                .asList(new String[] { "ABORT\t\"BUSY\"", "ABORT\t\"VOICE\"", "ABORT\t\"NO CARRIER\"",
                        "ABORT\t\"NO DIALTONE\"", "ABORT\t\"NO DIAL TONE\"", "\"\"\tBREAK", "\"\"\t\"+++ATH\"" });

        expectedDisconnectFileContent.forEach(s -> assertTrue(disconnectFileContent.contains(s)));
    }

    private void thenPeerFilenameIsReturned() {
        assertEquals("/etc/ppp/peers/" + this.MODEM_MODEL + "_" + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH,
                this.writer.formPeerFilename(this.usbDevice));
    }

    private void thenPppLogFilenameIsReturned() {
        assertEquals("/var/log/kura-" + this.MODEM_MODEL + "_" + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH,
                this.writer.formPppLogFilename(this.usbDevice));
    }

    private void thenChatFilenameIsReturned() {
        assertEquals(
                "/etc/ppp/scripts/chat_" + this.MODEM_MODEL + "_" + this.USB_BUS_NUMBER + "-" + this.USB_DEVICE_PATH,
                this.writer.formChatFilename(this.usbDevice));
    }

    private void thenPeerLinkAbsoluteNameIsReturned() {
        assertEquals("/etc/ppp/peers/" + this.PPP2, this.writer.formPeerLinkAbsoluteName("ppp2"));
    }

    private void thenDisconnectFilenameIsReturned() {
        UsbDevice usbDevice = new UsbModemDevice("1bbb", "00b6", "TELEFONICA", this.MODEM_MODEL, this.USB_BUS_NUMBER,
                this.USB_DEVICE_PATH);
        assertEquals("/etc/ppp/scripts/disconnect_" + this.MODEM_MODEL + "_" + this.USB_BUS_NUMBER + "-"
                + this.USB_DEVICE_PATH, this.writer.formDisconnectFilename(usbDevice));
    }

    private String readFile(String configFilename) throws IOException {
        Path path = Paths.get(configFilename);
        List<String> readLinesList = Files.readAllLines(path);
        StringBuilder readLines = new StringBuilder();
        readLinesList.forEach(line -> {
            readLines.append(line).append("\n");
        });

        return readLines.toString();
    }

}
