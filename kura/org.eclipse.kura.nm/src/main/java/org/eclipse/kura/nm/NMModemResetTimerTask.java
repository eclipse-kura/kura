package org.eclipse.kura.nm;

import java.util.Objects;
import java.util.TimerTask;

import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.modemmanager1.Modem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMModemResetTimerTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(NMModemResetTimerTask.class);

    private Modem modem;
    private boolean hasRun = false;

    public NMModemResetTimerTask(Modem modem) {
        this.modem = Objects.requireNonNull(modem);
    }

    public boolean hasRun() {
        return this.hasRun;
    }

    @Override
    public boolean cancel() {
        logger.info("Modem reset timer cancelled for {}", modem.getObjectPath());
        return super.cancel();
    }

    @Override
    public void run() {
        logger.info("Modem reset timer expired. Resetting modem {} ...", modem.getObjectPath());
        try {
            this.modem.Reset();
        } catch (DBusExecutionException e) {
            logger.warn("Could not perform modem reset for {} because: ", this.modem.getObjectPath(), e);
        }
        this.hasRun = true;
    }

    public String getModemDbusPath() {
        return this.modem.getObjectPath();
    }

}
