package org.eclipse.kura.nm;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceStateLock {

    private static final Logger logger = LoggerFactory.getLogger(DeviceStateLock.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private NMDeviceStateChangeHandler stateHandler;
    private final DBusConnection dbusConnection;

    public DeviceStateLock(DBusConnection dbusConnection, String dbusPath, NMDeviceState expectedNmDeviceState)
            throws DBusException {
        if (Objects.isNull(dbusPath) || dbusPath.isEmpty() || dbusPath.equals("/")) {
            throw new IllegalArgumentException(String.format("Illegat DBus path for DeviceSateLock \"%s\"", dbusPath));
        }
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.stateHandler = new NMDeviceStateChangeHandler(latch, dbusPath, expectedNmDeviceState);

        this.dbusConnection.addSigHandler(Device.StateChanged.class, stateHandler);
    }

    public void waitForSignal() throws DBusException {
        try {
            boolean countdownCompleted = latch.await(5, TimeUnit.SECONDS);
            if (!countdownCompleted) {
                logger.warn("Timeout elapsed. Exiting anyway");
            }
        } catch (InterruptedException e) {
            logger.warn("Wait interrupted because of:", e);
            Thread.currentThread().interrupt();
        } finally {
            this.dbusConnection.removeSigHandler(Device.StateChanged.class, stateHandler);
        }
    }

}
