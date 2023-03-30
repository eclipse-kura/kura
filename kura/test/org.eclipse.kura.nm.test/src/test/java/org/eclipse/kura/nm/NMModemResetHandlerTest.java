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
    public void basicTest() throws DBusException {
        givenNMModemResetHandlerWith("/org/freedesktop/NetworkManager/Devices/9", this.mockMMModemDevice, 50);
        givenDeviceStateChangeSignal("/org/freedesktop/NetworkManager/Devices/9",
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NMDeviceState.NM_DEVICE_STATE_FAILED);

        whenXSecondsHavePassed(1000);

        thenModemWasReset(true);
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
