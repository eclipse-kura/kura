package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.junit.Test;

import fi.w1.Wpa_supplicant1;

public class WpaSupplicantDbusWrapperTest {

    private static final String WPA_SUPPLICANT_BUS_NAME = "fi.w1.wpa_supplicant1";
    private static final String WPA_SUPPLICANT_BUS_PATH = "/fi/w1/wpa_supplicant1";

    private final DBusConnection mockedDbusConnection = mock(DBusConnection.class);
    private final Wpa_supplicant1 mockedWpaSupplicant = mock(Wpa_supplicant1.class);

    private WpaSupplicantDbusWrapper wpaSupplicantDbusWrapper;
    private Object occurredException;

    @Test
    public void syncScanShouldThrowWithNonExistentInterface() throws DBusException {
        givenMockWpaSupplicant();
        givenMockWpaSupplicantWillThrowWhenGetInterfaceIsCalledWith("wlan0");

        givenWpaSupplicantDbusWrapper();

        whenSyncScanIsCalledWith("wlan0");

        thenExceptionOccurred(DBusExecutionException.class);
    }

    @Test
    public void asyncScanShouldThrowWithNonExistentInterface() throws DBusException {
        givenMockWpaSupplicant();
        givenMockWpaSupplicantWillThrowWhenGetInterfaceIsCalledWith("wlan0");

        givenWpaSupplicantDbusWrapper();

        whenAsyncScanIsCalledWith("wlan0");

        thenExceptionOccurred(DBusExecutionException.class);
    }

    /*
     * Given
     */

    private void givenMockWpaSupplicant() throws DBusException {
        when(this.mockedDbusConnection.getRemoteObject(WPA_SUPPLICANT_BUS_NAME, WPA_SUPPLICANT_BUS_PATH, Wpa_supplicant1.class))
                .thenReturn(this.mockedWpaSupplicant);
    }

    private void givenMockWpaSupplicantWillThrowWhenGetInterfaceIsCalledWith(String interfaceName) {
        when(this.mockedWpaSupplicant.GetInterface(interfaceName))
                .thenThrow(new DBusExecutionException("Interface not found"));
    }

    private void givenWpaSupplicantDbusWrapper() throws DBusException {
        this.wpaSupplicantDbusWrapper = new WpaSupplicantDbusWrapper(this.mockedDbusConnection);
    }

    /*
     * When
     */

    private void whenSyncScanIsCalledWith(String interfaceName) {
        try {
            this.wpaSupplicantDbusWrapper.syncScan(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenAsyncScanIsCalledWith(String interfaceName) {
        try {
            this.wpaSupplicantDbusWrapper.asyncScan(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */
    private void thenExceptionOccurred(Class<DBusExecutionException> expectedExceptionClass) {
        assertNotNull(this.occurredException);
        assertEquals(expectedExceptionClass, this.occurredException.getClass());
    }

}
