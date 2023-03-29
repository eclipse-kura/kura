package org.eclipse.kura.nm;

import java.util.Objects;
import java.util.Timer;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemStateHandler implements DBusSigHandler<Device.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(NMModemStateHandler.class);
    private final Timer modemResetTimer = new Timer("ModemResetTimer");
    private NMModemResetTimerTask scheduledTasks = null;

    private String nmDevicePath;
    private Modem mmModemDevice;
    private long delay;

    public NMModemStateHandler(String nmDeviceDBusPath, Modem mmModem, long modemResetDelayMilliseconds) {
        this.nmDevicePath = nmDeviceDBusPath;
        this.mmModemDevice = Objects.requireNonNull(mmModem);
        this.delay = modemResetDelayMilliseconds;
    }

    @Override
    public void handle(Device.StateChanged s) {
        if (!s.getPath().equals(this.nmDevicePath)) {
            return;
        }

        NMDeviceState oldState = NMDeviceState.fromUInt32(s.getOldState());
        NMDeviceState newState = NMDeviceState.fromUInt32(s.getNewState());

        logger.info("Modem state change detected: {} -> {}, for device {}", oldState, newState, s.getPath());

        if (oldState == NMDeviceState.NM_DEVICE_STATE_FAILED
                && newState == NMDeviceState.NM_DEVICE_STATE_DISCONNECTED) {
            if (timerAlreadyScheduled()) {
                logger.debug("Modem {} already scheduled for reset. Ignoring event...", s.getPath());
            }

            logger.info("Modem {} disconnected. Scheduling modem reset in {} minutes ...", s.getPath(),
                    this.delay / (60 * 1000));

            this.scheduledTasks = new NMModemResetTimerTask(this.mmModemDevice);
            modemResetTimer.schedule(this.scheduledTasks, this.delay);
        } else if (newState == NMDeviceState.NM_DEVICE_STATE_ACTIVATED) {

            if (!timerAlreadyScheduled()) {
                return;
            }

            logger.info("Modem {} reconnected. Cancelling scheduled modem reset...", this.nmDevicePath);
            this.scheduledTasks.cancel();
            this.scheduledTasks = null;
        }
    }

    private boolean timerAlreadyScheduled() {
        return !Objects.isNull(this.scheduledTasks) && !this.scheduledTasks.hasRun();
    }

    public void clearTimer() {
        if (timerAlreadyScheduled()) {
            this.scheduledTasks.cancel();
        }
    }

}
