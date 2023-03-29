package org.eclipse.kura.nm;

import java.util.Objects;
import java.util.Optional;
import java.util.Timer;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemStateHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMModemStateHandler.class);
    private final Timer modemResetTimer = new Timer("ModemResetTimer");
    private final NMDbusConnector nm;

    private NMModemResetTimerTask scheduledTasks = null;

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

        Optional<Modem> modem = this.nm.getModemDevice(s.getPath());
        if (!modem.isPresent()) {
            logger.warn("Cannot retrieve modem from path. Cannot schedule reset");
            return;
        }

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());
        NMDeviceStateReason reason = NMDeviceStateReason.fromUInt32(s.getReason());

        logger.info("Modem state change detected: {} -> {} (reason: {}), for device {}", oldState, newState, reason,
                s.getPath());

        if (oldState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED && newState == NMDeviceState.NM_DEVICE_STATE_FAILED) {
            if (timerAlreadyScheduledFor(modem.get())) {
                logger.debug("Modem {} already scheduled for reset. Ignoring event...", s.getPath());
            }

            logger.info("Modem {} disconnected. Scheduling modem reset...", s.getPath());

            this.scheduledTasks = new NMModemResetTimerTask(modem.get());
            modemResetTimer.schedule(new NMModemResetTimerTask(modem.get()), 25000L); // WIP: Delay should be set by
                                                                                      // configuration, using fixed one
        } else if (newState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED) {

            if (!timerAlreadyScheduledFor(modem.get())) {
                return;
            }

            logger.info("Modem {} reconnected. Cancelling scheduled modem reset...", s.getPath());
            this.scheduledTasks.cancel();
            this.scheduledTasks = null;
            this.modemResetTimer.purge();
        }
    }

    private boolean timerAlreadyScheduledFor(Modem modem) {
        if (Objects.isNull(this.scheduledTasks)) {
            return false;
        }

        return modem.getObjectPath().equals(this.scheduledTasks.getModemDbusPath());
    }

}
