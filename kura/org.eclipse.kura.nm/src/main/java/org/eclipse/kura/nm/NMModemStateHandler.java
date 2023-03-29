package org.eclipse.kura.nm;

import java.util.Objects;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemStateHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMModemStateHandler.class);
    private final NMDbusConnector nm;

    public NMModemStateHandler(NMDbusConnector nmDbusConnector) {
        this.nm = Objects.requireNonNull(nmDbusConnector);
    }

    @Override
    public void handle(Device.StateChanged s) {

        NMDeviceType deviceType = NMDeviceType.NM_DEVICE_TYPE_UNKNOWN;
        try {
            deviceType = this.nm.getDeviceType(s.getPath());
        } catch (DBusException e) {
            logger.warn("Cannot retrieve device type for {}. Not handling signal. Caused by:", s.getPath(), e);
            return;
        }

        if (deviceType != NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            return;
        }

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());
        NMDeviceStateReason reason = NMDeviceStateReason.fromUInt32(s.getReason());

        logger.info("Modem state change detected: {} -> {} (reason: {}), for device {}", oldState, newState, reason,
                s.getPath());

        if (newState == NMDeviceState.NM_DEVICE_STATE_FAILED) {
            logger.info("Modem {} disconnected. Scheduling modem reset...", s.getPath());
            // Schedule Modem reset
        } else if (newState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED) {
            logger.info("Modem {} reconnected. Cancelling scheduled modem reset...", s.getPath());
            // Check dbus path
            // If there's a scheduled modem reset for that path
            // Cancel scheduled modem reset
        }
    }

}
