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
 *******************************************************************************/
package org.eclipse.kura.nm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.junit.Test;

public class NMModemResetHandlerTest {

    private Modem mockMMModemDevice = mock(Modem.class);
    private NMModemResetHandler resetHandler = null;

    @Test
    public void modemShouldNotBeResetWhenModemDoesntDisconnect() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 50);

        whenXSecondsHavePassed(1000);

        thenModemWasReset(false);
    }

    @Test
    public void modemShouldNotBeResetWhenADifferenctModemDisconnects() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 50);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/10",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);

        whenXSecondsHavePassed(1000);

        thenModemWasReset(false);
    }

    @Test
    public void modemShouldNotBeResetWhenAModemGenerateAnotherSignal() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 50);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_IP_CONFIG, NMDeviceState.NM_DEVICE_STATE_PREPARE);

        whenXSecondsHavePassed(1000);

        thenModemWasReset(false);
    }

    @Test
    public void modemShouldBeResetWhenModemDisconnectsAndTimeoutHasPassed() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 50);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);

        whenXSecondsHavePassed(1000);

        thenModemWasReset(true);
    }

    @Test
    public void modemShouldBeResetOnlyOnceWhenModemDisconnectsMultipleTimes() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 250);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);

        whenXSecondsHavePassed(3000);

        thenModemWasReset(true);
    }

    @Test
    public void modemShouldNotBeResetWhenItReconnectsBeforeTimeoutHasPassed() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 150);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_ACTIVATED, NMDeviceState.NM_DEVICE_STATE_SECONDARIES);

        whenXSecondsHavePassed(1000);

        thenModemWasReset(false);
    }

    @Test
    public void modemShouldNotBeResetWhenItDisconnectsButHandlerGetsCancelled() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 50);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenNMModemResetHandlerGetsCancelled();

        whenXSecondsHavePassed(1000);

        thenModemWasReset(false);
    }

    /*
     * Given
     */

    private void givenNMModemResetHandlerWith(String nmDeviceDbusPath, Modem mockModem, long delayMilliseconds) {
        this.resetHandler = new NMModemResetHandler(nmDeviceDbusPath, mockModem, delayMilliseconds);
    }

    private void givenDeviceStateChangeSignal(String nmDeviceDbusPath, NMDeviceState newState, NMDeviceState oldState)
            throws DBusException {

        Device.StateChanged signal = new Device.StateChanged(nmDeviceDbusPath, NMDeviceState.toUInt32(newState),
                NMDeviceState.toUInt32(oldState), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NONE.toUInt32());

        this.resetHandler.handle(signal);
    }

    private void givenNMModemResetHandlerGetsCancelled() {
        this.resetHandler.clearTimer();
    }

    /*
     * When
     */

    private void whenXSecondsHavePassed(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace(); // WIP
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Then
     */

    private void thenModemWasReset(boolean shouldHaveBeenCalled) {
        if (shouldHaveBeenCalled) {
            verify(this.mockMMModemDevice, times(1)).Reset();
        } else {
            verify(this.mockMMModemDevice, never()).Reset();
        }
    }

}
