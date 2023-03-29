package org.eclipse.kura.nm;

import java.util.Objects;
import java.util.TimerTask;

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
    public void run() {
        logger.info("Modem reset timer expired. Resetting modem {} ...", modem.getObjectPath());
        this.modem.Reset();
        this.hasRun = true;
    }

    public String getModemDbusPath() {
        return this.modem.getObjectPath();
    }

}
