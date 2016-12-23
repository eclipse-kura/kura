/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.windows.clock;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.clock.ClockEvent;
import org.eclipse.kura.clock.ClockService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClockServiceImpl implements ConfigurableComponent, ClockService, ClockSyncListener {

    private static final Logger s_logger = LoggerFactory.getLogger(ClockServiceImpl.class);

    @SuppressWarnings("unused")
    private ComponentContext m_ctx;
    private EventAdmin m_eventAdmin;
    private Map<String, Object> m_properties;
    private ClockSyncProvider m_provider;
    private boolean m_configEnabled;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.m_eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.m_eventAdmin = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        // save the properties
        this.m_properties = properties;

        s_logger.info("Activate. Current Time: {}", new Date());

        // save the bundle context
        this.m_ctx = componentContext;

        try {
            if (this.m_properties.get("enabled") != null) {
                this.m_configEnabled = (Boolean) this.m_properties.get("enabled");
            } else {
                this.m_configEnabled = false;
            }

            if (this.m_configEnabled) {
                // start the provider
                startClockSyncProvider();
            }
        } catch (Throwable t) {
            s_logger.error("Error updating ClockService Configuration", t);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivate...");
        try {
            stopClockSyncProvider();
        } catch (Throwable t) {
            s_logger.error("Error deactivate ClockService", t);
        }
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated...");
        try {

            // save the properties
            this.m_properties = properties;

            if (this.m_properties.get("enabled") != null) {
                this.m_configEnabled = (Boolean) this.m_properties.get("enabled");
            } else {
                this.m_configEnabled = false;
                return;
            }

            if (this.m_configEnabled) {
                // start the provider
                startClockSyncProvider();
            }
        } catch (Throwable t) {
            s_logger.error("Error updating ClockService Configuration", t);
        }
    }

    // ----------------------------------------------------------------
    //
    // Master Client Management APIs
    //
    // ----------------------------------------------------------------

    @Override
    public Date getLastSync() throws KuraException {
        if (this.m_provider != null) {
            return this.m_provider.getLastSync();
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
        String provider = (String) this.m_properties.get("clock.provider");
        if ("java-ntp".equals(provider)) {
            this.m_provider = new JavaNtpClockSyncProvider();
        } else if ("ntpd".equals(provider)) {
            this.m_provider = new NtpdClockSyncProvider();
        } else if ("gps".equals(provider)) {
            this.m_provider = new GpsClockSyncProvider();
        }
        if (this.m_provider != null) {
            this.m_provider.init(this.m_properties, this);
            this.m_provider.start();
        }
    }

    private void stopClockSyncProvider() throws KuraException {
        if (this.m_provider != null) {
            this.m_provider.stop();
            this.m_provider = null;
        }
    }

    /**
     * Called by the current ClockSyncProvider after each Clock synchronization
     */
    @Override
    public void onClockUpdate(long offset) {

        s_logger.info("Clock update. Offset: {}", offset);

        // set system clock if necessary
        boolean bClockUpToDate = false;
        if (offset != 0) {
            long time = System.currentTimeMillis() + offset;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            try {
                WindowsSetSystemTime winTime = new WindowsSetSystemTime();
                winTime.SetLocalTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND));
                bClockUpToDate = true;
                s_logger.info("System Clock Updated to {}", cal.getTime());
            } catch (Exception e) {
                s_logger.error("Error updating System Clock", e);
            }
        } else {
            bClockUpToDate = true;
        }

        // set hardware clock - this should be done automatically above
        /*
         * boolean updateHwClock = false;
         * if (m_properties.containsKey("clock.set.hwclock")) {
         * updateHwClock = (Boolean) m_properties.get("clock.set.hwclock");
         * }
         * if (updateHwClock) {
         * }
         */

        // Raise the event
        if (bClockUpToDate) {
            this.m_eventAdmin.postEvent(new ClockEvent(new HashMap<String, Object>()));
        }
    }
}
