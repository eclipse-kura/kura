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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.nm.enums.MMModemState;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.Test;

public class ModemTaskSchedulerTest {

    private ModemTaskScheduler modemTaskScheduler;
    private NetworkManagerDbusWrapper networkManager;
    private ModemManagerDbusWrapper modemManager;
    private Connection connection;
    private Device device;
    private NetworkProperties properties;
    private Modem modem;

    @Test
    public void shouldNotScheduleConnectionIfNoAutoconnect() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemTaskScheduler("1-4", false, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsNotScheduled();
    }

    @Test
    public void shouldScheduleConnectionIfAutoconnect() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemTaskScheduler("1-4", true, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsScheduled();
        thenConnectionIsActivated(1);
    }

    @Test
    public void shouldCancelScheduleConnectionIfModemConnected() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemDbusPath("/mypath/4");
        givenModemState("/mypath/4", MMModemState.MM_MODEM_STATE_CONNECTED);
        givenModemTaskScheduler("1-4", true, 3, 15, 3);

        whenScheduleConnection(10);

        thenConnectionIsActivated(1);
        thenConnectionIsNotScheduled();
    }

    @Test
    public void shouldScheduleConnectionIfModemNotConnected() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemTaskScheduler("1-4", true, 3, 15, 3);
        givenModemDbusPath("/mypath/4");
        givenModemState("/mypath/4", MMModemState.MM_MODEM_STATE_FAILED);

        whenScheduleConnection(10);

        thenConnectionIsActivated(1);
        thenConnectionIsScheduled();
    }

    @Test
    public void shouldScheduleMultipleConnection() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemTaskScheduler("1-4", true, 3, 1, 0);
        givenModemDbusPath("/mypath/4");
        givenModemState("/mypath/4", MMModemState.MM_MODEM_STATE_FAILED);

        whenScheduleConnection(10);

        thenConnectionIsActivated(3);
        thenConnectionIsScheduled();
    }

    @Test
    public void shouldNotScheduleResetIfTimeoutIsZero() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemTaskScheduler("1-4", false, 3, 15, 0);

        whenScheduleReset(10);

        thenResetIsNotScheduled();
    }

    @Test
    public void shouldScheduleResetIfTimeoutIsNotZero() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemTaskScheduler("1-4", false, 3, 15, 1);

        whenScheduleReset(10);

        thenResetIsScheduled();
    }

    @Test
    public void shouldCancelConnectionIfReset() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemDbusPath("/mypath/4");
        givenModemState("/mypath/4", MMModemState.MM_MODEM_STATE_FAILED);
        givenModemTaskScheduler("1-4", true, 3, 15, 1);

        whenScheduleReset(70);

        thenResetIsNotScheduled();
        thenConnectionIsNotScheduled();
    }

    @Test
    public void shouldResetIfResetIsScheduled() throws DBusException {
        givenNetworkManagerMock();
        givenModemManagerMock();
        givenModemDbusPath("/mypath/4");
        givenModemState("/mypath/4", MMModemState.MM_MODEM_STATE_FAILED);
        givenModemTaskScheduler("1-4", true, 3, 15, 1);

        whenScheduleReset(70);

        thenResetIsNotScheduled();
        thenModemIsReset();
    }

    /**
     * Given
     */

    private void givenNetworkManagerMock() {
        this.networkManager = mock(NetworkManagerDbusWrapper.class);
    }

    private void givenModemManagerMock() throws DBusException {
        this.modemManager = mock(ModemManagerDbusWrapper.class);
        this.modem = mock(Modem.class);
        when(this.modemManager.getModem(any())).thenReturn(this.modem);
    }

    private void givenModemTaskScheduler(String deviceId, boolean autoconnect, int maxFail, int holdoff,
            int resetTimeout) {
        this.connection = mock(Connection.class);
        this.device = mock(Device.class);
        Map<String, Object> rawProperties = new HashMap<>();
        rawProperties.put("net.interface." + deviceId + ".config.persist", autoconnect);
        rawProperties.put("net.interface." + deviceId + ".config.maxFail", maxFail);
        rawProperties.put("net.interface." + deviceId + ".config.holdoff", holdoff);
        rawProperties.put("net.interface." + deviceId + ".config.resetTimeout", resetTimeout);
        this.properties = new NetworkProperties(rawProperties);
        this.modemTaskScheduler = new ModemTaskScheduler(this.networkManager, this.modemManager, this.connection,
                this.device, deviceId, this.properties);
    }

    private void givenModemDbusPath(String path) throws DBusException {
        when(this.networkManager.getModemManagerDbusPath(any())).thenReturn(Optional.of(path));
    }

    private void givenModemState(String path, MMModemState state) throws DBusException {
        when(this.modemManager.getMMModemState(path)).thenReturn(state);
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

    private void thenConnectionIsActivated(int times) throws DBusException {
        verify(this.networkManager, atLeast(times)).activateConnection(this.connection, this.device);
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
