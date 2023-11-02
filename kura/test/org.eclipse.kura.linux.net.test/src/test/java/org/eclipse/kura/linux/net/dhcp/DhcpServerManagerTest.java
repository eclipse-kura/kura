/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.server.DhcpdConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.DhcpdLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.UdhcpdConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.UdhcpdLeaseReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class DhcpServerManagerTest {

    private static final String EXAMPLE_INTERFACE = "eth0";

    private static final String EXPECTED_ETC_CONF_FILENAME = "/etc/%s-%s.conf";
    private static final String EXPECTED_DNSMASQ_CONF_FILENAME = "/etc/dnsmasq.d/%s-%s.conf";
    private static final String EXPECTED_PID_FILENAME = "/var/run/%s-%s.pid";
    private static final String EXPECTED_LEASES_FILENAME = "/var/lib/dhcp/%s-%s.leases";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DhcpServerManager dhcpServerManager;
    private CommandExecutorService executorMock;
    private String returnedFilename;
    private Optional<DhcpServerConfigConverter> returnedConfigConverter;
    private Optional<DhcpServerLeaseReader> returnedLeaseReader;
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnConfigFilenameForDhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DHCPD);

        whenGetConfigFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_ETC_CONF_FILENAME, DhcpServerTool.DHCPD.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnConfigFilenameForUdhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.UDHCPD);

        whenGetConfigFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_ETC_CONF_FILENAME, DhcpServerTool.UDHCPD.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnConfigFilenameForDnsmasq() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetConfigFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_DNSMASQ_CONF_FILENAME, DhcpServerTool.DNSMASQ.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnConfigFilenameForNone() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetConfigFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/etc/");
    }

    @Test
    public void shouldReturnPidFilenameForDhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DHCPD);

        whenGetPidFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_PID_FILENAME, DhcpServerTool.DHCPD.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnPidFilenameForUdhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.UDHCPD);

        whenGetPidFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_PID_FILENAME, DhcpServerTool.UDHCPD.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnPidFilenameForDnsmasq() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetPidFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/var/run/");
    }

    @Test
    public void shouldReturnPidFilenameForNone() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetPidFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/var/run/");
    }

    @Test
    public void shouldReturnLeasesFilenameForDhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DHCPD);

        whenGetLeasesFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_LEASES_FILENAME, DhcpServerTool.DHCPD.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnLeasesFilenameForUdhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.UDHCPD);

        whenGetLeasesFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_LEASES_FILENAME, DhcpServerTool.UDHCPD.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnLeasesFilenameForDnsmasq() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetLeasesFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/var/lib/dhcp/dnsmasq.leases");
    }

    @Test
    public void shouldReturnLeasesFilenameForNone() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetLeasesFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/var/lib/dhcp/");
    }

    @Test
    public void shouldReturnConfigConverterForDhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DHCPD);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsPresent();
        thenReturnedConfigConverterIs(DhcpdConfigConverter.class);
    }

    @Test
    public void shouldReturnConfigConverterForUdhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.UDHCPD);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsPresent();
        thenReturnedConfigConverterIs(UdhcpdConfigConverter.class);
    }

    @Test
    public void shouldReturnConfigConverterForDnsmasq() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsPresent();
        thenReturnedConfigConverterIs(DnsmasqConfigConverter.class);
    }

    @Test
    public void shouldReturnConfigConverterForNone() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsEmpty();
    }

    @Test
    public void shouldReturnLeaseReaderForDhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DHCPD);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsPresent();
        thenReturnedLeaseReaderIs(DhcpdLeaseReader.class);
    }

    @Test
    public void shouldReturnLeaseReaderForUdhcpd() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.UDHCPD);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsPresent();
        thenReturnedLeaseReaderIs(UdhcpdLeaseReader.class);
    }

    @Test
    public void shouldReturnLeaseReaderForDnsmasq() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsPresent();
        thenReturnedLeaseReaderIs(DnsmasqLeaseReader.class);
    }

    @Test
    public void shouldReturnLeaseReaderForNone() throws NoSuchFieldException {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsEmpty();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenDhcpServerManager(DhcpServerTool dhcpServerTool) throws NoSuchFieldException {
        this.executorMock = Mockito.mock(CommandExecutorService.class);

        TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", dhcpServerTool);

        this.dhcpServerManager = new DhcpServerManager(executorMock);
    }

    /*
     * When
     */

    private void whenGetConfigFilename(String interfaceName) {
        try {
            this.returnedFilename = DhcpServerManager.getConfigFilename(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetPidFilename(String interfaceName) {
        try {
            this.returnedFilename = DhcpServerManager.getPidFilename(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetLeasesFilename(String interfaceName) {
        try {
            this.returnedFilename = DhcpServerManager.getLeasesFilename(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetConfigConverter() {
        try {
            this.returnedConfigConverter = DhcpServerManager.getConfigConverter();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetLeaseReader() {
        try {
            this.returnedLeaseReader = DhcpServerManager.getLeaseReader();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionsOccurred() {
        assertNull(this.occurredException);
    }

    private void thenReturnedFilenameIs(String expectedFilename) {
        assertEquals(expectedFilename, this.returnedFilename);
    }

    private void thenReturnedConfigConvertedIsPresent() {
        assertTrue(this.returnedConfigConverter.isPresent());
    }

    private void thenReturnedConfigConvertedIsEmpty() {
        assertFalse(this.returnedConfigConverter.isPresent());
    }

    private void thenReturnedConfigConverterIs(Class<?> dhcpServerConfigConverter) {
        assertEquals(dhcpServerConfigConverter, this.returnedConfigConverter.get().getClass());
    }

    private void thenReturnedLeaseReaderIsPresent() {
        assertTrue(this.returnedLeaseReader.isPresent());
    }

    private void thenReturnedLeaseReaderIsEmpty() {
        assertFalse(this.returnedLeaseReader.isPresent());
    }

    private void thenReturnedLeaseReaderIs(Class<?> dhcpServerLeaseReader) {
        assertEquals(dhcpServerLeaseReader, this.returnedLeaseReader.get().getClass());
    }

}
