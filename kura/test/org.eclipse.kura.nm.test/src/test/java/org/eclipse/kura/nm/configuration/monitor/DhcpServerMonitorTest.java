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
package org.eclipse.kura.nm.configuration.monitor;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.junit.Test;

public class DhcpServerMonitorTest {

    private DhcpServerMonitor monitor;
    private CommandExecutorService cesMock;
    private DhcpServerManager managerMock;
    private final Object lock = new Object();

    @Test
    public void shouldStartDhcpServerTest() throws KuraException, InterruptedException {
        givenDhcpdServerMonitorDisabled();
        whenEnabledInterfaceIsAdded();
        whenDhcpServerMonitorIsStarted();
        thenDhcpServerIsStarted();
    }

    @Test
    public void shouldStopDhcpServerTest() throws KuraException, InterruptedException {
        givenDhcpdServerMonitorEnabled();
        whenDisabledInterfaceIsAdded();
        whenDhcpServerMonitorIsStarted();
        thenDhcpServerIsStopped();
    }

    private void givenDhcpdServerMonitorDisabled() throws KuraException {
        givenDhcpdServerMonitor();
        isDisabled();
    }

    private void givenDhcpdServerMonitorEnabled() throws KuraException {
        givenDhcpdServerMonitor();
        isEnabled();
    }

    private void givenDhcpdServerMonitor() throws KuraException {
        this.cesMock = mock(CommandExecutorService.class);
        this.managerMock = mock(DhcpServerManager.class);
        this.monitor = new DhcpServerMonitor(this.cesMock);
        this.monitor.setDhcpServerManager(this.managerMock);
        this.monitor.clear();
    }

    private void isDisabled() throws KuraException {
        when(this.managerMock.isRunning("wlan0")).thenReturn(false);
        when(this.managerMock.enable("wlan0")).thenReturn(true);
    }

    private void isEnabled() throws KuraException {
        when(this.managerMock.isRunning("eth0")).thenReturn(true);
        when(this.managerMock.disable("eth0")).thenReturn(true);
    }

    private void whenEnabledInterfaceIsAdded() {
        this.monitor.putDhcpServerInterfaceConfiguration("wlan0", true);
    }

    private void whenDisabledInterfaceIsAdded() {
        this.monitor.putDhcpServerInterfaceConfiguration("eth0", false);
    }

    private void whenDhcpServerMonitorIsStarted() {
        this.monitor.start();
    }

    private void thenDhcpServerIsStarted() throws KuraException, InterruptedException {
        waitForSeconds(1);
        verify(this.managerMock, atLeast(1)).isRunning("wlan0");
        verify(this.managerMock).enable("wlan0");
    }

    private void thenDhcpServerIsStopped() throws KuraException, InterruptedException {
        waitForSeconds(1);
        verify(this.managerMock, atLeast(1)).isRunning("eth0");
        verify(this.managerMock).disable("eth0");
    }

    private void waitForSeconds(int timeout) throws InterruptedException {
        synchronized (this.lock) {
            this.lock.wait(timeout * 1000);
        }
    }
}
