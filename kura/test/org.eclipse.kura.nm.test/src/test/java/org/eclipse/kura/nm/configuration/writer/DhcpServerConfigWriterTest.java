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
package org.eclipse.kura.nm.configuration.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.nm.configuration.NetworkProperties;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DhcpServerConfigWriterTest {

    MockedStatic<DhcpServerManager> dhcpServerMock;
    private DhcpServerConfigWriter writer;
    private String interfaceName;
    private String dhcpServerConfigFileName;
    private boolean isUnknownHostException = false;
    private boolean isKuraException = false;

    @Test
    public void shouldReturnDhcpConfigFileName() {
        givenDhcpConfigWriterWithEmptyProperties();
        whenDhcpServerFileNameRetrived();
        thenDhcpServerFileNameIsCorrect();
    }

    @Test
    public void shouldWriteDhcpConfigurationFileTest() throws UnknownHostException, KuraException {
        givenDhcpConfigWriterWithDefaultProperties();
        whenWriteDhcpConfigFile();
        thenDhcpConfigFileExists();
    }

    @Test
    public void shouldWriteCorrectDhcpdConfigurationFileTest() throws KuraException, IOException {
        givenDhcpConfigWriterWithDefaultProperties();
        givenDhcpdTool();
        whenWriteDhcpConfigFile();
        thenDhcpdConfigFileIsCorrect();
    }

    @Test
    public void shouldWriteCorrectUdhcpdConfigurationFileTest() throws KuraException, IOException {
        givenDhcpConfigWriterWithDefaultProperties();
        givenUdhcpdTool();
        whenWriteDhcpConfigFile();
        thenUdhcpdConfigFileIsCorrect();
    }

    @Test
    public void shouldThrowUnknownHostExceptionWithoutAddressTest() throws KuraException {
        givenDhcpConfigWriterWithoutAddress();
        whenWriteDhcpConfigFile();
        thenUnknownHostExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutDefaultLeaseTimeTest() throws KuraException {
        givenDhcpConfigWriterWithoutDefaultLeaseTime();
        whenWriteDhcpConfigFile();
        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutMaxLeaseTimeTest() throws KuraException {
        givenDhcpConfigWriterWithoutMaxLeaseTime();
        whenWriteDhcpConfigFile();
        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutPrefixTest() throws KuraException {
        givenDhcpConfigWriterWithoutPrefix();
        whenWriteDhcpConfigFile();
        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutRangeStartTest() throws KuraException {
        givenDhcpConfigWriterWithoutRangeStart();
        whenWriteDhcpConfigFile();
        thenUnknownHostExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutRangeStopTest() throws KuraException {
        givenDhcpConfigWriterWithoutRangeStop();
        whenWriteDhcpConfigFile();
        thenUnknownHostExceptionIsCaught();
    }

    private void givenDhcpConfigWriterWithEmptyProperties() {
        this.interfaceName = "eth0";
        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(new HashMap<String, Object>())) {
            protected String getConfigFilename() {
                return "/etc/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpdTool() {
        this.dhcpServerMock = Mockito.mockStatic(DhcpServerManager.class);
        dhcpServerMock.when(DhcpServerManager::getTool).thenReturn(DhcpServerTool.DHCPD);
    }

    private void givenUdhcpdTool() {
        this.dhcpServerMock = Mockito.mockStatic(DhcpServerManager.class);
        dhcpServerMock.when(DhcpServerManager::getTool).thenReturn(DhcpServerTool.UDHCPD);
    }

    private void givenDhcpConfigWriterWithDefaultProperties() {
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.ip4.address", "192.168.0.11");
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpConfigWriterWithoutAddress() {
        this.isUnknownHostException = false;
        this.isKuraException = false;
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpConfigWriterWithoutDefaultLeaseTime() {
        this.isUnknownHostException = false;
        this.isKuraException = false;
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.ip4.address", "192.168.0.11");
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpConfigWriterWithoutMaxLeaseTime() {
        this.isUnknownHostException = false;
        this.isKuraException = false;
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.ip4.address", "192.168.0.11");
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpConfigWriterWithoutPrefix() {
        this.isUnknownHostException = false;
        this.isKuraException = false;
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.ip4.address", "192.168.0.11");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpConfigWriterWithoutRangeStart() {
        this.isUnknownHostException = false;
        this.isKuraException = false;
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.ip4.address", "192.168.0.11");
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        properties.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void givenDhcpConfigWriterWithoutRangeStop() {
        this.isUnknownHostException = false;
        this.isKuraException = false;
        this.interfaceName = "eth0";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        properties.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        properties.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        properties.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        properties.put("net.interface.eth0.config.ip4.address", "192.168.0.11");
        properties.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        properties.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(properties)) {
            protected String getConfigFilename() {
                return "/tmp/dhcpd-eth0.conf";
            }
        };
    }

    private void whenWriteDhcpConfigFile() throws KuraException {
        try {
            this.writer.writeConfiguration();
        } catch (UnknownHostException e) {
            this.isUnknownHostException = true;
        } catch (KuraException e) {
            this.isKuraException = true;
        }
    }

    private void whenDhcpServerFileNameRetrived() {
        this.dhcpServerConfigFileName = this.writer.getConfigFilename();
    }

    private void thenDhcpServerFileNameIsCorrect() {
        assertEquals("/etc/dhcpd-eth0.conf", this.dhcpServerConfigFileName);
    }

    private void thenDhcpConfigFileExists() {
        File dhcpConfigFile = new File("/tmp/dhcpd-eth0.conf");
        assertTrue(dhcpConfigFile.exists());
    }

    private void thenDhcpdConfigFileIsCorrect() throws IOException {
        Path dhcpConfigFilePath = Path.of("/tmp/dhcpd-eth0.conf");
        String content = Files.readString(dhcpConfigFilePath);
        String expectedContent = "# enabled? true\n"
                + "# prefix: 24\n"
                + "# pass DNS? true\n"
                + "\n"
                + "subnet 192.168.0.0 netmask 255.255.255.0 {\n"
                + "    option domain-name-servers 192.168.0.11;\n"
                + "\n"
                + "    interface eth0;\n"
                + "    option routers 192.168.0.11;\n"
                + "    default-lease-time 900;\n"
                + "    max-lease-time 1000;\n"
                + "    pool {\n"
                + "        range 192.168.0.111 192.168.0.120;\n"
                + "    }\n"
                + "}\n";
        assertEquals(expectedContent, content);
    }

    private void thenUdhcpdConfigFileIsCorrect() throws IOException {
        Path dhcpConfigFilePath = Path.of("/tmp/dhcpd-eth0.conf");
        String content = Files.readString(dhcpConfigFilePath);
        String expectedContent = "start 192.168.0.111\n"
                + "end 192.168.0.120\n"
                + "interface eth0\n"
                + "pidfile null\n"
                + "max_leases 9\n"
                + "auto_time 0\n"
                + "decline_time 900\n"
                + "conflict_time 900\n"
                + "offer_time 900\n"
                + "min_lease 900\n"
                + "opt subnet 255.255.255.0\n"
                + "opt router 192.168.0.11\n"
                + "opt lease 900\n"
                + "opt dns 192.168.0.11\n";
        assertEquals(expectedContent, content);
    }

    private void thenUnknownHostExceptionIsCaught() {
        assertTrue(this.isUnknownHostException);
    }

    private void thenKuraExceptionIsCaught() {
        assertTrue(this.isKuraException);
    }

    @After
    public void clean() {
        File dhcpConfigFile = new File("/tmp/dhcpd-eth0.conf");
        if (dhcpConfigFile.exists()) {
            dhcpConfigFile.delete();
        }
        if (this.dhcpServerMock != null) {
            this.dhcpServerMock.close();
        }
    }

}
