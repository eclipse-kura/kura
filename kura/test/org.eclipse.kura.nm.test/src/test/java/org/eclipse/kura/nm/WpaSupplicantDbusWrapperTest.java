package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.types.Variant;
import org.junit.Test;

import fi.w1.Wpa_supplicant1;
import fi.w1.wpa_supplicant1.Interface;

public class WpaSupplicantDbusWrapperTest {

    private static final String WPA_SUPPLICANT_BUS_NAME = "fi.w1.wpa_supplicant1";
    private static final String WPA_SUPPLICANT_BUS_PATH = "/fi/w1/wpa_supplicant1";

    private final DBusConnection mockedDbusConnection = mock(DBusConnection.class);
    private final Wpa_supplicant1 mockedWpaSupplicant = mock(Wpa_supplicant1.class);
    private final Interface mockedInterface = mock(Interface.class);

    private WpaSupplicantDbusWrapper wpaSupplicantDbusWrapper;
    private Exception occurredException;

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

    @Test
    public void asyncScanShouldWorkWithExistentInterface() throws DBusException {
        givenMockWpaSupplicant();
        givenMockInterface("wlan0", "/fi/w1/wpa_supplicant1/Interfaces/0");
        givenWpaSupplicantDbusWrapper();

        whenAsyncScanIsCalledWith("wlan0");

        thenExceptionDidNotOccur();
        thenInterfaceScanWasTriggered();
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

    private void givenMockInterface(String interfaceName, String dbusPath) throws DBusException {
        when(this.mockedWpaSupplicant.GetInterface(interfaceName))
                .thenReturn(new DBusPath(dbusPath));
        when(this.mockedDbusConnection.getRemoteObject(WPA_SUPPLICANT_BUS_NAME, dbusPath, Interface.class))
                .thenReturn(this.mockedInterface);
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

    private void thenExceptionDidNotOccur() {
        assertNull(this.occurredException);
    }

    private void thenExceptionOccurred(Class<DBusExecutionException> expectedExceptionClass) {
        assertNotNull(this.occurredException);
        assertEquals(expectedExceptionClass, this.occurredException.getClass());
    }

    private void thenInterfaceScanWasTriggered() {
        Map<String, Variant<?>> expectedOptions = new HashMap<>();
        expectedOptions.put("AllowRoam", new Variant<>(false));
        expectedOptions.put("Type", new Variant<>("active"));

        verify(this.mockedInterface, times(1)).Scan(expectedOptions);
    }
}
