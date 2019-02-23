/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - show return code on failure, minor cleanups
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockEvent;
import org.eclipse.kura.clock.ClockService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClockServiceImpl implements ConfigurableComponent, ClockService, ClockSyncListener {

    private static final ClockEvent EMPTY_EVENT = new ClockEvent(Collections.<String, Object> emptyMap());

    private static final String PROP_CLOCK_PROVIDER = "clock.provider";
    private static final String PROP_CLOCK_SET_HWCLOCK = "clock.set.hwclock";
    private static final String PROP_ENABLED = "enabled";

    private static final Logger logger = LoggerFactory.getLogger(ClockServiceImpl.class);

    private EventAdmin eventAdmin;
    private Map<String, Object> properties;
    private ClockSyncProvider provider;
    private boolean configEnabled;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(Map<String, Object> properties) {
        // save the properties
        this.properties = properties;

        logger.info("Activate. Current Time: {}", new Date());

        try {
            if (this.properties.get(PROP_ENABLED) != null) {
                this.configEnabled = (Boolean) this.properties.get(PROP_ENABLED);
            } else {
                this.configEnabled = false;
            }

            if (this.configEnabled) {
                // start the provider
                startClockSyncProvider();
            }
        } catch (Throwable t) {
            logger.error("Error updating ClockService Configuration", t);
        }
    }

    protected void deactivate() {
        logger.info("Deactivate...");
        try {
            stopClockSyncProvider();
        } catch (Throwable t) {
            logger.error("Error deactivate ClockService", t);
        }
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updated...");

        try {
            // save the properties
            this.properties = properties;

            if (this.properties.get(PROP_ENABLED) != null) {
                this.configEnabled = (Boolean) this.properties.get(PROP_ENABLED);
            } else {
                this.configEnabled = false;
                return;
            }

            if (this.configEnabled) {
                // start the provider
                startClockSyncProvider();
            } else {
                // stop the provider if it was running
                try {
                    stopClockSyncProvider();
                } catch (Throwable t) {
                    logger.error("Error deactivate ClockService", t);
                }
            }
        } catch (Throwable t) {
            logger.error("Error updating ClockService Configuration", t);
        }
    }

    // ----------------------------------------------------------------
    //
    // Master Client Management APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Date getLastSync() throws KuraException {
        if (this.provider != null) {
            return this.provider.getLastSync();
        } else {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Clock service not configured yet");
        }
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    private void startClockSyncProvider() throws KuraException {
        stopClockSyncProvider();
        String sprovider = (String) this.properties.get(PROP_CLOCK_PROVIDER);
        if ("java-ntp".equals(sprovider)) {
            this.provider = new JavaNtpClockSyncProvider();
        } else if ("ntpd".equals(sprovider)) {
            this.provider = new NtpdClockSyncProvider();
        }         
        if (this.provider != null) {
            this.provider.init(this.properties, this);
            this.provider.start();
        }
    }

    private void stopClockSyncProvider() throws KuraException {
        if (this.provider != null) {
            this.provider.stop();
            this.provider = null;
        }
    }

    /**
     * Called by the current ClockSyncProvider after each Clock synchronization
     */
    @Override
    public void onClockUpdate(long offset) {

        logger.info("Clock update. Offset: {}", offset);

        // set system clock if necessary
        boolean bClockUpToDate = false;
        if (offset != 0) {
            long time = System.currentTimeMillis() + offset;
            SafeProcess proc = null;
            try {
                proc = exec("date -s @" + time / 1000);		// divide by 1000 to switch to seconds
                proc.waitFor();
                final int rc = proc.exitValue();
                if (rc == 0) {
                    bClockUpToDate = true;
                    logger.info("System Clock Updated to {}", new Date());
                } else {
                    logger.error("Unexpected error while updating System Clock - rc = {}, it should've been {}", rc,
                            new Date());
                }
            } catch (Exception e) {
                logger.error("Error updating System Clock", e);
            } finally {
                if (proc != null) {
                    ProcessUtil.destroy(proc);
                }
            }
        } else {
            bClockUpToDate = true;
        }

        // set hardware clock
        boolean updateHwClock = false;
        if (this.properties.containsKey(PROP_CLOCK_SET_HWCLOCK)) {
            updateHwClock = (Boolean) this.properties.get(PROP_CLOCK_SET_HWCLOCK);
        }
        if (updateHwClock) {
            SafeProcess proc = null;
            try {
                proc = exec("hwclock --utc --systohc");
                proc.waitFor();
                final int rc = proc.exitValue();
                if (rc == 0) {
                    logger.info("Hardware Clock Updated");
                } else {
                    logger.error("Unexpected error while updating Hardware Clock - rc = {}", rc);
                }
            } catch (Exception e) {
                logger.error("Error updating Hardware Clock", e);
            } finally {
                if (proc != null) {
                    ProcessUtil.destroy(proc);
                }
            }
        }

        // Raise the event
        if (bClockUpToDate) {
            this.eventAdmin.postEvent(EMPTY_EVENT);
        }
    }

    protected SafeProcess exec(String command) throws IOException {
        return ProcessUtil.exec(command);
    }
}
