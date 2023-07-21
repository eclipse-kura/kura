package org.eclipse.kura.nm.signal.handlers;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.w1.wpa_supplicant1.Interface;

public class WPAScanLock {

    private static final Logger logger = LoggerFactory.getLogger(WPAScanLock.class);

    private static final long SCAN_TIMEOUT_S = 30;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final WPAScanDoneHandler scanHandler;
    private final DBusConnection dbusConnection;

    public WPAScanLock(DBusConnection dbusConnection, String dbusPath) throws DBusException {
        if (Objects.isNull(dbusPath) || dbusPath.isEmpty() || dbusPath.equals("/")) {
            throw new IllegalArgumentException(String.format("Illegat DBus path for DeviceSateLock \"%s\"", dbusPath));
        }
        this.dbusConnection = Objects.requireNonNull(dbusConnection);
        this.scanHandler = new WPAScanDoneHandler(this.latch, dbusPath);

        this.dbusConnection.addSigHandler(Interface.ScanDone.class, this.scanHandler);
    }

    public void waitForSignal() throws DBusException {
        try {
            boolean countdownCompleted = this.latch.await(SCAN_TIMEOUT_S, TimeUnit.SECONDS);
            if (!countdownCompleted) {
                logger.warn("Timeout elapsed. Exiting anyway");
            }
        } catch (InterruptedException e) {
            logger.warn("Wait interrupted because of:", e);
            Thread.currentThread().interrupt();
        } finally {
            this.dbusConnection.removeSigHandler(Interface.ScanDone.class, this.scanHandler);
        }
    }

}