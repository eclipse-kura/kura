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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.nm.NetworkProperties;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DhcpServerConfigWriterTest {

    private static final String DHCP_CONFIG_FILENAME = "dhcpd-%s.conf";
    private static final String DNSMASQ_CONFIG_FILENAME = "dnsmasq-%s.conf";

    @Rule
    public TemporaryFolder mockFiles = new TemporaryFolder();

    private static MockedStatic<DhcpServerManager> dhcpServerMock;
    private Map<String, Object> networkProperties = new HashMap<>();
    private DhcpServerTool selectedTool;
    private String configFilename;
    private DhcpServerConfigWriter writer;
    private boolean isUnknownHostException = false;
    private boolean isKuraException = false;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnDhcpConfigFileName() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenDhcpConfigWriter("eth0");

        whenGetConfigFilename();

        thenDhcpServerFileNameIs(String.format(DHCP_CONFIG_FILENAME, "eth0"));
    }

    @Test
    public void shouldReturnUdhcpConfigFileName() throws Exception {
        givenDhcpTool(DhcpServerTool.UDHCPD);
        givenDhcpConfigWriter("eth0");

        whenGetConfigFilename();

        thenDhcpServerFileNameIs(String.format(DHCP_CONFIG_FILENAME, "eth0"));
    }

    @Test
    public void shouldReturnDnsmasqConfigFileName() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenDhcpConfigWriter("eth0");

        whenGetConfigFilename();

        thenDhcpServerFileNameIs(String.format(DNSMASQ_CONFIG_FILENAME, "eth0"));
    }

    @Test
    public void shouldReturnEmptyConfigFileName() throws Exception {
        givenDhcpTool(DhcpServerTool.NONE);
        givenDhcpConfigWriter("eth0");

        whenGetConfigFilename();

        thenDhcpServerFileNameIs("etc");
    }

    @Test
    public void shouldWriteCorrectDhcpConfigurationFile() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");
        
        whenWriteConfiguration();

        thenConfigFileContains(new StringBuilder()
                .append("# enabled? true\n")
                .append("# prefix: 24\n")
                .append("# pass DNS? true\n")
                .append("\n")
                .append("subnet 192.168.0.0 netmask 255.255.255.0 {\n")
                .append("    option domain-name-servers 192.168.0.11;\n")
                .append("\n")
                .append("    interface eth0;\n")
                .append("    option routers 192.168.0.11;\n")
                .append("    default-lease-time 900;\n")
                .append("    max-lease-time 1000;\n")
                .append("    pool {\n")
                .append("        range 192.168.0.111 192.168.0.120;\n")
                .append("    }\n")
                .append("}\n")
                .toString());
    }
    
    @Test
    public void shouldWriteCorrectUDhcpConfigurationFile() throws Exception {
        givenDhcpTool(DhcpServerTool.UDHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");
        
        whenWriteConfiguration();

        thenConfigFileContains(new StringBuilder()
                .append("start 192.168.0.111\n")
                .append("end 192.168.0.120\n")
                .append("interface eth0\n")
                .append("pidfile null\n")
                .append("max_leases 9\n")
                .append("auto_time 0\n")
                .append("decline_time 900\n")
                .append("conflict_time 900\n")
                .append("offer_time 900\n")
                .append("min_lease 900\n")
                .append("opt subnet 255.255.255.0\n")
                .append("opt router 192.168.0.11\n")
                .append("opt lease 900\n")
                .append("opt dns 192.168.0.11\n")
                .toString());
    }

    @Test
    public void shouldWriteCorrectDnsmasqConfigurationFile() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");
        
        whenWriteConfiguration();

        thenConfigFileContains(new StringBuilder()
                .append("interface=eth0\n")
                .append("dhcp-range=eth0,192.168.0.111,192.168.0.120,900s\n")
                .append("dhcp-option=eth0,1,255.255.255.0\n")
                .append("dhcp-option=eth0,3,192.168.0.11\n")
                .append("dhcp-option=eth0,6,0.0.0.0\n")
                .append("dhcp-option=eth0,27,1\n")              
                .toString());
    }
    
    @Test
    public void shouldWriteCorrectDnsmasqConfigurationFileWithoutPassDNS() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", false);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");
        
        whenWriteConfiguration();

        thenConfigFileContains(new StringBuilder()
                .append("interface=eth0\n")
                .append("dhcp-range=eth0,192.168.0.111,192.168.0.120,900s\n")
                .append("dhcp-option=eth0,1,255.255.255.0\n")
                .append("dhcp-option=eth0,3,192.168.0.11\n")
                .append("dhcp-option=eth0,6\n")
                .append("dhcp-ignore-names=eth0\n")
                .append("dhcp-option=eth0,27,1\n")
                .toString());
    }

    @Test
    public void shouldThrowUnknownHostExceptionWithoutAddressTest() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenUnknownHostExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutDefaultLeaseTimeTest() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutMaxLeaseTimeTest() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutPrefixTest() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutRangeStartTest() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenUnknownHostExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionWithoutRangeStopTest() throws Exception {
        givenDhcpTool(DhcpServerTool.DHCPD);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenUnknownHostExceptionIsCaught();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenDhcpTool(DhcpServerTool tool) {
        dhcpServerMock = Mockito.mockStatic(DhcpServerManager.class);

        switch (tool) {
        case DHCPD:
            dhcpServerMock.when(DhcpServerManager::getTool).thenReturn(DhcpServerTool.DHCPD);
            break;
        case UDHCPD:
            dhcpServerMock.when(DhcpServerManager::getTool).thenReturn(DhcpServerTool.UDHCPD);
            break;
        case DNSMASQ:
            dhcpServerMock.when(DhcpServerManager::getTool).thenReturn(DhcpServerTool.DNSMASQ);
            break;
        case NONE:
            dhcpServerMock.when(DhcpServerManager::getTool).thenReturn(DhcpServerTool.NONE);
            break;
        }

        this.selectedTool = tool;
    }

    private void givenNetworkPropertiesWith(String key, Object value) {
        this.networkProperties.put(key, value);
    }

    private void givenDhcpConfigWriter(String interfaceName) throws IOException {
        String filename;

        switch (DhcpServerConfigWriterTest.this.selectedTool) {
        case DHCPD:
        case UDHCPD:
            filename = String.format(DHCP_CONFIG_FILENAME, interfaceName);
            break;
        case DNSMASQ:
            filename = String.format(DNSMASQ_CONFIG_FILENAME, interfaceName);
            break;
        case NONE:
        default:
            filename = "etc";
        }

        this.configFilename = this.mockFiles.newFile(filename).getAbsolutePath();

        this.writer = new DhcpServerConfigWriter(interfaceName, new NetworkProperties(this.networkProperties)) {

            @Override
            protected String getConfigFilename() {
                return DhcpServerConfigWriterTest.this.configFilename;
            }
        };
    }

    /*
     * When
     */

    private void whenGetConfigFilename() {
        this.configFilename = this.writer.getConfigFilename();
    }

    private void whenWriteConfiguration() {
        try {
            this.writer.writeConfiguration();
        } catch (UnknownHostException e) {
            this.isUnknownHostException = true;
        } catch (KuraException e) {
            this.isKuraException = true;
        }
    }

    /*
     * Then
     */

    private void thenDhcpServerFileNameIs(String expectedFilename) {
        assertEquals(this.mockFiles.getRoot() + "/" + expectedFilename, this.configFilename);
    }

    private void thenConfigFileContains(String expectedContent) throws IOException {
        File dhcpConfigFile = new File(this.configFilename);
        assertTrue(dhcpConfigFile.exists());

        String content = FileUtils.readFileToString(dhcpConfigFile, "UTF-8");
        assertEquals(expectedContent, content);
    }

    private void thenUnknownHostExceptionIsCaught() {
        assertTrue(this.isUnknownHostException);
    }

    private void thenKuraExceptionIsCaught() {
        assertTrue(this.isKuraException);
    }

    /*
     * Utilities
     */

    @After
    public void cleanUp() throws IOException {
        dhcpServerMock.close();
    }

}
