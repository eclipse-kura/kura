package org.eclipse.kura.nm;

import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.modemmanager1.Modem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMModemStateChangeHandler implements DBusSigHandler<Modem.StateChanged> {

    private static final Logger logger = LoggerFactory.getLogger(MMModemStateChangeHandler.class);

    @Override
    public void handle(Modem.StateChanged s) {
        MMModemState oldState = MMModemState.toMMModemState(s.getOld());
        MMModemState newState = MMModemState.toMMModemState(s.getNewparam());
        MMModemStateChangeReason reason = MMModemStateChangeReason.fromUInt32(s.getReason());

        logger.debug("Modem state change detected: {} -> {} (reason: {}), for device {}", oldState, newState, reason,
                s.getPath());
    }
}
