/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.junit.Test;

public class ModemTaskSchedulerTest {

    private ModemTaskScheduler modemTaskScheduler;
    private NMDbusConnector nmDbusConnector;
    private Device device;
    private NetworkProperties properties;
    private Modem modem;

    @Test
    public void shouldNotScheduleConnectionIfNoAutoconnect() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", false, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsNotScheduled();
        thenConnectionIsNotActivated("1-4");
    }

    @Test
    public void shouldNotScheduleConnectionIfAlreadyConnect() throws DBusException {
        givenNMDbusConnectorMock(true, true);
        givenModemTaskScheduler("1-4", false, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsNotScheduled();
        thenConnectionIsNotActivated("1-4");
    }

    @Test
    public void shouldScheduleConnectionIfAutoconnect() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", true, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsScheduled();
        thenConnectionIsActivated(1, "1-4");
    }

    @Test
    public void shouldCancelScheduleConnectionIfModemConnected() throws DBusException {
        givenNMDbusConnectorMock(false, true);
        givenModemTaskScheduler("1-4", true, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsActivated(1, "1-4");
        thenConnectionIsNotScheduled();
    }

    @Test
    public void shouldScheduleConnectionIfModemNotConnected() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", true, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsActivated(1, "1-4");
        thenConnectionIsScheduled();
    }

    @Test
    public void shouldScheduleMultipleConnection() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", true, 3, 1, 0);

        whenScheduleConnection(10);

        thenConnectionIsActivated(3, "1-4");
        thenConnectionIsScheduled();
    }

    @Test
    public void shouldNotScheduleResetIfModemIsConnected() throws DBusException {
        givenNMDbusConnectorMock(true, true);
        givenModemTaskScheduler("1-4", false, 3, 15, 15);

        whenScheduleReset(10);

        thenResetIsNotScheduled();
    }

    @Test
    public void shouldNotScheduleResetIfTimeoutIsZero() throws DBusException {
        givenNMDbusConnectorMock(true, true);
        givenModemTaskScheduler("1-4", false, 3, 15, 0);

        whenScheduleReset(10);

        thenResetIsNotScheduled();
    }

    @Test
    public void shouldScheduleResetIfTimeoutIsNotZero() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", false, 3, 15, 1);

        whenScheduleReset(1);

        thenResetIsScheduled();
    }

    @Test
    public void shouldCancelConnectionIfReset() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", true, 3, 15, 1);

        whenScheduleReset(70);

        thenResetIsNotScheduled();
        thenConnectionIsNotScheduled();
    }

    @Test
    public void shouldResetIfResetIsScheduled() throws DBusException {
        givenNMDbusConnectorMock(false, false);
        givenModemTaskScheduler("1-4", true, 3, 15, 1);

        whenScheduleReset(70);

        thenResetIsNotScheduled();
        thenModemIsReset();
    }

    /**
     * Given
     */

    private void givenNMDbusConnectorMock(boolean isConnectedFirstCall, boolean isConnectedSecondCall)
            throws DBusException {
        this.nmDbusConnector = mock(NMDbusConnector.class);
        when(this.nmDbusConnector.isConnectionActivated(any())).thenReturn(isConnectedFirstCall)
                .thenReturn(isConnectedSecondCall);
        when(this.nmDbusConnector.isModemConnected(any())).thenReturn(isConnectedFirstCall)
                .thenReturn(isConnectedSecondCall);
        this.modem = mock(Modem.class);
        when(this.nmDbusConnector.getModem(any())).thenReturn(Optional.of(this.modem));
    }

    private void givenModemTaskScheduler(String deviceId, boolean autoconnect, int maxFail, int holdoff,
            int resetTimeout) {
        this.device = mock(Device.class);
        Map<String, Object> rawProperties = new HashMap<>();
        rawProperties.put("net.interface." + deviceId + ".config.persist", autoconnect);
        rawProperties.put("net.interface." + deviceId + ".config.maxFail", maxFail);
        rawProperties.put("net.interface." + deviceId + ".config.holdoff", holdoff);
        rawProperties.put("net.interface." + deviceId + ".config.resetTimeout", resetTimeout);
        this.properties = new NetworkProperties(rawProperties);
        this.modemTaskScheduler = new ModemTaskScheduler(deviceId, this.device, this.properties);
        this.modemTaskScheduler.setNMDBusConnector(this.nmDbusConnector);
    }

    /**
     * When
     */

    private void whenScheduleConnection(int timeout) {
        this.modemTaskScheduler.scheduleConnection();
        wait(timeout);
    }

    private void whenScheduleReset(int timeout) {
        this.modemTaskScheduler.scheduleReset();
        wait(timeout);
    }

    /**
     * Then
     */

    private void thenConnectionIsNotScheduled() {
        assertFalse(this.modemTaskScheduler.isConnectionScheduled());
    }

    private void thenConnectionIsScheduled() {
        assertTrue(this.modemTaskScheduler.isConnectionScheduled());
    }

    private void thenResetIsNotScheduled() {
        assertFalse(this.modemTaskScheduler.isResetScheduled());
    }

    private void thenResetIsScheduled() {
        assertTrue(this.modemTaskScheduler.isResetScheduled());
    }

    private void thenConnectionIsActivated(int times, String deviceId) throws DBusException {
        verify(this.nmDbusConnector, atLeast(times)).apply(deviceId);
    }

    private void thenConnectionIsNotActivated(String deviceId) throws DBusException {
        verify(this.nmDbusConnector, never()).apply(deviceId);
    }

    private void thenModemIsReset() throws DBusException {
        verify(this.modem).Reset();
    }

    private void wait(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }
}
