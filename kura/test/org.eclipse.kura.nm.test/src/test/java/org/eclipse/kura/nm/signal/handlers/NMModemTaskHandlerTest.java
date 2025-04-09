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
package org.eclipse.kura.nm.signal.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.kura.nm.ModemTaskScheduler;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.eclipse.kura.nm.enums.NMDeviceStateReason;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.networkmanager.Device;
import org.junit.Test;

public class NMModemTaskHandlerTest {

    private NMModemSignalHandler modemTaskHandler = null;
    private ModemTaskScheduler modemTaskScheduler = null;
    private Device.StateChanged signal;
    private Device device;

    private static final String MOCK_DEVICE_DBUSPATH_9 = "/org/freedesktop/NetworkManager/Devices/9";
    private static final String MOCK_DEVICE_DBUSPATH_5 = "/org/freedesktop/NetworkManager/Devices/5";

    @Test
    public void shouldScheduleResetAndConnectionIfFailedDisconnectedStates() throws DBusException {
        givenModemTaskScheduler(MOCK_DEVICE_DBUSPATH_9);
        givenDeviceStateChanged(MOCK_DEVICE_DBUSPATH_9, NMDeviceState.NM_DEVICE_STATE_FAILED,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);

        whenHandleIsCalled();

        thenScheduleConnectionAndReset(1);
    }

    @Test
    public void shouldCancelSchedulerIfActivatedState() throws DBusException {
        givenModemTaskScheduler(MOCK_DEVICE_DBUSPATH_9);
        givenDeviceStateChanged(MOCK_DEVICE_DBUSPATH_9, NMDeviceState.NM_DEVICE_STATE_FAILED,
                NMDeviceState.NM_DEVICE_STATE_ACTIVATED);

        whenHandleIsCalled();

        thenScheduleIsCancelled(1);
    }

    @Test
    public void shouldDoNothingIfWrongDevicePath() throws DBusException {
        givenModemTaskScheduler(MOCK_DEVICE_DBUSPATH_9);
        givenDeviceStateChanged(MOCK_DEVICE_DBUSPATH_5, NMDeviceState.NM_DEVICE_STATE_FAILED,
                NMDeviceState.NM_DEVICE_STATE_ACTIVATED);

        whenHandleIsCalled();

        thenScheduleConnectionAndReset(0);
    }

    /*
     * Given
     */

    private void givenModemTaskScheduler(String devicePath) {
        this.modemTaskScheduler = mock(ModemTaskScheduler.class);
        this.modemTaskHandler = new NMModemSignalHandler(this.modemTaskScheduler);

        this.device = mock(Device.class);
        when(this.modemTaskScheduler.getDevice()).thenReturn(this.device);
        when(this.device.getObjectPath()).thenReturn(devicePath);
    }

    private void givenDeviceStateChanged(String devicePath, NMDeviceState oldState, NMDeviceState newState)
            throws DBusException {
        this.signal = new Device.StateChanged(devicePath, NMDeviceState.toUInt32(newState),
                NMDeviceState.toUInt32(oldState), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NONE.toUInt32());
    }

    /*
     * When
     */

    private void whenHandleIsCalled() {
        this.modemTaskHandler.handle(this.signal);
    }

    /*
     * Then
     */

    private void thenScheduleConnectionAndReset(int times) {
        verify(this.modemTaskScheduler, times(times)).scheduleConnection();
        verify(this.modemTaskScheduler, times(times)).scheduleReset();
    }

    private void thenScheduleIsCancelled(int times) {
        verify(this.modemTaskScheduler, times(times)).cancel();
    }
}
