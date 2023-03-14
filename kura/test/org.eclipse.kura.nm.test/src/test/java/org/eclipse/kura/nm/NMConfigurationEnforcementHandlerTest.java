package org.eclipse.kura.nm;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.networkmanager.Device;
import org.junit.Test;

public class NMConfigurationEnforcementHandlerTest {

    private NMDbusConnector mockNMConnector = mock(NMDbusConnector.class);
    private NMConfigurationEnforcementHandler configurationEnforcementHandler;

    private boolean exceptionWasThrown = false;

    @Test
    public void configurationEnforcementShouldTriggerWithConfigureSignal() throws DBusException {
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/0", "eth0");
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/1", "wlan0");
        givenNMConnectorManaging(Arrays.asList("eth0", "wlan0"));

        givenConfigurationEnforcementHandlerWith(this.mockNMConnector);

        whenConfigurationEnforcementHandlesSignalsWith("/org/freedesktop/NetworkManager/Devices/0",
                NMDeviceState.NM_DEVICE_STATE_CONFIG, NMDeviceState.NM_DEVICE_STATE_PREPARE, new UInt32(1));

        thenNoExceptionWasThrown();
        thenConfigurationEnforcementIsTriggered(true);
    }

    public void configurationEnforcementShouldTriggerWithDisconnectSignal() throws DBusException {
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/0", "eth0");
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/1", "wlan0");
        givenNMConnectorManaging(Arrays.asList("eth0", "wlan0"));

        givenConfigurationEnforcementHandlerWith(this.mockNMConnector);

        whenConfigurationEnforcementHandlesSignalsWith("/org/freedesktop/NetworkManager/Devices/0",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_PREPARE, new UInt32(1));

        thenNoExceptionWasThrown();
        thenConfigurationEnforcementIsTriggered(true);
    }

    public void configurationEnforcementShouldNotTriggerWithDisconnectSignalAfterFailure() throws DBusException {
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/0", "eth0");
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/1", "wlan0");
        givenNMConnectorManaging(Arrays.asList("eth0", "wlan0"));

        givenConfigurationEnforcementHandlerWith(this.mockNMConnector);

        whenConfigurationEnforcementHandlesSignalsWith("/org/freedesktop/NetworkManager/Devices/0",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED, new UInt32(1));

        thenNoExceptionWasThrown();
        thenConfigurationEnforcementIsTriggered(false);
    }

    public void configurationEnforcementShouldNotTriggerForUnmanagedDevice() throws DBusException {
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/0", "eth0");
        givenNMConnectorWith("/org/freedesktop/NetworkManager/Devices/1", "wlan0");
        givenNMConnectorManaging(Arrays.asList("eth0"));

        givenConfigurationEnforcementHandlerWith(this.mockNMConnector);

        whenConfigurationEnforcementHandlesSignalsWith("/org/freedesktop/NetworkManager/Devices/1",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_PREPARE, new UInt32(1));

        thenNoExceptionWasThrown();
        thenConfigurationEnforcementIsTriggered(false);
    }

    /*
     * Given
     */

    private void givenConfigurationEnforcementHandlerWith(NMDbusConnector mockConnector) {
        this.configurationEnforcementHandler = new NMConfigurationEnforcementHandler(mockConnector);
    }

    private void givenNMConnectorWith(String dbusPath, String interfaceName) throws DBusException {
        when(this.mockNMConnector.getDeviceIpInterface(dbusPath)).thenReturn(interfaceName);
    }

    private void givenNMConnectorManaging(List<String> managedDevices) {
        when(this.mockNMConnector.getManagedDevices()).thenReturn(managedDevices);
    }

    /*
     * When
     */

    private void whenConfigurationEnforcementHandlesSignalsWith(String dbusPath, NMDeviceState newState,
            NMDeviceState oldState, UInt32 reason) {
        try {
            Device.StateChanged signal = new Device.StateChanged(dbusPath, NMDeviceState.toUInt32(newState),
                    NMDeviceState.toUInt32(oldState), reason);

            this.configurationEnforcementHandler.handle(signal);

        } catch (DBusException e) {
            this.exceptionWasThrown = true;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionWasThrown() {
        assertFalse(this.exceptionWasThrown);
    }

    private void thenConfigurationEnforcementIsTriggered(boolean shouldHaveBeenTriggered) throws DBusException {
        if (shouldHaveBeenTriggered) {
            verify(this.mockNMConnector, times(1)).apply();
        } else {
            verify(this.mockNMConnector, never()).apply();
        }
    }

}
