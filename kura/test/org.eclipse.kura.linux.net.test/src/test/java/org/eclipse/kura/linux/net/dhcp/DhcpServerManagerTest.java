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
import static org.junit.Assert.assertNull;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class DhcpServerManagerTest {

    private static final String EXAMPLE_INTERFACE = "eth0";

    private static final String EXPECTED_ETC_CONF_FILENAME = "/etc/%s-%s.conf";
    private static final String EXPECTED_DNSMASQ_CONF_FILENAME = "/etc/dnsmasq.d/%s-%s.conf";
    private static final String EXPECTED_PID_FILENAME = "/var/run/%s-%s.pid";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DhcpServerManager dhcpServerManager;
    private CommandExecutorService executorMock;
    private String returnedFilename;
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

    /*
     * Then
     */

    private void thenNoExceptionsOccurred() {
        assertNull(this.occurredException);
    }

    private void thenReturnedFilenameIs(String expectedFilename) {
        assertEquals(expectedFilename, this.returnedFilename);
    }

}
